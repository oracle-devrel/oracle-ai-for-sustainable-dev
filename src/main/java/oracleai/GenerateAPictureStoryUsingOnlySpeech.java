package oracleai;

import com.oracle.bmc.Region;
import com.oracle.bmc.aispeech.AIServiceSpeechClient;
import com.oracle.bmc.aispeech.model.*;
import com.oracle.bmc.aispeech.requests.CreateTranscriptionJobRequest;
import com.oracle.bmc.aispeech.requests.GetTranscriptionJobRequest;
import com.oracle.bmc.aispeech.responses.CreateTranscriptionJobResponse;
import com.oracle.bmc.aispeech.responses.GetTranscriptionJobResponse;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.responses.GetObjectResponse;
import com.oracle.bmc.objectstorage.responses.PutObjectResponse;
import com.theokanning.openai.image.CreateImageRequest;
import com.theokanning.openai.service.OpenAiService;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Controller
@RequestMapping("/picturestory")
public class GenerateAPictureStoryUsingOnlySpeech {

    static List<String> imageLocations = new ArrayList();


    @GetMapping("/picturestory1")
    public String picturestory1(Model model) throws Exception {
        imageLocations.add("/images/bloodsugarreport.jpeg");
        imageLocations.add("/images/objectdetectiontestimage.jpg");
        model.addAttribute("imageLocations", imageLocations.toArray(new String[0]));
        return "resultswithimages";
    }

    @GetMapping("/picturestory")
    public String picturestory(@RequestParam("genopts") String genopts, Model model) throws Exception {
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
        sendToObjectStorage(file);

        String transcriptionJobId = getTranscriptFromOCISpeech(file.getName());  // //oradbclouducm_aitests_leia.m4a.json
//        String transcriptionJobId = getTranscriptFromOCISpeech("testing123.wav");
        System.out.println("transcriptionJobId: " + transcriptionJobId);
        String jsonTranscriptFromObjectStorage =
                getFromObjectStorage(transcriptionJobId,
//                        AIApplication.OBJECTSTORAGE_NAMESPACE + "_" + AIApplication.OBJECTSTORAGE_BUCKETNAME + "_" + "testing123.wav" + ".json"));
                        AIApplication.OBJECTSTORAGE_NAMESPACE + "_" + AIApplication.OBJECTSTORAGE_BUCKETNAME + "_" + file.getName() + ".json");
        System.out.println("jsonTranscriptFromObjectStorage: " + jsonTranscriptFromObjectStorage);
//        System.out.println("getFromObjectStorage: " + getFromObjectStorage("leia.m4a"));
//        String pictureDescription  = parse(jsonTranscriptFromObjectStorage);
        String pictureDescription = "man rowing a boat through the forest";
        imageLocations.add(imagegeneration(pictureDescription + " " + genopts));
        model.addAttribute("imageLocations", imageLocations.toArray(new String[0]));

        return "resultswithimages";
        /**
         String transcription = transcribe(file) + genopts;
         System.out.println("transcription " + transcription);
         String imageLocation = imagegeneration(transcription);
         System.out.println("imageLocation " + imageLocation);
         storyImages.add(imageLocation);
         String htmlStoryFrames = "";
         Iterator<String> iterator = storyImages.iterator();
         while(iterator.hasNext()) {
         htmlStoryFrames += "<td><img src=\"" + iterator.next() +"\" width=\"400\" height=\"400\"></td>";
         }
         return getHtmlString(htmlStoryFrames);
         */
    }

    private String getTranscriptFromOCISpeech(String fileName) throws IOException {

        AIServiceSpeechClient client;
        AuthenticationDetailsProvider provider;
        if (true) {
            provider = getAuthenticationDetailsProvider();
            client =
                    AIServiceSpeechClient.builder().region(Region.US_CHICAGO_1).build(provider);
//                    ObjectStorageClient.builder().region(Region.US_CHICAGO_1.US_PHOENIX_1).build(provider);
        } else {
//            aiServiceVisionClient = new AIServiceVisionClient(InstancePrincipalsAuthenticationDetailsProvider.builder().build());
        }
        CreateTranscriptionJobDetails createTranscriptionJobDetails = CreateTranscriptionJobDetails.builder()
                //          .displayName("EXAMPLE-displayName-Value")
                .compartmentId(AIApplication.COMPARTMENT_ID)
                //               .description("EXAMPLE-description-Value")
                .additionalTranscriptionFormats(new ArrayList<>(Arrays.asList(CreateTranscriptionJobDetails.AdditionalTranscriptionFormats.Srt)))
                .modelDetails(TranscriptionModelDetails.builder()
                        .domain(TranscriptionModelDetails.Domain.Generic)
                        .languageCode(TranscriptionModelDetails.LanguageCode.EnUs)
                        .transcriptionSettings(TranscriptionSettings.builder()
                                .diarization(Diarization.builder()
                                        .isDiarizationEnabled(false)
                                        .numberOfSpeakers(7).build()).build()).build())
                .normalization(TranscriptionNormalization.builder()
                        .isPunctuationEnabled(true)
                        .filters(new ArrayList<>(Arrays.asList(ProfanityTranscriptionFilter.builder()
                                .mode(ProfanityTranscriptionFilter.Mode.Mask).build()))).build())
                .inputLocation(ObjectListInlineInputLocation.builder()
                        .objectLocations(new ArrayList<>(Arrays.asList(ObjectLocation.builder()
                                .namespaceName(AIApplication.OBJECTSTORAGE_NAMESPACE)
                                .bucketName(AIApplication.OBJECTSTORAGE_BUCKETNAME)
                                .objectNames(new ArrayList<>(Arrays.asList(fileName))).build()))).build())
                .outputLocation(OutputLocation.builder()
                        .namespaceName(AIApplication.OBJECTSTORAGE_NAMESPACE)
                        .bucketName(AIApplication.OBJECTSTORAGE_BUCKETNAME)
//                            .prefix("EXAMPLE-prefix-Value1")
                        .build())
//                    .freeformTags(new HashMap<java.lang.String, java.lang.String>() {
//                        {
//                            put("EXAMPLE_KEY_yRf3m","EXAMPLE_VALUE_8Huo8VgOyTwUIGjFP4Xr");
//                        }
//                    })
//                    .definedTags(new HashMap<java.lang.String, java.util.Map<java.lang.String, java.lang.Object>>() {
//                        {
//                            put("EXAMPLE_KEY_9ftH6",new HashMap<java.lang.String, java.lang.Object>() {
//                                {
//                                    put("EXAMPLE_KEY_TYlyl","EXAMPLE--Value");
//                                }
//                            });
//                        }
//                    })
                .build();

        CreateTranscriptionJobRequest createTranscriptionJobRequest = CreateTranscriptionJobRequest.builder()
                .createTranscriptionJobDetails(createTranscriptionJobDetails)
//                    .opcRetryToken("EXAMPLE-opcRetryToken-Value")
//                    .opcRequestId("VZSHXTPLTC8OHEXY2YDI<unique_ID>")
                .build();

        CreateTranscriptionJobResponse response = client.createTranscriptionJob(createTranscriptionJobRequest);

        GetTranscriptionJobRequest getTranscriptionJobRequest = GetTranscriptionJobRequest.builder()
                .transcriptionJobId(response.getTranscriptionJob().getId())
//                .opcRequestId("4IZ7R3K1QROIUZD2TZGK<unique_ID>")
                .build();
        GetTranscriptionJobResponse getTranscriptionJobResponseresponse = null;
        TranscriptionJob.LifecycleState transcriptJobState = null;
        while (
                transcriptJobState == null ||
                        (
                                !transcriptJobState.equals(TranscriptionJob.LifecycleState.Succeeded) &&
                                        !transcriptJobState.equals(TranscriptionJob.LifecycleState.Canceled) &&
                                        !transcriptJobState.equals(TranscriptionJob.LifecycleState.Failed))
        ) {
            getTranscriptionJobResponseresponse =
                    client.getTranscriptionJob(getTranscriptionJobRequest);
            transcriptJobState = getTranscriptionJobResponseresponse.getTranscriptionJob().getLifecycleState();
            System.out.println("transcriptJobState:" + transcriptJobState);
        }
        System.out.println("getInputLocation:" +
                getTranscriptionJobResponseresponse.getTranscriptionJob().getInputLocation());
        String fullString = getTranscriptionJobResponseresponse.getTranscriptionJob().getId();
        int lastIndex = fullString.lastIndexOf(".");
        String extractedString = "";
        if (lastIndex != -1) { // Check if the dot was found
            extractedString = fullString.substring(lastIndex + 1);
        }
        return "job-" + extractedString;
    }

    @NotNull
    private static AuthenticationDetailsProvider getAuthenticationDetailsProvider() throws IOException {
//        return  new ConfigFileAuthenticationDetailsProvider(
//                System.getenv("OCICONFIG_FILE"),System.getenv("OCICONFIG_PROFILE"));
        return new ConfigFileAuthenticationDetailsProvider(
                System.getenv("OCICONFIG_FILE"), System.getenv("OCICONFIG_PROFILE"));
    }

    public void sendToObjectStorage(File fileToUpload) throws Exception {
        System.out.println("GenerateAPictureStoryUsingOnlySpeech.sendToObjectStorage fileToUpload:" + fileToUpload);
        ObjectStorageClient client;
        AuthenticationDetailsProvider provider;
        if (true) {
            provider = getAuthenticationDetailsProvider();
            client =
                    ObjectStorageClient.builder().region(Region.US_CHICAGO_1).build(provider);
//                    ObjectStorageClient.builder().region(Region.US_CHICAGO_1.US_PHOENIX_1).build(provider);
        } else {
//            aiServiceVisionClient = new AIServiceVisionClient(InstancePrincipalsAuthenticationDetailsProvider.builder().build());
        }
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .namespaceName(AIApplication.OBJECTSTORAGE_NAMESPACE)
                .bucketName(AIApplication.OBJECTSTORAGE_BUCKETNAME)
                .objectName(fileToUpload.getName())
//                .objectName(audioFilePath.getFileName().toString())
                .putObjectBody(new FileInputStream(fileToUpload)) //InputStream
                .build();
        PutObjectResponse response = client.putObject(putObjectRequest);
        System.out.println("File uploaded successfully. Object Storage Location: "
                + fileToUpload.getName());
    }

    public String getFromObjectStorage(String transcriptionJobId, String objectName) throws Exception {
        System.out.println("GenerateAPictureStoryUsingOnlySpeech.getFromObjectStorage objectName:" + objectName);
        ObjectStorageClient client;
        AuthenticationDetailsProvider provider;
        if (true) {
            provider = getAuthenticationDetailsProvider();
            client =
                    ObjectStorageClient.builder().region(Region.US_CHICAGO_1).build(provider);
//                    ObjectStorageClient.builder().region(Region.US_CHICAGO_1.US_PHOENIX_1).build(provider);
        } else {
//            aiServiceVisionClient = new AIServiceVisionClient(InstancePrincipalsAuthenticationDetailsProvider.builder().build());
        }
        GetObjectRequest putObjectRequest = GetObjectRequest.builder()
                .namespaceName(AIApplication.OBJECTSTORAGE_NAMESPACE)
                .bucketName(AIApplication.OBJECTSTORAGE_BUCKETNAME)
                .objectName(transcriptionJobId + "/" + objectName)
//                .objectName(audioFilePath.getFileName().toString())
//                .putObjectBody(new FileInputStream(fileToUpload)) //InputStream
                .build();
        GetObjectResponse response = client.getObject(putObjectRequest);
        String responseString = "";
        try (InputStream inputStream = response.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                responseString += line;
            }
        }
        return responseString;
    }


    public String imagegeneration(String imagedescription) throws Exception {
        OpenAiService service =
                new OpenAiService(System.getenv("OPENAI_KEY"), Duration.ofSeconds(60));
        CreateImageRequest openairequest = CreateImageRequest.builder()
                .prompt(imagedescription)
                .build();
        String imageLocation = service.createImage(openairequest).getData().get(0).getUrl();
        System.out.println("Image is located at:" + imageLocation);
        service.shutdownExecutor();
        return imageLocation;
    }


    public String transcribe(File file) throws Exception {
        OpenAiService service =
                new OpenAiService(System.getenv("OPENAI_KEY"), Duration.ofSeconds(60));
        String audioTranscription = transcribeFile(file, service);
        service.shutdownExecutor();
        return audioTranscription;
    }

    private String transcribeFile(File file, OpenAiService service) throws Exception {
        String endpoint = "https://api.openai.com/v1/audio/transcriptions";
        String modelName = "whisper-1";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(System.getenv("OPENAI_KEY"));
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        byte[] fileBytes = new byte[0];
        try (FileInputStream fis = new FileInputStream(file);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }
            fileBytes = bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        body.add("file", new ByteArrayResource(fileBytes) {
            @Override
            public String getFilename() {
                return file.getName();
            }
        });
        body.add("model", modelName);
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(endpoint, HttpMethod.POST, requestEntity, String.class);
        return response.getBody();
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
            try (final ByteArrayOutputStream out = new ByteArrayOutputStream(); final TargetDataLine line = getTargetDataLineForRecord();) {
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

        public void buildByteOutputStream(final ByteArrayOutputStream out, final TargetDataLine line, int frameSizeInBytes, final int bufferLengthInBytes) throws IOException {
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
