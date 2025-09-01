package oracleai.services;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.oracle.bmc.aispeech.AIServiceSpeechClient;
import com.oracle.bmc.aispeech.model.*;
import com.oracle.bmc.aispeech.requests.CreateTranscriptionJobRequest;
import com.oracle.bmc.aispeech.requests.GetTranscriptionJobRequest;
import com.oracle.bmc.aispeech.responses.CreateTranscriptionJobResponse;
import com.oracle.bmc.aispeech.responses.GetTranscriptionJobResponse;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import lombok.Getter;
import lombok.Setter;
import oracleai.AIApplication;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OracleSpeechAI {

        public static String getTranscriptFromOCISpeech(String fileName) throws IOException, InterruptedException {
                AuthenticationDetailsProvider provider = AuthProvider.getAuthenticationDetailsProvider();
                AIServiceSpeechClient client = AIServiceSpeechClient.builder().build(provider);

                // Try Chicago region instead of default us-ashburn-1
                try {
                        if (AIApplication.OCI_SPEECH_SERVICE_ENDPOINT != null
                                        && !AIApplication.OCI_SPEECH_SERVICE_ENDPOINT.isEmpty()) {
                                client.setEndpoint(AIApplication.OCI_SPEECH_SERVICE_ENDPOINT);
                                System.out.println("Using configured Speech endpoint: "
                                                + AIApplication.OCI_SPEECH_SERVICE_ENDPOINT);
                        } else {
                                // Use Chicago region endpoint
                                String chicagoEndpoint = "https://speech.aiservice.us-chicago-1.oci.oraclecloud.com";
                                client.setEndpoint(chicagoEndpoint);
                                System.out.println("Using Chicago Speech endpoint: " + chicagoEndpoint);
                        }
                } catch (Exception e) {
                        System.err.println("Error setting Speech AI endpoint: " + e.getMessage());
                        // Continue with default region
                }

                CreateTranscriptionJobDetails createTranscriptionJobDetails = CreateTranscriptionJobDetails.builder()
                                // .displayName("EXAMPLE-displayName-Value")
                                .compartmentId(AIApplication.COMPARTMENT_ID)
                                // .description("EXAMPLE-description-Value")
                                .additionalTranscriptionFormats(new ArrayList<>(Arrays.asList(
                                                CreateTranscriptionJobDetails.AdditionalTranscriptionFormats.Srt)))
                                .modelDetails(TranscriptionModelDetails.builder()
                                                .domain(TranscriptionModelDetails.Domain.Generic)
                                                .languageCode(TranscriptionModelDetails.LanguageCode.EnUs)
                                                .transcriptionSettings(TranscriptionSettings.builder()
                                                                .diarization(Diarization.builder()
                                                                                .isDiarizationEnabled(false)
                                                                                .numberOfSpeakers(7).build())
                                                                .build())
                                                .build())
                                .normalization(TranscriptionNormalization.builder()
                                                .isPunctuationEnabled(true)
                                                .filters(new ArrayList<>(Arrays.asList(ProfanityTranscriptionFilter
                                                                .builder()
                                                                .mode(ProfanityTranscriptionFilter.Mode.Mask).build())))
                                                .build())
                                .inputLocation(ObjectListInlineInputLocation.builder()
                                                .objectLocations(new ArrayList<>(Arrays.asList(ObjectLocation.builder()
                                                                .namespaceName(AIApplication.OBJECTSTORAGE_NAMESPACE)
                                                                .bucketName(AIApplication.OBJECTSTORAGE_BUCKETNAME)
                                                                .objectNames(new ArrayList<>(Arrays.asList(fileName)))
                                                                .build())))
                                                .build())
                                .outputLocation(OutputLocation.builder()
                                                .namespaceName(AIApplication.OBJECTSTORAGE_NAMESPACE)
                                                .bucketName(AIApplication.OBJECTSTORAGE_BUCKETNAME)
                                                // .prefix("EXAMPLE-prefix-Value1")
                                                .build())
                                // .freeformTags(new HashMap<java.lang.String, java.lang.String>() {
                                // {
                                // put("EXAMPLE_KEY_yRf3m","EXAMPLE_VALUE_8Huo8VgOyTwUIGjFP4Xr");
                                // }
                                // })
                                // .definedTags(new HashMap<java.lang.String, java.util.Map<java.lang.String,
                                // java.lang.Object>>() {
                                // {
                                // put("EXAMPLE_KEY_9ftH6",new HashMap<java.lang.String, java.lang.Object>() {
                                // {
                                // put("EXAMPLE_KEY_TYlyl","EXAMPLE--Value");
                                // }
                                // });
                                // }
                                // })
                                .build();

                CreateTranscriptionJobRequest createTranscriptionJobRequest = CreateTranscriptionJobRequest.builder()
                                .createTranscriptionJobDetails(createTranscriptionJobDetails)
                                .build();
                CreateTranscriptionJobResponse response = client.createTranscriptionJob(createTranscriptionJobRequest);
                GetTranscriptionJobRequest getTranscriptionJobRequest = GetTranscriptionJobRequest.builder()
                                .transcriptionJobId(response.getTranscriptionJob().getId())
                                .build();
                GetTranscriptionJobResponse getTranscriptionJobResponseresponse = null;
                TranscriptionJob.LifecycleState transcriptJobState = null;
                TranscriptionJob.LifecycleState lastState;
                while (transcriptJobState == null ||
                                (!transcriptJobState.equals(TranscriptionJob.LifecycleState.Succeeded) &&
                                                !transcriptJobState.equals(TranscriptionJob.LifecycleState.Canceling) &&
                                                !transcriptJobState.equals(TranscriptionJob.LifecycleState.Canceled) &&
                                                !transcriptJobState.equals(TranscriptionJob.LifecycleState.Failed))) {
                        Thread.currentThread().sleep(1000);
                        lastState = transcriptJobState;
                        getTranscriptionJobResponseresponse = client.getTranscriptionJob(getTranscriptionJobRequest);
                        transcriptJobState = getTranscriptionJobResponseresponse.getTranscriptionJob()
                                        .getLifecycleState();
                        if (lastState != null && lastState.equals(transcriptJobState))
                                System.out.print(".");
                        else
                                System.out.println("transcriptJobState:" + transcriptJobState);
                }

                // Check if the job failed
                if (transcriptJobState.equals(TranscriptionJob.LifecycleState.Failed)) {
                        System.err.println("Speech transcription job failed!");
                        System.err.println("Job ID: " + response.getTranscriptionJob().getId());
                        System.err.println("Input Location: "
                                        + getTranscriptionJobResponseresponse.getTranscriptionJob().getInputLocation());

                        // Get more details about the failure
                        TranscriptionJob job = getTranscriptionJobResponseresponse.getTranscriptionJob();
                        System.err.println("Job details: " + job.toString());
                        if (job.getLifecycleDetails() != null) {
                                System.err.println("Lifecycle details: " + job.getLifecycleDetails());
                        }

                        // Build more specific error message based on common issues
                        String errorDetails = "Speech transcription failed. ";
                        if (job.getLifecycleDetails() != null && !job.getLifecycleDetails().isEmpty()) {
                                errorDetails += "Details: " + job.getLifecycleDetails() + ". ";
                        }
                        errorDetails += "Common causes: 1) Audio quality too poor, 2) No clear speech detected, 3) Audio too short (<3 seconds) or too long (>60 minutes), 4) Unsupported audio encoding.";

                        throw new RuntimeException(errorDetails);
                }

                System.out.println("getInputLocation:" +
                                getTranscriptionJobResponseresponse.getTranscriptionJob().getInputLocation());
                String fullString = getTranscriptionJobResponseresponse.getTranscriptionJob().getId();
                int lastIndex = fullString.lastIndexOf(".");
                String extractedString = "";
                if (lastIndex != -1)
                        extractedString = fullString.substring(lastIndex + 1);
                return "job-" + extractedString;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        @Getter
        @Setter
        public static class TranscriptionResponse {
                private List<Transcription> transcriptions;

                @JsonIgnoreProperties(ignoreUnknown = true)
                @Getter
                @Setter
                public static class Transcription {
                        private List<Token> tokens;

                        @JsonIgnoreProperties(ignoreUnknown = true)
                        @Getter
                        @Setter
                        public static class Token {
                                private String token;
                        }
                }
        }

}
