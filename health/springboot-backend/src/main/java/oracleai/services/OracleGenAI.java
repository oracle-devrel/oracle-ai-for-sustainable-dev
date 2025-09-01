package oracleai.services;

import com.oracle.bmc.ClientConfiguration;
import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.Region;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.retrier.RetryConfiguration;
import com.oracle.bmc.ClientConfiguration;
import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.Region;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.generativeaiinference.GenerativeAiInferenceClient;
import com.oracle.bmc.generativeaiinference.model.CohereLlmInferenceRequest;
import com.oracle.bmc.generativeaiinference.model.GenerateTextDetails;
import com.oracle.bmc.generativeaiinference.model.OnDemandServingMode;
import com.oracle.bmc.generativeaiinference.requests.GenerateTextRequest;
import com.oracle.bmc.generativeaiinference.responses.GenerateTextResponse;
import com.oracle.bmc.generativeaiinference.responses.GenerateTextResponse;

import oracleai.AIApplication;

import java.util.Arrays;
import java.util.List;

public class OracleGenAI {

        public static String chat(String textcontent) throws Exception {
                return new OracleGenAI().doChat(textcontent);
        }

        public String doChat(String textcontent) throws Exception {

                // ClientConfiguration clientConfiguration =
                // ClientConfiguration.builder()
                // .readTimeoutMillis(240000)
                // .retryConfiguration(RetryConfiguration.NO_RETRY_CONFIGURATION)
                // .build();

                try {
                        final GenerativeAiInferenceClient generativeAiInferenceClient = new GenerativeAiInferenceClient(
                                        AuthProvider.getAuthenticationDetailsProvider());

                        // Set endpoint from configuration if available, otherwise use region
                        if (AIApplication.OCI_GENAI_SERVICE_ENDPOINT != null
                                        && !AIApplication.OCI_GENAI_SERVICE_ENDPOINT.isEmpty()) {
                                generativeAiInferenceClient.setEndpoint(AIApplication.OCI_GENAI_SERVICE_ENDPOINT);
                        } else {
                                generativeAiInferenceClient.setRegion(Region.EU_FRANKFURT_1); // Match the region from
                                                                                              // AIApplication
                        }
                        CohereLlmInferenceRequest cohereLlmInferenceRequest = CohereLlmInferenceRequest.builder()
                                        .prompt(textcontent)
                                        .maxTokens(600)
                                        .temperature(0.75)
                                        .frequencyPenalty(1.0)
                                        .topP(0.7)
                                        .isStream(false) // SDK doesn't support streaming responses, feature is under
                                                         // development
                                        .isEcho(true)
                                        .build();

                        // Build generate text request, send, and get response
                        GenerateTextDetails generateTextDetails = GenerateTextDetails.builder()
                                        .servingMode(OnDemandServingMode.builder().modelId("cohere.command")
                                                        .build()) // Use basic Cohere Command (most widely available)
                                        // .servingMode(OnDemandServingMode.builder().modelId("cohere.command-light").build())
                                        // // Alternative: Cohere Command Light
                                        // .servingMode(OnDemandServingMode.builder().modelId("cohere.command-r-plus").build())
                                        // // Alternative: Cohere Command R Plus
                                        // .servingMode(OnDemandServingMode.builder().modelId("cohere.command-r-16k").build())
                                        // // Alternative: Cohere Command R 16k
                                        // .servingMode(OnDemandServingMode.builder().modelId("meta.llama-3-70b-instruct").build())
                                        // // Alternative: Meta Llama (requires different request format)
                                        // .servingMode(DedicatedServingMode.builder().endpointId("custom-model-endpoint").build())
                                        // // for custom model from Dedicated AI Cluster
                                        .compartmentId(AIApplication.COMPARTMENT_ID)
                                        .inferenceRequest(cohereLlmInferenceRequest)
                                        .build();

                        GenerateTextRequest generateTextRequest = GenerateTextRequest.builder()
                                        .generateTextDetails(generateTextDetails)
                                        .build();

                        GenerateTextResponse generateTextResponse = generativeAiInferenceClient
                                        .generateText(generateTextRequest);

                        System.out.println(generateTextResponse.toString());
                        return generateTextResponse.toString();
                } catch (Exception e) {
                        System.err.println("Failed to generate text using GenAI:");
                        System.err.println("  Endpoint: " + AIApplication.OCI_GENAI_SERVICE_ENDPOINT);
                        System.err.println("  Compartment: " + AIApplication.COMPARTMENT_ID);
                        System.err.println("  Error: " + e.getMessage());

                        // Return a meaningful error response instead of throwing
                        String escapedMessage = e.getMessage()
                                        .replace("\\", "\\\\")
                                        .replace("\"", "\\\"")
                                        .replace("\n", "\\n")
                                        .replace("\r", "\\r")
                                        .replace("\t", "\\t");
                        return "{\"error\": \"GenAI service not available\", \"message\": \"" + escapedMessage
                                        + "\", \"fallback_response\": \"I'm sorry, I'm currently unable to process your request. Please try again later.\"}";
                }
        }

}
