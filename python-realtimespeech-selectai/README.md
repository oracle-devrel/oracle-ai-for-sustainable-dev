# Python example client for AI Speech Realtime SDK

## Setup

1. Change Directory:
   ```bash
   cd ./speech-packages/ai-speech-realtime-sdk/example-clients/ai-speech-realtime-client-python
   ```
2. In RealtimeClient.py,
   update **line 144** with your `compartmentId`
   update **line 32** with your `authentication method` and `profile`
3. Example import:
   ```python
   from oci.ai_speech_realtime import (RealtimeClient, RealtimeClientListener, RealtimeParameters)
   ```
4. ON VPN →
   ```bash
   pip install -r requirements.txt
   ```

## Usage

1. Run Python File →
   ```bash
   python RealtimeClient.py
   ```
2. In RealtimeClient.py,
   **Lines from 52-59** can be used to modify the various parameters for the audio capture.

   **Lines from 112-132** can be used to modify the various parameters for the realtime service.

   **Line 137** can be updated with a different endpoint if required.
