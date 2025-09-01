import asyncio
import getpass
import os
import json
import pyaudio
import requests
import time
import wave
from datetime import datetime
import oracledb
import oci
from oci.config import from_file
from oci.auth.signers.security_token_signer import SecurityTokenSigner
from oci.ai_speech_realtime import (
    RealtimeSpeechClient,
    RealtimeSpeechClientListener,
    RealtimeParameters,
)
from aiohttp import web

from oci.ai_speech import AIServiceSpeechClient
from oci.ai_speech.models import SynthesizeSpeechDetails

latest_thetime = None
latest_question = None
latest_answer = None
compartment_id = os.getenv('COMPARTMENT_ID')
print(f"compartment_id: {compartment_id}")

# If using thick mode/driver, do the following to load needed libraries...
# (client can be downloaded from https://www.oracle.com/database/technologies/instant-client/winx64-64-downloads.html)
# oracledb.init_oracle_client(lib_dir=r"C:\[path_to_instant_client]\instantclient_23_7")
connection = oracledb.connect(
    user="moviestream",
    password="Welcome12345",
    dsn="selectaidb_high",
    config_dir="/Users/pparkins/Downloads/Wallet_SelectAIDB",
    wallet_location="/Users/pparkins/Downloads/Wallet_SelectAIDB",
    wallet_password="Welcome12345"
)
print(f"Successfully connected to Oracle Database Connection: {connection}")

queue = asyncio.Queue()

SAMPLE_RATE = 16000
FORMAT = pyaudio.paInt16
CHANNELS = 1
BUFFER_DURATION_MS = 96
FRAMES_PER_BUFFER = int(SAMPLE_RATE * BUFFER_DURATION_MS / 1000)

cummulativeResult = ""
isSelect = False
isNarrate = False
isShowSQL = False
isRunSQL = False
isExplainSQL = False
last_result_time = None


def audio_callback(in_data, frame_count, time_info, status):
    queue.put_nowait(in_data)
    return (None, pyaudio.paContinue)


p = pyaudio.PyAudio()

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
isInsertResults = False


async def send_audio(client):
    while True:
        data = await queue.get()
        await client.send_data(data)


def play_audio(file_path):
    try:
        wf = wave.open(file_path, 'rb')
        p = pyaudio.PyAudio()
        stream = p.open(
            format=p.get_format_from_width(wf.getsampwidth()),
            channels=wf.getnchannels(),
            rate=wf.getframerate(),
            output=True
        )

        data = wf.readframes(1024)
        while data:
            stream.write(data)
            data = wf.readframes(1024)

        stream.stop_stream()
        stream.close()
        p.terminate()
        print("Audio playback finished.")
    except Exception as e:
        print(f"Error playing audio: {e}")


class SpeechListener(RealtimeSpeechClientListener):
    def on_result(self, result):
        global cummulativeResult, isSelect, isNarrate, isShowSQL, isRunSQL, isExplainSQL, last_result_time
        if result["transcriptions"][0]["isFinal"]:
            transcription = result['transcriptions'][0]['transcription']
            cummulativeResult += transcription
            print(f"Received final results: {transcription}")
            print(f"Current cummulative result: {cummulativeResult}")
            if cummulativeResult.lower().startswith("hey db"):
                cummulativeResult = cummulativeResult[len("hey db"):].strip()
                isSelect = True
            elif cummulativeResult.lower().startswith("hey deebee"):
                cummulativeResult = cummulativeResult[len("hey deebee"):].strip()
                isSelect = True
            elif cummulativeResult.lower().startswith("adb"):
                cummulativeResult = cummulativeResult[len("adb"):].strip()
                isSelect = True
            else:
                cummulativeResult = ""
            if cummulativeResult.lower().endswith("use narrate"):
                cummulativeResult = cummulativeResult[:-len("use narrate")].strip()
                isNarrate = True
                print(f"isNarrate: {isNarrate}")
            elif cummulativeResult.lower().endswith("use show sql"):
                cummulativeResult = cummulativeResult[:-len("use show sql")].strip()
                isShowSQL = True
                print(f"isShowSQL: {isShowSQL}")
            elif cummulativeResult.lower().endswith("use run sql"):
                cummulativeResult = cummulativeResult[:-len("use run sql")].strip()
                isRunSQL = True
                print(f"isRunSQL: {isRunSQL}")
            elif cummulativeResult.lower().endswith("use explain"):
                cummulativeResult = cummulativeResult[:-len("use explain sql")].strip()
                isExplainSQL = True
                print(f"isExplainSQL: {isExplainSQL}")
            last_result_time = asyncio.get_event_loop().time()
        else:
            print(f"Received partial results: {result['transcriptions'][0]['transcription']}")

    def on_ack_message(self, ackmessage):
        print(f"ACK received: {ackmessage}")

    def on_connect(self):
        print("Connected to Realtime Speech Service.")

    def on_connect_message(self, connectmessage):
        print(f"Connect message: {connectmessage}")

    def on_network_event(self, ackmessage):
        print(f"Network event: {ackmessage}")

    def on_error(self, exception):
        print(f"An error occurred: {exception}")


async def check_idle():
    global last_result_time, isSelect, isNarrate, isShowSQL, isRunSQL, isExplainSQL
    while True:
        if isSelect and last_result_time and (asyncio.get_event_loop().time() - last_result_time > 2):
            executeSelectAI()
            isSelect = False
            isNarrate = False
            isShowSQL = False
            isRunSQL = False
            isExplainSQL = False
        await asyncio.sleep(1)


def authenticator():
    config = from_file("~/.oci/config", "DEFAULT")
    with open(config["security_token_file"], "r") as f:
        token = f.readline()
    private_key = oci.signer.load_private_key_from_file(config["key_file"])
    auth = SecurityTokenSigner(token=token, private_key=private_key)
    return auth


def executeSelectAI():
    global cummulativeResult, isNarrate, isShowSQL, isRunSQL, isExplainSQL, isInsertResults, latest_thetime, latest_question, latest_answer
    print(f"executeSelectAI called cummulative result: {cummulativeResult}")
    cummulativeResult += " . Answer in 20 words or less."
    if isNarrate:
        selectai_action = "narrate" # query database and give back narration style reply
    elif isShowSQL:
        selectai_action = "showsql" # show the sql generated by select AI for this prompt/query
    elif isRunSQL:
        selectai_action = "runsql" # show the results of running the sql generated by select AI for this prompt/query
    elif isExplainSQL:
        selectai_action = "explainsql" # explain the sql generated by select AI for this prompt/query
    else:
        selectai_action = "chat"  # bypass database and go straight to LLM

    query = """SELECT DBMS_CLOUD_AI.GENERATE(
                prompt       => :prompt,
                profile_name => 'GENAI',
                action       => :action)
            FROM dual"""

    try:
        with connection.cursor() as cursor:
            print(f"executeSelectAI using {selectai_action}")
            cursor.execute(query, {'prompt': cummulativeResult, 'action': selectai_action})
            result = cursor.fetchone()
            if result and isinstance(result[0], oracledb.LOB):
                text_result = result[0].read()
                print(f"Query result: {text_result}")
                latest_thetime = datetime.now()
                latest_question = cummulativeResult
                latest_answer = text_result[:3000]

            cummulativeResult = ""

            if selectai_action in ("showsql", "runsql", "explainsql"):
                return
            # API key-based authentication, using phoenix OCI Region - https://docs.oracle.com/en-us/iaas/Content/speech/using/speech.htm#ser-limits
            config = oci.config.from_file("~/.oci/config", "DEFAULT")
            ai_speech_client = oci.ai_speech.AIServiceSpeechClient(config)
            synthesize_speech_response = ai_speech_client.synthesize_speech(
                synthesize_speech_details=oci.ai_speech.models.SynthesizeSpeechDetails(
                    text=f" {latest_answer}",
                    is_stream_enabled=True,
                    compartment_id=compartment_id,
                    configuration=oci.ai_speech.models.TtsOracleConfiguration(
                        model_family="ORACLE",
                        model_details=oci.ai_speech.models.TtsOracleTts1StandardModelDetails(
                            model_name="TTS_1_STANDARD",
                            voice_id="Bob"),
                        speech_settings=oci.ai_speech.models.TtsOracleSpeechSettings(
                            text_type="SSML",
                            sample_rate_in_hz=28000,
                            output_format="PCM",
                            speech_mark_types=["WORD"])),
                    audio_config=oci.ai_speech.models.TtsBaseAudioConfig(
                        config_type="BASE_AUDIO_CONFIG")
                )       )
            with open("TTSoutput.pcm", "wb") as audio_file:
               audio_file.write(synthesize_speech_response.data.content)
            print("Speech synthesis completed and saved as TTSoutput.pcm")

            # Play the generated speech
            play_audio("TTSoutput.pcm")

    except Exception as e:
        print(f"An error occurred: {e}")


async def handle_request(request):
    global latest_thetime, latest_question, latest_answer
    data = {
        "thetime": latest_thetime.isoformat() if latest_thetime else None,
        "question": latest_question,
        "answer": latest_answer
    }
    return web.json_response(data)


if __name__ == "__main__":
    loop = asyncio.new_event_loop()  # Fix event loop issue
    asyncio.set_event_loop(loop)

    realtime_speech_parameters = RealtimeParameters()
    realtime_speech_parameters.language_code = "en-US"
    realtime_speech_parameters.model_domain = (
        realtime_speech_parameters.MODEL_DOMAIN_GENERIC
    )
    realtime_speech_parameters.final_silence_threshold_in_ms = 2000

    realtime_speech_url = "wss://realtime.aiservice.us-phoenix-1.oci.oraclecloud.com"
    client = RealtimeSpeechClient(
        config=config,
        realtime_speech_parameters=realtime_speech_parameters,
        listener=SpeechListener(),
        service_endpoint=realtime_speech_url,
        signer=None,
        compartment_id=compartment_id,
    )

    # Instance, resource principal, or session token-based authentication (as shown below) can also be used
    # client = RealtimeSpeechClient(
    #     config=config,
    #     realtime_speech_parameters=realtime_speech_parameters,
    #     listener=SpeechListener(),
    #     service_endpoint=realtime_speech_url,
    #     signer=authenticator(),
    #     compartment_id=compartment_id,
    # )

    loop.create_task(send_audio(client))
    loop.create_task(check_idle())

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
