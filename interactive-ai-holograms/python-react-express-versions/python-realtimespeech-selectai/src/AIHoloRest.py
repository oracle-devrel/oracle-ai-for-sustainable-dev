import asyncio
import getpass
import os
import json
import requests
import time
import oci
import ssl
from datetime import datetime
import oracledb
from aiohttp import web
from oci.ai_speech import AIServiceSpeechClient
from oci.ai_speech.models import SynthesizeSpeechDetails

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

cummulativeResult = ""
isSelect = False
isRag = False
isChat = False
isShowSQL = False
isRunSQL = False
isExplainSQL = False
last_result_time = None
is_connected = False
isInsertResults = False

def executeSandbox(cummulativeResult: str = None,):
    global isInsertResults, latest_thetime, latest_question, latest_answer
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
        latest_answer = response_data.get('answer', '')
        sources = response_data.get('sources', [])
        sources_str = ', '.join(sources)
        latest_answer = f"{answer} Sources searched: {sources_str}"
        print("RAG Full Response latest_answer:", latest_answer)
        doTTSAndAudio2Face(latest_answer, latest_question)
    else:
        print("Failed to fetch data:", response.status_code, response.text)
    cummulativeResult = ""

def executeSelectAI(cummulativeResult: str = None):
    global isInsertResults, isShowSQL, isRunSQL, isExplainSQL, latest_thetime, latest_question, latest_answer
    # if not cummulativeResult:
    #     cummulativeResult = latest_question or ""
    print(f"executeSelectAI called cummulative result: {cummulativeResult}")
    contains_logic = [
        {"containsWord": "interactive ai holograms", "latestQuestion": "interactive ai holograms", "latestAnswer": "interactive ai holograms is the activation you are seeing now which uses Oracle Database and AI"}
    ]
    contains_logic0 = [
        {"containsWord": "f1", "latestQuestion": "f1", "latestAnswer": "f1"},
        {"containsWord": "f 1", "latestQuestion": "f1", "latestAnswer": "f1"},
        {"containsWord": "eff 1", "latestQuestion": "f1", "latestAnswer": "f1"},
        {"containsWord": "satellite", "latestQuestion": "satellites", "latestAnswer": "satellites"},
        {"containsWord": "satellites", "latestQuestion": "satellites", "latestAnswer": "satellites"},
        {"containsWord": "spatial", "latestQuestion": "spatial", "latestAnswer": "spatial"}
    ]
    # profile_name(s) specify what LLMs, tables, vectors, etc. to use.
    chatquery = """SELECT DBMS_CLOUD_AI.GENERATE(
                prompt       => :prompt,
                profile_name => 'AIHOLO_OCIGENAI', 
                action       => 'chat')"""

    narratequery = """SELECT DBMS_CLOUD_AI.GENERATE(
                prompt       => :prompt,
                profile_name => 'AIHOLO', 
                action       => 'narrate')"""
    
    runsqlquery = """SELECT DBMS_CLOUD_AI.GENERATE(
                prompt       => :prompt,
                profile_name => 'AIHOLO', 
                action       => 'runsql')"""

    showssqlquery = """SELECT DBMS_CLOUD_AI.GENERATE(
                prompt       => :prompt,
                profile_name => 'AIHOLO', 
                action       => 'showsql')"""
    
    explainsqlquery = """SELECT DBMS_CLOUD_AI.GENERATE(
                prompt       => :prompt,
                profile_name => 'AIHOLO', 
                action       => 'explainsql')"""

    if isShowSQL:
        query = showssqlquery
        print("showsql true")
    elif isRunSQL:
        query = runsqlquery
        print("runsql true")
    elif isExplainSQL:
        query = explainsqlquery
        print("isExplainSQL true")
    elif isChat:
        query = chatquery
        print("chat true")
    elif isRag:
        query = narratequery
        print("chat true")
    else:
        query = narratequery

    # cummulativeResult += " ignore case"
    cummulativeResult += " . Make answer one sentence that is shorter than 50 words"

    try:
        with connection.cursor() as cursor:
            try:
                if handleContainsLogic(cummulativeResult.lower(), contains_logic):
                    cummulativeResult = cummulativeResult.replace("show sql", "")
                    print(f"executeSelectAI handled directly with cummulative result: {cummulativeResult}")
                    return
                else:
                    start_time = time.time()
                    print(f"Query : {query} ")
                    cursor.execute(query, {'prompt': cummulativeResult})
                    result = cursor.fetchone()
                    if result and isinstance(result[0], oracledb.LOB):
                        text_result = result[0].read()
                        end_time = time.time()
                        elapsed_time = end_time - start_time
                        print(f"Query execution time: {elapsed_time:.4f} seconds")

                        latest_thetime = datetime.now()
                        latest_question = cummulativeResult
                        latest_answer = text_result[:3000]
                        latest_answer = latest_answer.replace(narrate_sorry_noselect_message, "")
                        print(f"doTTSAndAudio2Face: {latest_answer}")
                        doTTSAndAudio2Face(latest_answer, latest_question)
                        print(f"---doTTSAndAudio2Face finished---")
                        
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

    except Exception as e:
        print(f"An error occurred: {e}")

    cummulativeResult = ""

def doTTSAndAudio2Face(latest_answer, latest_question):
    config = oci.config.from_file("~/.oci/config", "DEFAULT")
    speech_client = AIServiceSpeechClient(config)
    print(f"latest_question: {latest_question}")
    print(f"latest_answer: {latest_answer}")
    # text_to_speech = SynthesizeSpeechDetails(
    #     text=f" {latest_answer}",
    #     is_stream_enabled=True,
    # )
    text_to_speech = SynthesizeSpeechDetails(
        text=f" {latest_answer}",
        is_stream_enabled=False,
        configuration=oci.ai_speech.models.TtsOracleConfiguration(
            model_family="ORACLE",
            # Brian Annabelle Bob Stacy Phil Cindy Brad 
            model_details=oci.ai_speech.models.TtsOracleTts2NaturalModelDetails(voice_id="Brian"),
            speech_settings=oci.ai_speech.models.TtsOracleSpeechSettings(
                speech_mark_types=["WORD"]
            ),
        )    
    )
    # list_voices_response = speech_client.list_voices()
    # print(list_voices_response.data)
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
        "dir_path": "C:/Users/opc/src/github.com/paulparkinson/oracle-ai-for-sustainable-dev/interactive-ai-holograms/python-realtimespeech-selectai"
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
        
SSL_CERT_FILE = r"C:\Users\opc\src\github.com\paulparkinson\oracle-ai-for-sustainable-dev\interactive-ai-holograms\localhost.pem"  # Path to your certificate
SSL_KEY_FILE = r"C:\Users\opc\src\github.com\paulparkinson\oracle-ai-for-sustainable-dev\interactive-ai-holograms\localhost-key.pem"  # Path to your private key


async def handle_request(request):
    global latest_thetime, latest_question, isSelect, isRag, isShowSQL, isRunSQL, isExplainSQL, isChat, last_result_time, cummulativeResult
    print("Received request to handle.")
    # latest_question = request.query.get("question", "default question")
    
    latest_question = request.query.get("question", None)

    if latest_question is None:
        # Show HTML form if question param is not present
        html_form = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Oracle AI Service</title>
        </head>
        <body>
            <h1>Oracle AI App</h1>
            <form method="GET" action="/data">
                <label for="question">Enter your question:</label>
                <input type="text" id="question" name="question">
                <button type="submit">Submit</button>
            </form>
        </body>
        </html>
        """
        return web.Response(text=html_form, content_type='text/html')



    cummulativeResult = latest_question  # This ensures the cumulative result is set directly from the question

    print(f"latest_question: {latest_question}")
    print(f"cummulativeResult: {cummulativeResult}")

    lowered_cumulative_result = cummulativeResult.lower()
    
    print(f"Current cummulative result before trimming `use xyz`: {cummulativeResult}")
    print(f"isSelect: {isSelect}")
    print(f"isShowSQL: {isShowSQL}")
    print(f"isRunSQL: {isRunSQL}")
    print(f"isExplainSQL: {isExplainSQL}")
    print(f"isRag: {isRag}")
    print(f"isChat: {isChat}")

    if "use rag" in lowered_cumulative_result:
        cummulativeResult = cummulativeResult.replace("use rag", "")
        print(f"use rag issued: {cummulativeResult}")
        isRag = True
    elif "use database" in lowered_cumulative_result:
        cummulativeResult = cummulativeResult.replace("use database", "")
        print(f"use database issued: {cummulativeResult}")
        isRag = True
    elif "use chat" in lowered_cumulative_result:
        cummulativeResult = cummulativeResult.replace("use chat", "")
        isChat = True
        isSelect = True
    elif "run sql" in lowered_cumulative_result:
        cummulativeResult = cummulativeResult.replace("run sql", "")
        isRunSQL = True
        isSelect = True
    elif "show sql" in lowered_cumulative_result:
        cummulativeResult = cummulativeResult.replace("show sql", "")
        isShowSQL = True
        isSelect = True
    elif "explain sql" in lowered_cumulative_result:
        cummulativeResult = cummulativeResult.replace("explain sql", "")
        isExplainSQL = True
        isSelect = True
    else:
        isChat = True
        isSelect = True

    print(f"Current cummulative result: {cummulativeResult}")
    print(f"isSelect: {isSelect}")
    print(f"isShowSQL: {isShowSQL}")
    print(f"isRunSQL: {isRunSQL}")
    print(f"isExplainSQL: {isExplainSQL}")
    print(f"isRag: {isRag}")
    print(f"isChat: {isChat}")

    if isSelect:
        executeSelectAI(cummulativeResult)
        isSelect = False
        isShowSQL = False
        isRunSQL = False
        isExplainSQL = False
        isChat = False
    elif isRag:
        executeSelectAI(cummulativeResult)
        # executeSandbox(cummulativeResult)
        isSelect = False
        isShowSQL = False
        isRunSQL = False
        isExplainSQL = False
        isChat = False

    data = {
        "thetime": latest_thetime.isoformat() if latest_thetime else None,
        "question": latest_question,
        "answer": latest_answer
    }
    return web.json_response(text=json.dumps(data))

async def main():

    app = web.Application()
    app.router.add_get('/data', handle_request)

    # Set up SSL context for HTTPS
    ssl_context = ssl.create_default_context(ssl.Purpose.CLIENT_AUTH)
    ssl_context.load_cert_chain(certfile=SSL_CERT_FILE, keyfile=SSL_KEY_FILE)

    runner = web.AppRunner(app)
    await runner.setup()

    # Create an HTTPS server
    site = web.TCPSite(runner, '0.0.0.0', 8443, ssl_context=ssl_context)
    await site.start()

    print("HTTPS server is running on https://0.0.0.0:8443")

    # app = web.Application()
    # app.router.add_get('/data', handle_request)
    # runner = web.AppRunner(app)
    # await runner.setup()
    # site = web.TCPSite(runner, '0.0.0.0', 8080)
    # await site.start()

    # print("Server is running and listening on port 8080")

    # Keep the app running
    while True:
        await asyncio.sleep(3600)

if __name__ == "__main__":
    asyncio.run(main())
