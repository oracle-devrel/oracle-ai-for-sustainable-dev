import asyncio
import getpass
import os
import json
import pyaudio
import requests
import time
from datetime import datetime
import oracledb
import oci
from oci.config import from_file
import asyncio
from datetime import datetime
import oracledb
import oci
from oci.config import from_file
from aiohttp import web

latest_thetime = None
latest_question = None
latest_answer = None
latest_action = None
narrate_sorry_noselect_message = "Sorry, unfortunately a valid SELECT statement could not be generated for your natural language prompt. Here is some more information to help you further:"
compartment_id = os.getenv('COMPARTMENT_ID')
print(f"compartment_id: {compartment_id}")

connection = oracledb.connect(
    user="moviestream",
    password="Welcome12345",
    dsn="selectaidb_high",
    config_dir=r"C:\Users\opc\Downloads\Wallet_SelectAIDB\Wallet_SelectAIDB",
    wallet_location=r"C:\Users\opc\Downloads\Wallet_SelectAIDB\Wallet_SelectAIDB",
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
isRag = False
isChat = False
isShowSQL = False
last_result_time = None
is_connected = False

isInsertResults = False


def executeSandbox():
    global cummulativeResult, isInsertResults, latest_thetime, latest_question, latest_answer
    print(f"isRag is true, using ai sandbox: {cummulativeResult}")
    url = 'http://129.153.130.96:8000/v1/chat/completions'
    data = {"message": cummulativeResult}
    headers = {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer asdf'
    }
    response = requests.post(url, json=data, headers=headers)
    if response.status_code == 200:
        response_data = response.json()
        answer = response_data.get('answer', '')
        sources = response_data.get('sources', [])
        sources_str = ', '.join(sources)
        latest_answer = f"{answer} Sources searched: {sources_str}"
        print("RAG Full Response latest_answer:", latest_answer)
        doTTSAndAudio2Face(latest_answer, latest_question)
    else:
        print("Failed to fetch data:", response.status_code, response.text)
    cummulativeResult = ""

def executeSelectAI():
    global cummulativeResult, isInsertResults, isShowSQL, latest_thetime, latest_question, latest_answer
    print(f"executeSelectAI called cummulative result: {cummulativeResult}")
    contains_logic = [
        {"containsWord": "f1", "latestQuestion": "f1", "latestAnswer": "f1"},
        {"containsWord": "f 1", "latestQuestion": "f1", "latestAnswer": "f1"},
        {"containsWord": "eff 1", "latestQuestion": "f1", "latestAnswer": "f1"},
        {"containsWord": "satellite", "latestQuestion": "satellites", "latestAnswer": "satellites"},
        {"containsWord": "satellites", "latestQuestion": "satellites", "latestAnswer": "satellites"},
        {"containsWord": "spatial", "latestQuestion": "swag", "latestAnswer": "spatial"}
    ]

    chatquery = """SELECT DBMS_CLOUD_AI.GENERATE(
                prompt       => :prompt,
                profile_name => 'VIDEOGAMES_PROFILE', 
                action       => 'chat')
            FROM dual"""

    narratequery = """SELECT DBMS_CLOUD_AI.GENERATE(
                prompt       => :prompt,
                profile_name => 'VIDEOGAMES_PROFILE', 
                action       => 'narrate')
            FROM dual"""

    showssqlquery = """SELECT DBMS_CLOUD_AI.GENERATE(
                prompt       => :prompt,
                profile_name => 'VIDEOGAMES_PROFILE', 
                action       => 'showsql')
            FROM dual"""

    if isShowSQL:
        query = showssqlquery
        print("showsql true")
    elif isChat:
        query = chatquery
        print("chat true")
    else:
        query = narratequery

    cummulativeResult += " ignore case"
    try:
        with connection.cursor() as cursor:
            try:
                if handleContainsLogic(cummulativeResult.lower(), contains_logic):
                    cummulativeResult = cummulativeResult.replace("show sql", "")
                    print(f"executeSelectAI handled directly with cummulative result: {cummulativeResult}")
                    return
                else:
                    start_time = time.time()
                    cursor.execute(query, {'prompt': cummulativeResult})
                    result = cursor.fetchone()
                    if result and isinstance(result[0], oracledb.LOB):
                        text_result = result[0].read()
                        print(f"isinstance LOB text_result: {text_result}")
                        end_time = time.time()
                        elapsed_time = end_time - start_time
                        print(f"Query execution time: {elapsed_time:.4f} seconds")

                        latest_thetime = datetime.now()
                        latest_question = cummulativeResult
                        latest_answer = text_result[:3000]
                        latest_answer = latest_answer.replace(narrate_sorry_noselect_message, "")
                    else:
                        print(f"!isinstance LOB result: {result}")
            except Exception as query_error:
                print(f"An error occurred during query execution: {query_error}")
                latest_thetime = datetime.now()
                latest_question = "default"
                latest_answer = "default"

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

            doTTSAndAudio2Face(latest_answer, latest_question)

    except Exception as e:
        print(f"An error occurred: {e}")

    cummulativeResult = ""

def doTTSAndAudio2Face(latest_answer, latest_question):
    config = oci.config.from_file("~/.oci/config", "DEFAULT")
    speech_client = AIServiceSpeechClient(config)
    print(f"latest_question: {latest_question}")
    print(f"latest_answer: {latest_answer}")
    text_to_speech = SynthesizeSpeechDetails(
        text=f" {latest_answer}",
        is_stream_enabled=True,
    )
    response = speech_client.synthesize_speech(synthesize_speech_details=text_to_speech)
    with open("TTSoutput.wav", "wb") as audio_file:
        audio_file.write(response.data.content)
    print("Speech synthesis completed and saved as TTSoutput.wav")
    url = 'http://localhost:8011/A2F/Player/SetRootPath'
    headers = {
        'accept': 'application/json',
        'Content-Type': 'application/json'
    }
    payload = {
        "a2f_player": "/World/audio2face/Player",
        "dir_path": "C:/oracle-ai-for-sustainable-dev"
    }
    response = requests.post(url, json=payload, headers=headers)
    if response.status_code == 200:
        print("SetRootPath call successful.")
    set_track_url = 'http://localhost:8011/A2F/Player/SetTrack'
    set_track_data = {
        "a2f_player": "/World/audio2face/Player",
        "file_name": "TTSoutput.wav",
        "time_range": [0, -1]
    }
    requests.post(set_track_url, json=set_track_data, headers=headers)
    play_track_url = 'http://localhost:8011/A2F/Player/Play'
    play_track_data = {"a2f_player": "/World/audio2face/Player"}
    requests.post(play_track_url, json=play_track_data, headers=headers)

def handleContainsLogic(cummulative_result, logic_array):
    global latest_thetime, latest_question, latest_answer, latest_action
    for item in logic_array:
        if item["containsWord"] in cummulative_result:
            latest_thetime = datetime.now()
            latest_question = item["latestQuestion"]
            latest_answer = item["containsWord"]
            latest_action = item["containsWord"]
            return True
    return False

async def handle_request_echo(request):
    global latest_thetime, latest_question, latest_answer
    data = {
        "thetime": latest_thetime.isoformat() if latest_thetime else None,
        "question": latest_question,
        "answer": latest_answer
    }
    return web.json_response(text=str(latest_action))


async def handle_request(request):
    # global latest_thetime, latest_question, latest_answer
    global latest_thetime, latest_question, latest_answer, cummulativeResult, isSelect, isRag, isShowSQL, isChat, last_result_time
    print("Received request to handle.")
    latest_question = request.query.get("question", "default question")
    latest_answer = request.query.get("answer", "default answer")

    # doTTSAndAudio2Face(latest_answer, latest_question)



    print(f"Received final results: {latest_question}")
    triggers = ["oracle", "hey db", "hey deebee", "a db", "adb", "they deebee", "hey deebee", "hey sweetie"]
    lowered_cumulative_result = cummulativeResult.lower()
    for trigger in triggers:
        if trigger in lowered_cumulative_result:
            cummulativeResult = cummulativeResult[
                                        lowered_cumulative_result.find(trigger) + len(trigger) + 1:].strip()
            if "use rag" in lowered_cumulative_result:
                cummulativeResult = cummulativeResult.replace("use rag", "")
                isRag = True
            elif "use chat" in lowered_cumulative_result:
                cummulativeResult = cummulativeResult.replace("use chat", "")
                isChat = True
                isSelect = True
            elif "show sql" in lowered_cumulative_result:
                cummulativeResult = cummulativeResult.replace("show sql", "")
                isShowSQL = True
                isSelect = True
            elif "show sequel" in lowered_cumulative_result:
                cummulativeResult = cummulativeResult.replace("show sequel", "")
                isShowSQL = True
                isSelect = True
            elif "show S Q L" in lowered_cumulative_result:
                cummulativeResult = cummulativeResult.replace("show S Q L", "")
                isShowSQL = True
                isSelect = True
            else:
                isSelect = True
                break
        else:
            cummulativeResult = ""

    
    if isSelect:
        executeSelectAI()
        isSelect = False
        isShowSQL = False
        isChat = False
    elif isRag:
        executeSandbox()
        isRag = False
    print(f"Current cummulative result: {cummulativeResult}")
    print(f"isSelect: {isSelect}")
    print(f"isRag: {isRag}")
    # copy paste to webrequest starts here - todo factor out method




    data = {
        "thetime": latest_thetime.isoformat() if latest_thetime else None,
        "question": latest_question,
        "answer": latest_answer
    }
    return web.json_response(text=json.dumps(data))
    # return web.json_response(text=str(latest_action))

async def connect_with_retry(client, max_retries=5, initial_delay=2):
    global is_connected
    delay = initial_delay
    while True:
        try:
            if not is_connected:
                await client.connect()
                print("Connection successful.")
            await asyncio.sleep(1)
        except Exception as e:
            print(f"Connection attempt failed with error: {e}")
            print(f"Retrying in {delay} seconds...")
            await asyncio.sleep(delay)
            delay = min(delay * 2, 60)
        if is_connected:
            delay = initial_delay

if __name__ == "__main__":


    loop = asyncio.get_event_loop()
    # loop.create_task(send_audio(client))
    # loop.create_task(check_idle())

    app = web.Application()
    app.router.add_get('/data', handle_request)
    runner = web.AppRunner(app)
    loop.run_until_complete(runner.setup())
    site = web.TCPSite(runner, '0.0.0.0', 4884)
    loop.run_until_complete(site.start())

    # loop.run_until_complete(connect_with_retry(client))

    try:
        loop.run_forever()
    except KeyboardInterrupt:
        print("Shutting down.")
    finally:
        if stream.is_active():
            stream.stop_stream()
            stream.close()
        loop.run_until_complete(runner.cleanup())
        loop.close()
