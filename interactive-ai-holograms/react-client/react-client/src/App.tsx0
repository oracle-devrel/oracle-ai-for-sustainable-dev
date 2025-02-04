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
  const finalResultRef = React.useRef(""); // Ref to track the latest finalResult

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
  const [isLoading, setIsLoading] = React.useState(false);

  // Update the ref whenever finalResult changes
  React.useEffect(() => {
    finalResultRef.current = finalResult;
  }, [finalResult]);

  const getRegion = async (): Promise<{ region: string }> => {
    try {
      const response = await fetch("https://130.61.51.75:8448/region", {
        method: "GET",
        headers: { "Content-Type": "application/json" },
      });

      if (!response.ok) {
        throw new Error(`HTTP error! Status: ${response.status}`);
      }

      const result = await response.json();
      console.log("Region fetched:", result);
      return result;
    } catch (error) {
      console.error("Error fetching region:", error);
      setErrors((prevErrors) => [...prevErrors, `Error fetching region: ${error.message}`]);
      return { region: "" }; // Return a fallback object
    }
  };

  const authenticate = async (): Promise<{ token: string; sessionId: string; compartmentId: string }> => {
    try {
      const response = await fetch("https://130.61.51.75:8448/authenticate", {
        method: "GET",
        headers: { "Content-Type": "application/json" },
      });

      if (!response.ok) {
        throw new Error(`HTTP error! Status: ${response.status}`);
      }

      const result = await response.json();
      console.log("Authentication response:", result);
      return result;
    } catch (error) {
      console.error("Error in fetching session token:", error.message);
      throw error; // Propagate the error to `startSession` for handling
    }
  };

  const handleCallSelectAI = async (question: string): Promise<void> => {
    if (!question) {
      console.error("handleCallSelectAI: No question provided");
      setAiResponse("Error: No question provided");
      return;
    }

    console.log("handleCallSelectAI question:", question);
    const url = `https://130.61.51.75:8443/data?question=${encodeURIComponent(question)}`;
    const urlJava = `https://130.61.51.75:8444/echo/set?value=question`;
    console.log("handleCallSelectAI url:", url);
    setIsLoading(true);

    try {


      // const responseUnreal = await fetch(urlJava, {
      //   method: "GET",
      //   headers: { Accept: "application/json" },
      // });

      // if (!responseUnreal.ok) {
      //   throw new Error(`HTTP error! Status: ${responseUnreal.status}`);
      // }
      // const data2 = await responseUnreal; //currently just thrown away

      
      const response = await fetch(url, {
        method: "GET",
        headers: { Accept: "application/json" },
      });

      if (!response.ok) {
        throw new Error(`HTTP error! Status: ${response.status}`);
      }
      const data = await response.json();



      

      setAiResponse(JSON.stringify(data, null, 2));
    } catch (error) {
      console.error("Error fetching AI data:", error.message);
      setAiResponse(`Error: ${error.message}`);
    } finally {
      setIsLoading(false);
    }


  };

  React.useEffect(() => {
    if (message) {
      setResultStream((prevStream) => [message, ...prevStream]);
      const data = message;

      if (data?.event === "RESULT") {
        const transcriptions = data.transcriptions;
        if (transcriptions[0].isFinal) {
          setFinalResult((prev) => {
            const newResult = prev + (prev.length > 0 ? " " : "") + transcriptions[0].transcription;
            console.log("Updated finalResult:", newResult);
            return newResult;
          });
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

    const { region } = await getRegion();
    if (!region) {
      console.error("Region not available, session cannot start");
      return;
    }

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
        console.log("WebSocket Client Connected");
      },
      onConnectMessage(connectMessage) {
        console.log("WebSocket Client Authenticated:", connectMessage);
        setResultStream((prevStream) => [connectMessage, ...prevStream]);
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
        console.log("WebSocket server closed:", closeEvent.code, closeEvent.reason);
      },
    };

    try {
      const tokenObject = await authenticate();
      console.log("Token received", tokenObject);
      setTokenTime(new Date().getTime());
      if (!tokenObject.sessionId) {
        console.error("Invalid session token");
        setTokenDetails(JSON.stringify(tokenObject, undefined, 4));
        return;
      }
      setTokenDetails(JSON.stringify(tokenObject, undefined, 4));

      realtimeWebSocket.current = new AIServiceSpeechRealtimeApi(
        realtimeEventListener,
        tokenObject.token,
        tokenObject.compartmentId,
        `wss://realtime.aiservice.${region}.oci.oraclecloud.com/ws/transcribe/stream`,
        realtimeClientParameters
      );

      realtimeWebSocket.current.connect();
    } catch (error) {
      console.error("Failed to start session:", error.message);
    }
  };

  const stopSession = () => {
    try {
      console.log("stopSession...");
      const latestFinalResult = finalResultRef.current;
      console.log("finalResult before handleCallSelectAI:", latestFinalResult);
      realtimeWebSocket.current.close();
      handleCallSelectAI(latestFinalResult);
    } catch (e) {
      console.error(e);
    }
  };

  const requestFinalResult = () => {
    realtimeWebSocket.current && realtimeWebSocket.current.requestFinalResult();
  };

  
  const mirrorMe = () => {
    console.log("switch to mirrorMe....");
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
        <h5>Click 'Start session' and ask DeeBee/DB a question. </h5>
        <br />
        <h5>Examples...</h5>
        'What is the latest version of the Oracle Database?'
        <br />
        'What is the latest version of the Oracle Database? Use Rag.'
        <br />
        'Whats is the best video game?'
        <br />
        'Whats is the best video game. Use Database'
        <br />
        'What is Oracle for Startups? Use Database'
        <br />
        <span>
          <button onClick={() => (buttonState ? stopSession() : startSession())}>
            {buttonState ? "Stop Session and Submit Question" : "Start session and Ask Question"}
          </button>
          
          {/* <button onClick={requestFinalResult}>Request Final Result</button> */}
          <button
            onClick={() => {
              buttonState && stopSession();
              reset();
            }}
          >
            Clear screen
          </button>
          <br /><input
            type="text"
            value={textFieldValue}
            onChange={(e) => setTextFieldValue(e.target.value)}
            placeholder="Enter your question"
          />
          <button onClick={() => handleCallSelectAI(textFieldValue)}>Call Select AI</button>
          <br /><button onClick={mirrorMe}>Switch To "Mirror Me"</button>
        </span>
    
      </div>
      <hr />
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
     
   
     
    </div>
  );
}

export default App;
