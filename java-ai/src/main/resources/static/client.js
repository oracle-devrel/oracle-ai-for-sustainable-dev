const socket = new WebSocket("wss://141.148.204.74:8444/speech");

socket.onopen = () => {
    console.log("Connected to Secure Speech WebSocket Server");
    startRecording();
};

socket.onmessage = (event) => {
    console.log("Transcription:", event.data);
    document.getElementById("transcription").innerText += event.data + " ";
};

socket.onerror = (error) => {
    console.error("WebSocket Error:", error);
};

socket.onclose = () => {
    console.log("Disconnected from Secure Speech WebSocket Server");
};

// Start audio streaming
function startRecording() {
    navigator.mediaDevices.getUserMedia({ audio: true })
        .then(stream => {
            const audioContext = new AudioContext();
            const mediaStreamSource = audioContext.createMediaStreamSource(stream);
            const scriptProcessor = audioContext.createScriptProcessor(4096, 1, 1);

            scriptProcessor.onaudioprocess = (event) => {
                if (socket.readyState === WebSocket.OPEN) {
                    const inputBuffer = event.inputBuffer.getChannelData(0);
                    const int16Array = convertFloat32ToInt16(inputBuffer);
                    socket.send(int16Array);
                }
            };

            mediaStreamSource.connect(scriptProcessor);
            scriptProcessor.connect(audioContext.destination);
        })
        .catch(error => {
            console.error("Error accessing microphone:", error);
        });
}

// Convert Float32 to Int16 for streaming
function convertFloat32ToInt16(buffer) {
    const int16Array = new Int16Array(buffer.length);
    for (let i = 0; i < buffer.length; i++) {
        const s = Math.max(-1, Math.min(1, buffer[i]));
        int16Array[i] = s < 0 ? s * 0x8000 : s * 0x7FFF;
    }
    return int16Array;
}
