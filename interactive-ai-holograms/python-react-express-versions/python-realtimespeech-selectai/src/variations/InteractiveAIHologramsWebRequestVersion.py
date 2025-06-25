import asyncio
import os
import json
import requests
from datetime import datetime
import oracledb
import oci
from oci.config import from_file
from oci.ai_speech import AIServiceSpeechClient
from oci.ai_speech.models import SynthesizeSpeechDetails
from aiohttp import web

latest_thetime = None
latest_question = None
latest_answer = None
latest_action = None
narrate_sorry_noselect_message = "Sorry, unfortunately a valid SELECT statement could not be generated for your natural language prompt. Here is some more information to help you further:"

compartment_id = os.getenv('COMPARTMENT_ID')
print(f"compartment_id: {compartment_id}")

# connection = oracledb.connect(
#     user="moviestream",
#     password="Welcome12345",
#     dsn="selectaidb_high",
#     config_dir=r"C:\Users\paulp\Downloads\Wallet_SelectAIDB",
#     wallet_location=r"C:\Users\paulp\Downloads\Wallet_SelectAIDB",
#     wallet_password="Welcome12345"
# )
# print(f"Successfully connected to Oracle Database Connection: {connection}")

async def handle_request(request):
    global latest_thetime, latest_question, latest_answer
    print("Received request to handle.")
    latest_question = request.query.get("question", "default question")
    latest_answer = request.query.get("answer", "default answer")

    doTTSAndAudio2Face(latest_answer, latest_question)
    data = {
        "thetime": latest_thetime.isoformat() if latest_thetime else None,
        "question": latest_question,
        "answer": latest_answer
    }
    return web.json_response(text=json.dumps(data))
    # return web.json_response(text=str(latest_action))

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
        "dir_path": "C:/aiholo-app/python-realtimespeech-selectai"
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

if __name__ == "__main__":
    app = web.Application()
    app.router.add_get('/data', handle_request)
    runner = web.AppRunner(app)
    loop = asyncio.get_event_loop()

    loop.run_until_complete(runner.setup())
    site = web.TCPSite(runner, '0.0.0.0', 4884)
    loop.run_until_complete(site.start())

    try:
        loop.run_forever()
    except KeyboardInterrupt:
        print("Shutting down.")
    finally:
        loop.run_until_complete(runner.cleanup())
        loop.close()
