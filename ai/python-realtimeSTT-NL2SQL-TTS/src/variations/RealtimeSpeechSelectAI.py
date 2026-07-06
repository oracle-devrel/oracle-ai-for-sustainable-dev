import asyncio
import getpass
import os
import json
import pyaudio
import oracledb
from datetime import datetime
import oci
from oci.config import from_file
from oci.auth.signers.security_token_signer import SecurityTokenSigner
from oci.ai_speech_realtime import (
    RealtimeClient,
    RealtimeClientListener,
    RealtimeParameters,
)
from aiohttp import web

# Global variables to store the latest data
latest_thetime = None
latest_question = None
latest_answer = None
compartment_id = os.getenv('COMPARTMENT_ID')
print(f"compartment_id: {compartment_id}")
pw = getpass.getpass("Enter database user password:")

# Use this when making a connection with a wallet
connection = oracledb.connect(
    user="moviestream",
    password=pw,
    dsn="selectaidb_high",
    config_dir=r"C:\Users\paulp\Downloads\Wallet_SelectAIDB",
    wallet_location=r"C:\Users\paulp\Downloads\Wallet_SelectAIDB"
)
print(f"Successfully connected to Oracle Database Connection: {connection}")

# Create a FIFO queue
queue = asyncio.Queue()

# Set audio parameters
SAMPLE_RATE = 16000
FORMAT = pyaudio.paInt16
CHANNELS = 1
BUFFER_DURATION_MS = 96

# Calculate the number of frames per buffer
FRAMES_PER_BUFFER = int(SAMPLE_RATE * BUFFER_DURATION_MS / 1000)

# Variables to keep track of results and state
cummulativeResult = ""
isSelect = False
last_result_time = None

def authenticator():
    config = from_file("~/.oci/config", "MYSPEECHAIPROFILE")
    with open(config["security_token_file"], "r") as f:
        token = f.readline()
    private_key = oci.signer.load_private_key_from_file(config["key_file"])
    auth = SecurityTokenSigner(token=token, private_key=private_key)
    return auth

def audio_callback(in_data, frame_count, time_info, status):
    # This function will be called by PyAudio when there's new audio data
    queue.put_nowait(in_data)
    return (None, pyaudio.paContinue)

p = pyaudio.PyAudio()

# Open the stream
stream = p.open(
    format=FORMAT,
    channels=CHANNELS,
    rate=SAMPLE_RATE,
    input=True,
    frames_per_buffer=FRAMES_PER_BUFFER,
    stream_callback=audio_callback,
)

stream.start_stream()
config = from_file()
isInsertResults = True

async def send_audio(client):
    while True:
        data = await queue.get()
        # Send it over the websocket
        await client.send_data(data)

class SpeechListener(RealtimeClientListener):
    def on_result(self, result):
        global cummulativeResult, isSelect, last_result_time
        if result["transcriptions"][0]["isFinal"]:
            transcription = result['transcriptions'][0]['transcription']
            cummulativeResult += transcription
            print(f"Received final results: {transcription}")
            print(f"Current cummulative result: {cummulativeResult}")
            if cummulativeResult.lower().startswith("select ai"):
                cummulativeResult = cummulativeResult[len("select ai"):].strip()
                isSelect = True
            elif cummulativeResult.lower().startswith("select the eye"):
                cummulativeResult = cummulativeResult[len("select the eye"):].strip()
                isSelect = True
            else:
                cummulativeResult = ""
            last_result_time = asyncio.get_event_loop().time()
        else:
            print(f"Received partial results: {result['transcriptions'][0]['transcription']}")


    def on_ack_message(self, ackmessage):
        return super().on_ack_message(ackmessage)

    def on_connect(self):
        return super().on_connect()

    def on_connect_message(self, connectmessage):
        return super().on_connect_message(connectmessage)

    def on_network_event(self, ackmessage):
        return super().on_network_event(ackmessage)

    def on_error(self):
        return super().on_error()

async def check_idle():
    global last_result_time, isSelect
    while True:
        if isSelect and last_result_time and (asyncio.get_event_loop().time() - last_result_time > 2):
            executeSelectAI()
            isSelect = False
        await asyncio.sleep(1)

# Function to execute AI query and optionally insert results into the table
# For example Select AI I am looking for the top five selling movies for the latest month please
def executeSelectAI():
    global cummulativeResult, isInsertResults, latest_thetime, latest_question, latest_answer
    print(f"executeSelectAI called cummulative result: {cummulativeResult}")

    # AI query - todo use openai_gpt4o
    query = """SELECT DBMS_CLOUD_AI.GENERATE(
                prompt       => :prompt,
                profile_name => 'openai_gpt35', 
                action       => 'narrate')
            FROM dual"""

    try:
        with connection.cursor() as cursor:
            cursor.execute(query, prompt=cummulativeResult)
            result = cursor.fetchone()
            if result and isinstance(result[0], oracledb.LOB):
                text_result = result[0].read()
                print(text_result)

                latest_thetime = datetime.now()
                latest_question = cummulativeResult
                latest_answer = text_result[:3000]  # Truncate if necessary
                cummulativeResult = ""

                if isInsertResults:
                    insert_query = """
                    INSERT INTO selectai_data (thetime, question, answer)
                    VALUES (:thetime, :question, :answer)
                    """
                    cursor.execute(insert_query, {
                        'thetime': latest_thetime,
                        'question': latest_question,
                        'answer': latest_answer
                    })
                    connection.commit()
                    print("Insert successful.")
            else:
                print(result)
    except Exception as e:
        print(f"An error occurred: {e}")

    cummulativeResult = ""

async def handle_request(request):
    global latest_thetime, latest_question, latest_answer
    data = {
        "thetime": latest_thetime.isoformat() if latest_thetime else None,  # Convert datetime to ISO format
        "question": latest_question,
        "answer": latest_answer
    }
    return web.json_response(data)


if __name__ == "__main__":
    # Run the event loop
    def message_callback(message):
        print(f"Received message: {message}")

    realtime_speech_parameters = RealtimeParameters()
    realtime_speech_parameters.language_code = "en-US"
    realtime_speech_parameters.model_domain = (
        realtime_speech_parameters.MODEL_DOMAIN_GENERIC
    )
    realtime_speech_parameters.partial_silence_threshold_in_ms = 0
    realtime_speech_parameters.final_silence_threshold_in_ms = 2000
    realtime_speech_parameters.should_ignore_invalid_customizations = False
    realtime_speech_parameters.stabilize_partial_results = (
        realtime_speech_parameters.STABILIZE_PARTIAL_RESULTS_NONE
    )

    realtime_speech_url = "wss://realtime.aiservice.us-phoenix-1.oci.oraclecloud.com"
    client = RealtimeClient(
        config=config,
        realtime_speech_parameters=realtime_speech_parameters,
        listener=SpeechListener(),
        service_endpoint=realtime_speech_url,
        signer=authenticator(),
        compartment_id=compartment_id,
    )

    loop = asyncio.get_event_loop()
    loop.create_task(send_audio(client))
    loop.create_task(check_idle())

    # Set up the HTTP server
    app = web.Application()
    app.router.add_get('/selectai_data', handle_request)
    runner = web.AppRunner(app)
    loop.run_until_complete(runner.setup())
    site = web.TCPSite(runner, 'localhost', 8080)
    loop.run_until_complete(site.start())

    loop.run_until_complete(client.connect())

    if stream.is_active():
        stream.close()

    print("Closed")
