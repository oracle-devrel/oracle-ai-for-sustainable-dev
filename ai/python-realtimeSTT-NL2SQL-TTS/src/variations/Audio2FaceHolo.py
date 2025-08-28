import requests
import time
import os

# Base URL for the REST API (ensure that this is correct)
BASE_URL = "http://localhost:8011/A2F"

# Path to the audio file you want to load
audio_file_path = r"C:\Users\paulp\AppData\Local\ov\pkg\deps\b9070d65cb1908ec472cf47bc29f8126\exts\omni.audio2face.player_deps\deps\audio2face-data\tracks\output.wav"

# Function to load the audio file
def load_audio_file():
    url = f"{BASE_URL}/USD/Load"
    data = {
        "file_path": audio_file_path
    }
    response = requests.post(url, json=data)
    if response.status_code == 200:
        print("Audio file loaded successfully!")
    else:
        print(f"Failed to load audio file: {response.status_code}")

# Function to play the audio
def play_audio():
    url = f"{BASE_URL}/USD/Play"
    response = requests.post(url)
    if response.status_code == 200:
        print("Audio started playing!")
    else:
        print(f"Failed to play audio: {response.status_code}")

# Function to stop the audio
def stop_audio():
    url = f"{BASE_URL}/USD/Stop"
    response = requests.post(url)
    if response.status_code == 200:
        print("Audio stopped successfully!")
    else:
        print(f"Failed to stop audio: {response.status_code}")

# Monitor the audio file and reload it when updated
last_modified_time = None

def monitor_audio_file():
    global last_modified_time

    while True:
        # Check the last modified time of the audio file
        current_modified_time = os.path.getmtime(audio_file_path)

        # If the file has been modified, reload and play it
        if last_modified_time is None or current_modified_time > last_modified_time:
            stop_audio()  # Stop any existing audio playback
            load_audio_file()  # Load the updated audio file
            play_audio()  # Play the audio automatically
            last_modified_time = current_modified_time

        # Wait 5 seconds before checking again
        time.sleep(5)

# Start monitoring the audio file for changes
monitor_audio_file()
