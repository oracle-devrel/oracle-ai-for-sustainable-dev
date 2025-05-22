package oracleai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import oracleai.services.ImageGeneration;
import oracleai.services.OracleObjectStore;
import oracleai.services.OracleSpeechAI;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.sound.sampled.*;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/picturestory")
public class GenerateAPictureStoryUsingOnlySpeech {

    static List<String> imageLocations = new ArrayList();

    @GetMapping("/reset")
    public String reset(Model model) {
        imageLocations = new ArrayList();
        model.addAttribute("results", "story board cleared successfully");
        return "resultspage";
    }

    @PostMapping("/picturestory")
    public String picturestory(@RequestParam("opts") String opts,
                               @RequestParam("genopts") String genopts,
                               @RequestParam("file") MultipartFile multipartFile,
                               Model model) throws Exception {
        if (opts.equals("fileaudio") ) return fileaudio(genopts, multipartFile, model);
        else return liveaudio(genopts, model);
    }

    @NotNull
    private String fileaudio(String genopts, MultipartFile multipartFile, Model model) throws Exception {
        OracleObjectStore.sendToObjectStorage(multipartFile.getOriginalFilename(), multipartFile.getInputStream());
        String transcriptionJobId = OracleSpeechAI.getTranscriptFromOCISpeech(multipartFile.getOriginalFilename());
        System.out.println("transcriptionJobId: " + transcriptionJobId);
        String jsonTranscriptFromObjectStorage =
                OracleObjectStore.getFromObjectStorage(transcriptionJobId,
                        AIApplication.OBJECTSTORAGE_NAMESPACE + "_" +
                                AIApplication.OBJECTSTORAGE_BUCKETNAME + "_" +
                                multipartFile.getOriginalFilename() + ".json");
        System.out.println("jsonTranscriptFromObjectStorage: " + jsonTranscriptFromObjectStorage);
        String pictureDescription  = getConcatenatedTokens(jsonTranscriptFromObjectStorage);
        imageLocations.add(ImageGeneration.imagegeneration(pictureDescription + " " + genopts));
        model.addAttribute("imageLocations", imageLocations.toArray(new String[0]));
        return "resultswithimages";
    }

    public String liveaudio(String genopts, Model model) throws Exception {
        AudioFormat format =
                new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100.0f, 16, 1,
                        (16 / 8) * 1, 44100.0f, true);
        SoundRecorder soundRecorder = new SoundRecorder();
        soundRecorder.build(format);
        System.out.println("Start recording ....");
        soundRecorder.start();
        Thread.sleep(8000);
        soundRecorder.stop();
        System.out.println("Stopped recording ....");
        Thread.sleep(5000); //give the process time
        String name = "AISoundClip";
        AudioFileFormat.Type fileType = AudioFileFormat.Type.WAVE;
        AudioInputStream audioInputStream = soundRecorder.audioInputStream;
        System.out.println("Saving...");
        File file = new File(name + "." + fileType.getExtension());
        audioInputStream.reset();
        AudioSystem.write(audioInputStream, fileType, file);
        System.out.println("Saved " + file.getAbsolutePath());
        OracleObjectStore.sendToObjectStorage(file.getName(), new FileInputStream(file));
        String transcriptionJobId = OracleSpeechAI.getTranscriptFromOCISpeech(file.getName());
        System.out.println("transcriptionJobId: " + transcriptionJobId);
        String jsonTranscriptFromObjectStorage =
                OracleObjectStore.getFromObjectStorage(transcriptionJobId,
                        AIApplication.OBJECTSTORAGE_NAMESPACE + "_" +
                                AIApplication.OBJECTSTORAGE_BUCKETNAME + "_" + file.getName() + ".json");
        System.out.println("jsonTranscriptFromObjectStorage: " + jsonTranscriptFromObjectStorage);
        String pictureDescription  = getConcatenatedTokens(jsonTranscriptFromObjectStorage);
        imageLocations.add(ImageGeneration.imagegeneration(pictureDescription + " " + genopts));
        model.addAttribute("imageLocations", imageLocations.toArray(new String[0]));
        return "resultswithimages";
    }

    public String getConcatenatedTokens(String json) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            OracleSpeechAI.TranscriptionResponse response =
                    objectMapper.readValue(json, OracleSpeechAI.TranscriptionResponse.class);
            return response.getTranscriptions().stream()
                    .flatMap(transcription -> transcription.getTokens().stream())
                    .map(OracleSpeechAI.TranscriptionResponse.Transcription.Token::getToken)
                    .collect(Collectors.joining(" "));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public class SoundRecorder implements Runnable {
        AudioInputStream audioInputStream;
        private AudioFormat format;
        public Thread thread;


        public SoundRecorder build(AudioFormat format) {
            this.format = format;
            return this;
        }

        public void start() {
            thread = new Thread(this);
            thread.start();
        }

        public void stop() {
            thread = null;
        }

        @Override
        public void run() {
            try (final ByteArrayOutputStream out = new ByteArrayOutputStream();
                 final TargetDataLine line = getTargetDataLineForRecord();) {
                int frameSizeInBytes = format.getFrameSize();
                int bufferLengthInFrames = line.getBufferSize() / 8;
                final int bufferLengthInBytes = bufferLengthInFrames * frameSizeInBytes;
                buildByteOutputStream(out, line, frameSizeInBytes, bufferLengthInBytes);
                this.audioInputStream = new AudioInputStream(line);
                setAudioInputStream(convertToAudioIStream(out, frameSizeInBytes));
                audioInputStream.reset();
            } catch (IOException ex) {
                ex.printStackTrace();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        public void buildByteOutputStream(final ByteArrayOutputStream out,
                                          final TargetDataLine line, int frameSizeInBytes,
                                          final int bufferLengthInBytes) throws IOException {
            final byte[] data = new byte[bufferLengthInBytes];
            int numBytesRead;

            line.start();
            while (thread != null) {
                if ((numBytesRead = line.read(data, 0, bufferLengthInBytes)) == -1) {
                    break;
                }
                out.write(data, 0, numBytesRead);
            }
        }

        private void setAudioInputStream(AudioInputStream aStream) {
            this.audioInputStream = aStream;
        }

        public AudioInputStream convertToAudioIStream(final ByteArrayOutputStream out, int frameSizeInBytes) {
            byte[] audioBytes = out.toByteArray();
            AudioInputStream audioStream =
                    new AudioInputStream(new ByteArrayInputStream(audioBytes), format,
                            audioBytes.length / frameSizeInBytes);
            System.out.println("Recording finished");
            return audioStream;
        }

        public TargetDataLine getTargetDataLineForRecord() {
            TargetDataLine line;
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            if (!AudioSystem.isLineSupported(info)) {
                throw new UnsupportedOperationException("Line not supported");
            }
            try {
                line = (TargetDataLine) AudioSystem.getLine(info);
                line.open(format, line.getBufferSize());
            } catch (final Exception ex) {
                return null;
            }
            return line;
        }
    }

}
