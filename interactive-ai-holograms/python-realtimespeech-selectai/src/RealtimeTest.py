package com.oracle.pic.ocas.speech;

import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.aispeech.model.CustomizationInference;
import com.oracle.bmc.aispeech.model.RealtimeMessageAckAudio;
import com.oracle.bmc.aispeech.model.RealtimeMessageConnect;
import com.oracle.bmc.aispeech.model.RealtimeMessageResult;
import com.oracle.bmc.aispeech.model.RealtimeParameters;
import com.oracle.bmc.aispeech.realtimespeech.RealtimeSpeechClient;
import com.oracle.bmc.aispeech.realtimespeech.RealtimeSpeechClientListener;
import com.oracle.bmc.aispeech.realtimespeech.exceptions.RealtimeSpeechConnectException;
import com.oracle.bmc.auth.SessionTokenAuthenticationDetailsProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RealtimeTestClient implements RealtimeSpeechClientListener {

  private static final String SERVER = "wss://realtime.aiservice.us-ashburn-1.oci.oraclecloud.com";
  private static final String COMPARTMENT = "ocid1.compartment.oc1..aaaaaaaaq5uqqsqb2gvrjqkh2vugrur26izafhetj5flhpcqn3mnm35zorba";
  private static final String LANGUAGE_CODE = "en-US";
  private static final String MODEL_DOMAIN = "GENERIC";
  private static final String STABILIZE_PARTIAL_RESULTS = "NONE";
  private static final String ENCODING = "audio/raw;rate=16000";
  private static final int PARTIAL_SILENCE_THRESHOLD = 0;
  private static final int FINAL_SILENCE_THRESHOLD = 2000;

  private static final float AUDIO_SAMPLE_RATE = 16_000.0f;
  private static final int AUDIO_SAMPLE_SIZE = 16;
  private static final int AUDIO_NUM_CHANNELS = 1;
  private static final boolean AUDIO_SIGNED = true;
  private static final boolean AUDIO_BIG_ENDIAN = false;

  private String wav;
  private String mulaw;

  private String server = SERVER;
  private String encoding = ENCODING;
  private int partialSilenceThresholdInMs = PARTIAL_SILENCE_THRESHOLD;
  private int finalSilenceThresholdInMs = FINAL_SILENCE_THRESHOLD;
  private String language = LANGUAGE_CODE;
  private String modelDomain = MODEL_DOMAIN;
  private String stabilizePartialResults = STABILIZE_PARTIAL_RESULTS;
  private Boolean shouldIgnoreCustomizationLoadErrors = true;

  private RealtimeSpeechClient realtimeClient;
  final String compartmentId = COMPARTMENT;

  public static void main(String[] args) throws IOException {

    final Options options = new Options();

    options.addOption("help", false, "print this message");
    options.addOption("server", true, "address of realtime server");
    options.addOption("wav", true, "pass a wav file");
    options.addOption("mulaw", true, "pass a mulaw file");
    options.addOption("language", true, "set the language (default is en-US)");
    options.addOption("modelDomain", true, "set the model domain (default is GENERIC)");
    options.addOption("stabilizePartialResults", true, "set the partial result stabilization (default is NONE)");
    options.addOption("shortPauseMs", true, "set the short silence duration (default is 0)");
    options.addOption("longPauseMs", true, "set the long silence duration (default is 2000)");
    options.addOption("ignCustErr", true, "set whether to ignore customization load errors on not (default is false)");
    options.addOption("nSessions", true, "number of concurrent sessions");

    int nSessions = 1;
    final CommandLineParser parser = new DefaultParser();
    final CommandLine cmd;
    try {
      cmd = parser.parse(options, args);
      if (cmd.hasOption("nSessions")) {
        nSessions = Integer.parseInt(cmd.getOptionValue("nSessions"));
      }
    } catch (ParseException e) {
      log.error("Could Not parse nSessions value, defaulting to 1: ", e);
    }

    try {
      final ConfigFileReader.ConfigFile configFile = ConfigFileReader.parseDefault();
      // Can include profile here if needed.
      // final ConfigFileReader.ConfigFile configFile =
      // ConfigFileReader.parse("~/.oci/config", "US-PHOENIX-1");
      ExecutorService executorService = Executors.newFixedThreadPool(20);

      WebSocketClient client = new WebSocketClient();
      client.start();

      for (int i = 0; i < nSessions; i++) {

        final RealtimeTestClient realtimeTestClient = new RealtimeTestClient();
        if (!realtimeTestClient.parseOptions(options, args)) {
          return;
        }

        if (realtimeTestClient.wav != null) {
          boolean res = realtimeTestClient.setFrequency(realtimeTestClient.wav);
          log.info("returned : " + res);
          if (!res) {
            System.exit(1);
          }
        }

        if (realtimeTestClient.mulaw != null) {
          realtimeTestClient.encoding = "audio/raw;rate=8000;codec=mulaw";
        }

        // log.info("Is client started in the parent: {}", client.isStarted());
        startSession(realtimeTestClient, configFile, client);

        // RealtimeSpeechClient.stopClient();
      }

      executorService.shutdown();

      try {
        executorService.awaitTermination(3600, TimeUnit.SECONDS);
        executorService.shutdownNow();

        client.stop();
      } catch (InterruptedException e) {
        executorService.shutdownNow();
        // Preserve interrupt status
        Thread.currentThread().interrupt();
      }

    } catch (ParseException e) {
      log.error("ParseException {}", e.getMessage());
    } catch (IOException e) {
      log.error("Realtime Client: Unable to connect!");
    } catch (Exception e) {
      log.error("Exception Occured: ", e);
    }
    return;
  }

  private boolean setFrequency(String wavfile) {
    try (AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File(wavfile))) {
      final AudioFormat audioFormat = audioInputStream.getFormat();
      log.info("Playing file ({} hz) {} {}", audioFormat.getFrameRate(), audioFormat.getFrameSize(),
          audioFormat.getEncoding());
      switch ((int) audioFormat.getFrameRate()) {
        case 8000:
          if (audioFormat.getFrameSize() == 2 && audioFormat.getEncoding() == Encoding.PCM_SIGNED) {
            encoding = "audio/raw;rate=8000";
            log.info("Encoding set to " + encoding);
          } else if (audioFormat.getFrameSize() == 1 && audioFormat.getEncoding() == Encoding.ULAW) {
            encoding = "audio/raw;rate=8000;codec=mulaw";
            log.info("Encoding set to " + encoding);
          } else if (audioFormat.getFrameSize() == 1 && audioFormat.getEncoding() == Encoding.ALAW) {
            encoding = "audio/raw;rate=8000;codec=alaw";
            log.info("Encoding set to " + encoding);
          } else {
            log.error("Unsupported format");
            return false;
          }
          break;
        case 16_000:
          if (audioFormat.getFrameSize() == 2 && audioFormat.getEncoding() == Encoding.PCM_SIGNED) {
            encoding = "audio/raw;rate=16000";
            log.info("Encoding set to " + encoding);
          } else {
            log.error("Unsupported format");
            return false;
          }
          break;
        default:
          log.error("Unsupported format");
          return false;
      }
    } catch (UnsupportedAudioFileException e) {
      log.error("UnsupportedAudioFileException {}", e.getMessage());
      return false;
    } catch (IOException e) {
      log.error("IOException {}", e.getMessage());
      return false;
    }
    return true;
  }

  private boolean parseOptions(Options options, String[] args) throws ParseException { // NOPMD
    final CommandLineParser parser = new DefaultParser();
    final CommandLine cmd = parser.parse(options, args);

    if (cmd.hasOption("help")) {
      final HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("RealtimeClient", options);
      return false;
    }

    if (cmd.hasOption("server")) {
      server = cmd.getOptionValue("server");
    }
    if (cmd.hasOption("wav")) {
      wav = cmd.getOptionValue("wav");
    }
    if (cmd.hasOption("mulaw")) {
      mulaw = cmd.getOptionValue("mulaw");
    }
    if (cmd.hasOption("language")) {
      language = cmd.getOptionValue("language");
    }
    if (cmd.hasOption("modelDomain")) {
      modelDomain = cmd.getOptionValue("modelDomain");
    }
    if (cmd.hasOption("stabilizePartialResults")) {
        stabilizePartialResults = cmd.getOptionValue("stabilizePartialResults");
    }
    if (cmd.hasOption("shortPauseMs")) {
      partialSilenceThresholdInMs = Integer.parseInt(cmd.getOptionValue("shortPauseMs"));
    }
    if (cmd.hasOption("longPauseMs")) {
      finalSilenceThresholdInMs = Integer.parseInt(cmd.getOptionValue("longPauseMs"));
    }
    if (cmd.hasOption("ignCustErr")) {
      shouldIgnoreCustomizationLoadErrors = Boolean.parseBoolean(cmd.getOptionValue("ignCustErr"));
    }

    return true;

  }

  private void playWav(String wavfile) {
    try (AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File(wavfile))) {
      final int bufferDuration = 96; // 96 ms
      final int bufferSize = ((int) audioInputStream.getFormat().getFrameRate() * 2 * bufferDuration) / 1000;
      workLoop(audioInputStream, bufferDuration, bufferSize);
      realtimeClient.requestFinalResult();
      Thread.sleep(1000);
    } catch (UnsupportedAudioFileException e) {
      log.error("UnsupportedAudioFileException {}", e.getMessage());
      return;
    } catch (IOException e) {
      log.error("IOException {}", e.getMessage());
      return;
    } catch (InterruptedException e) {
      log.error("InterruptedException {}", e.getMessage());
    } catch (RealtimeSpeechConnectException e) {
      log.error("Realtime Client: Connection is closed");
      return;
    } catch (Exception e) {
      log.info("Exception occured: ", e);
      return;
    }
  }

  private void playRaw(String rawfile, int sampleRate, int sampleSize) {
    try (InputStream rawInputStream = new FileInputStream(new File(rawfile))) {
      final int bufferDuration = 96; // 96 ms
      final int bufferSize = ((int) sampleRate * sampleSize * bufferDuration) / 1000;
      workLoop(rawInputStream, bufferDuration, bufferSize);
      realtimeClient.requestFinalResult();
      Thread.sleep(1000);
    } catch (IOException e) {
      log.error("IOException {}", e.getMessage());
      return;
    } catch (InterruptedException e) {
      log.error("InterruptedException {}", e.getMessage());
    } catch (RealtimeSpeechConnectException e) {
      log.error("Realtime Client: Connection is closed");
      return;
    }
  }

  private void playMicrophone() {
    // open mic and start the work loop
    final AudioFormat format = new AudioFormat(
        AUDIO_SAMPLE_RATE,
        AUDIO_SAMPLE_SIZE,
        AUDIO_NUM_CHANNELS,
        AUDIO_SIGNED,
        AUDIO_BIG_ENDIAN);
    final DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

    try (TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(info)) {
      microphone.open(format);
      microphone.start();
      final AudioInputStream ais = new AudioInputStream(microphone);
      final int bufferDuration = 96; // 96 ms
      final int bufferSize = ((int) ais.getFormat().getFrameRate() * ais.getFormat().getFrameSize() * bufferDuration)
          / 1000;

      workLoop(ais, bufferDuration, bufferSize);
    } catch (LineUnavailableException e) {
      log.error("Realtime Client: Unable to get microphone");
    } catch (RealtimeSpeechConnectException e) {
      log.error("Realtime Client: Connection is closed");
    }
  }

  private void workLoop(InputStream inputStream, int simulateRealtimeMs, int bufferSize)
      throws RealtimeSpeechConnectException {
    int res = 0;
    final int zeroLength = 0;

    final byte[] audioBytes = new byte[bufferSize];

    try (InputStream ais = inputStream) {
      int totalBufferSize = 0;
      long start = System.currentTimeMillis();
      log.info("Simulate start");
      while (res >= zeroLength) {
        res = ais.read(audioBytes);
        if (res > zeroLength) {
          realtimeClient.sendAudioData(audioBytes);
          // log.info("Simulate data {} {} {}", res, audioBytes.length, totalBufferSize);
          totalBufferSize += audioBytes.length;
          if (simulateRealtimeMs > 0) {
            try {
              Thread.sleep(simulateRealtimeMs);
            } catch (InterruptedException e) {
              log.error("InterruptedException {}", e.getMessage());
            }
          }
        }
      }
      log.info("Simulate end : {}", System.currentTimeMillis() - start);
    } catch (IOException e) {
      log.error(
          "Realtime Client: Unable to read audio from microphone or send it for transcription!");
    }
  }

  @Override
  public void onClose(int statusCode, String statusMessage) {
    log.info("onNetworkEvent");
  }

  @Override
  public void onAckMessage(RealtimeMessageAckAudio ackMessage) {
    log.info("Ack Message : {}", ackMessage.toString());
  }

  @Override
  public void onResult(RealtimeMessageResult result) {
    if (result.getTranscriptions().size() > 0) {
      if (result.getTranscriptions().get(0).getIsFinal()) {
        log.info(
            "Final Result  : " +
                result.getTranscriptions()
                    .get(0)
                    .getTranscription()
                    .trim()); // TODO remove trim
      } else {
        log.info(
            "Partial Result: " +
                result.getTranscriptions()
                    .get(0)
                    .getTranscription()
                    .trim()); // TODO remove trim
      }
    } else {
      log.error("Result Error : " + result);
    }
  }

  @Override
  public void onError(Throwable error) {
    log.error("Error" + error.toString()); // TODO
    error.printStackTrace();
  }

  @Override
  public void onConnect() {
    log.info("Connected");
  }

  @Override
  public void onConnectMessage(RealtimeMessageConnect realtimeConnectMessage) {
    log.info("Received Connect message for session id: " + realtimeConnectMessage.getSessionId() + " and request id: " + realtimeConnectMessage.getSessionId());
  }

  public static void startSession(RealtimeTestClient realtimeTestClient, ConfigFileReader.ConfigFile configFile,
      WebSocketClient client) {
    try {
      realtimeTestClient.realtimeClient = new RealtimeSpeechClient(
          realtimeTestClient,
          new SessionTokenAuthenticationDetailsProvider(configFile),
          // for API keys use
          // new ConfigFileAuthenticationDetailsProvider(configFile)
          realtimeTestClient.compartmentId,
          client);

       final CustomizationInference cmTest1 = CustomizationInference.builder()
               .customizationAlias("test_alias")
       .build();

      final RealtimeParameters realtimeClientParameters = RealtimeParameters.builder()
          .isAckEnabled(false)
          .partialSilenceThresholdInMs(realtimeTestClient.partialSilenceThresholdInMs)
          .finalSilenceThresholdInMs(realtimeTestClient.finalSilenceThresholdInMs)
          .shouldIgnoreInvalidCustomizations(realtimeTestClient.shouldIgnoreCustomizationLoadErrors)
          .languageCode("en-US")
          .modelDomain(RealtimeParameters.ModelDomain.create(
              realtimeTestClient.modelDomain))
          .encoding(realtimeTestClient.encoding)
          .stabilizePartialResults(RealtimeParameters.StabilizePartialResults.create(
              realtimeTestClient.stabilizePartialResults))
          .customizations(Arrays.asList(cmTest1))
          .build();

      realtimeTestClient.realtimeClient.open(
          realtimeTestClient.server,
          realtimeClientParameters);
    } catch (RealtimeSpeechConnectException e) {
      log.error("Could not connect to the realtime service: ", e);
      realtimeTestClient.realtimeClient.close();
    } catch (IOException e) {
      log.error("IO Exception: ", e);
      realtimeTestClient.realtimeClient.close();
    }

    try {
      while (realtimeTestClient.realtimeClient.getStatus() != RealtimeSpeechClient.Status.CONNECTED) {
        Thread.sleep(10);
      }
    } catch (InterruptedException e) {
      log.error("Realtime Client: Unable to connect!");
      return;
    }

    if (realtimeTestClient.wav != null) {
      realtimeTestClient.playWav(realtimeTestClient.wav);
    } else if (realtimeTestClient.mulaw != null) {
      realtimeTestClient.playRaw(realtimeTestClient.mulaw, 8000, 1);
    } else {
      realtimeTestClient.playMicrophone();
    }

    // Optionally call the realtimeClient.requestFinalResult() to request the
    // service to generate final result.
    // This is particularly useful if the session needs to be closed but a final
    // result has not been received yet.
    // realtimeTestClient.realtimeClient.requestFinalResult();

    realtimeTestClient.realtimeClient.close();
  }

}