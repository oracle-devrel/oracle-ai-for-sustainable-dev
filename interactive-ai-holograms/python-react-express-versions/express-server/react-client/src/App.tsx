import React from "react";
import "./App.css";
import {
  AIServiceSpeechRealtimeApi,
  RealtimeParameters,
  RealtimeClientListener,
  RealtimeMessageAckAudio,
  RealtimeMessageResult,
  RealtimeParametersModelDomainEnum,
  RealtimeParametersStabilizePartialResultsEnum,
} from "oci-aispeech-realtime-web";

function App() {
  const realtimeWebSocket = React.useRef<AIServiceSpeechRealtimeApi>();

  const [tokenDetails, setTokenDetails] = React.useState("");
  const [errors, setErrors] = React.useState([]);
  const [buttonState, setButtonState] = React.useState(false);
  const [textFieldValue, setTextFieldValue] = React.useState("");
  const [aiResponse, setAiResponse] = React.useState("");

  const [message, setMessage] = React.useState<RealtimeMessageResult>();
  const [resultStream, setResultStream] = React.useState([]);
  const [partialResult, setPartialResult] = React.useState("");
  const [finalResult, setFinalResult] = React.useState("");

  const [startTime, setStartTime] = React.useState(0);
  const [tokenTime, setTokenTime] = React.useState(0);
  const [connectionTime, setConnectionTime] = React.useState(0);
  const [authTime, setAuthTime] = React.useState(0);

  const handleCallSelectAI = async () => {
    const url = `https://130.61.51.75:8443/data?question=${encodeURIComponent(textFieldValue)}`;
    try {
      const response = await fetch(url, {
        method: 'GET',
        headers: {
          'Accept': 'application/json',
        }
      });
      if (!response.ok) {
        throw new Error(`HTTP error! Status: ${response.status}`);
      }
      const data = await response.json();
      console.log('Response from AI call:', data);
      setAiResponse(JSON.stringify(data, null, 2));
    } catch (error) {
      console.error('Error fetching AI data:', error.message);
      setAiResponse(`Message sent - see server for details if metahuman doesn't reply`);
      // setAiResponse(`Failed to fetch data: ${error.message}`);
    }
  };

  const getRegion = async () => {
    try {
      const response = await fetch("https://130.61.51.75:8448/region", {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
        },
      });
      const result = await response.text();
      console.log("Server Response:", JSON.parse(result));
      return JSON.parse(result);
    } catch (error) {
      console.error("Error in fetching region :", error);
      setErrors((prevErrors) => [...prevErrors, "Error in fetching region:" + JSON.stringify(error)]);
      return {};
    }
  };

  const authenticate = async () => {
    try {
      const response = await fetch("https://130.61.51.75:8448/authenticate", {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
        },
      });
      if (response.ok) {
        const result = await response.text();
        console.log("Server Response:", JSON.parse(result));
        return JSON.parse(result);
      } else {
        console.error("Error in fetching session token:", response);
        setErrors((prevErrors) => [...prevErrors, "Error in fetching session token: " + response.status + " " + response.statusText]);
        return {};
      }
    } catch (error) {
      console.error("Error in fetching session token:", error);
      setErrors((prevErrors) => [...prevErrors, "Error in fetching session token: " + JSON.stringify(error)]);
      return {};
    }
  };

  React.useEffect(() => {
    if (message) {
      setResultStream((resultStream) => [message, ...resultStream]);
      let data = message;
      console.log(data);
      if (data && data.event === "RESULT") {
        let transcriptions = data.transcriptions;
        if (transcriptions[0].isFinal) {
          setFinalResult((finalResult) => finalResult + (finalResult.length > 0 ? " " : "") + transcriptions[0].transcription);
          setPartialResult("");
        } else {
          setPartialResult(transcriptions[0].transcription);
        }
      }
    }
  }, [message]);

  const startSession = async () => {
    reset();
    setStartTime(new Date().getTime());

    const serviceRegion = (await getRegion()).region;

    const realtimeClientParameters: RealtimeParameters = {
      customizations: [],
      languageCode: "en-US",
      modelDomain: RealtimeParametersModelDomainEnum.GENERIC,
      stabilizePartialResults: RealtimeParametersStabilizePartialResultsEnum.NONE,
      partialSilenceThresholdInMs: 0,
      finalSilenceThresholdInMs: 1000,
      shouldIgnoreInvalidCustomizations: false,
      encoding: "audio/raw;rate=16000",
    };

    const realtimeEventListener: RealtimeClientListener = {
      onAckAudio(ackMessage: RealtimeMessageAckAudio) {},
      onConnect(openEvent) {
        setButtonState(true);
        setConnectionTime(new Date().getTime());
        console.log("WebSocket Client Connected ");
      },
      onConnectMessage(connectMessage) {
        console.log("WebSocket Client Authenticated: ", connectMessage);
        setResultStream([connectMessage, ...resultStream]);
        setAuthTime(new Date().getTime());
      },
      onError(error) {
        setButtonState(false);
        setErrors((prevErrors) => [...prevErrors, error.name + " " + error.message]);
        console.error(error);
      },
      onResult(resultMessage) {
        console.log(resultMessage);
        setMessage(resultMessage);
      },
      onClose(closeEvent) {
        setButtonState(false);
        console.log("WebSocket server closed: ", closeEvent.code, closeEvent.reason);
      },
    };

    authenticate().then((tokenObject) => {
      console.log("Token received", tokenObject);
      setTokenTime(new Date().getTime());
      if (tokenObject.sessionId === undefined) {
        setTokenDetails(JSON.stringify(tokenObject, undefined, 4));
        stopSession();
        return;
      }
      setTokenDetails(JSON.stringify(tokenObject, undefined, 4));

      realtimeWebSocket.current = new AIServiceSpeechRealtimeApi(
        realtimeEventListener,
        tokenObject.token,
        tokenObject.compartmentId,
        `wss://realtime.aiservice.${serviceRegion}.oci.oraclecloud.com/ws/transcribe/stream`,
        realtimeClientParameters
      );

      realtimeWebSocket.current.connect();
    });
  };

  const stopSession = () => {
    try {
      realtimeWebSocket.current.close();
    } catch (e) {
      console.error(e);
    }
  };

  const requestFinalResult = () => {
    realtimeWebSocket.current && realtimeWebSocket.current.requestFinalResult();
  };

  const reset = () => {
    setAuthTime(0);
    setConnectionTime(0);
    setTokenTime(0);
    setStartTime(0);
    setFinalResult("");
    setPartialResult("");
    setMessage(undefined);
    setResultStream([]);
    setTokenDetails("");
    setErrors([]);
  };

  return (
    <div className="App">
      <div>
        <h3>Interactive AI Holograms</h3>
        <h5>Click 'Start session' and ask DeeBee/DB a question. Examples...</h5>
        <br/>'Hey DeeBee, Whats is the most popular video game' to query LLM directly
        <br/>'Hey DeeBee, Whats is the most popular video game. Use RAG' to use NL2SQL and Vector/RAG search on private data
        <span>
          <button onClick={() => buttonState ? stopSession() : startSession()}>
            {buttonState ? "Stop Session" : "Start session"}
          </button>
          <button onClick={requestFinalResult}>Request final result</button>
          <button onClick={() => { buttonState && stopSession(); reset(); }}>
            Clear screen
          </button>
          <input type="text" value={textFieldValue} onChange={(e) => setTextFieldValue(e.target.value)} placeholder="Enter your question" />
          <button onClick={handleCallSelectAI}>Call Select AI</button>
        </span>
        {aiResponse && (
          <div>
            <h4>AI Response:</h4>
            <pre>{aiResponse}</pre>
          </div>
        )}
      </div>
      <hr />
      {/* Additional display components for various events, errors, and messages */}
      {startTime > 0 && (
        <>
          <h6>
            <pre>Start time - {new Date(startTime).toLocaleString()}</pre>
            {tokenTime > 0 && (
              <pre>
                Token received - {new Date(tokenTime).toLocaleString()} (+{tokenTime - startTime} ms)
              </pre>
            )}
            {connectionTime > 0 && (
              <pre>
                WebSocket connected - {new Date(connectionTime).toLocaleString()} (+{connectionTime - tokenTime} ms)
              </pre>
            )}
            {authTime > 0 && (
              <pre>
                Realtime authenticated - {new Date(authTime).toLocaleString()} (+{authTime - connectionTime} ms)
              </pre>
            )}
          </h6>
        </>
      )}
      {errors.length > 0 && (
        <div>
          <h4>Errors:</h4>
          {errors.map((error, index) => (
            <pre key={index}>{error}</pre>
          ))}
        </div>
      )}
      {resultStream.length > 0 && (
        <div>
          <h4>Transcriptions:</h4>
          <pre style={{ textWrap: "pretty" }}>
            {finalResult + (finalResult.length > 0 ? " " : "")}
            <em>{partialResult}</em>
          </pre>
        </div>
      )}
      {resultStream.length > 0 && (
        <div>
          <h4>WebSocket Messages:</h4>
          <pre>{JSON.stringify(resultStream, undefined, 4)}</pre>
        </div>
      )}
    </div>
  );
}

export default App;
