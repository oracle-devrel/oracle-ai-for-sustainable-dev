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
    
    public  String doChat(String textcontent) throws Exception {
        final GenerativeAiInferenceClient generativeAiInferenceClient =
                new GenerativeAiInferenceClient(AuthProvider.getAuthenticationDetailsProvider());
        // generativeAiInferenceClient.setEndpoint(ENDPOINT);
        generativeAiInferenceClient.setRegion(Region.US_CHICAGO_1);
        CohereLlmInferenceRequest cohereLlmInferenceRequest =
                CohereLlmInferenceRequest.builder()
                        .prompt(textcontent)
                        .maxTokens(600)
                        .temperature(0.75)
                        .frequencyPenalty(1.0)
                        .topP(0.7)
                        .isStream(false) // SDK doesn't support streaming responses, feature is under development
                        .isEcho(true)
                        .build();
        GenerateTextDetails generateTextDetails = GenerateTextDetails.builder()
                .servingMode(OnDemandServingMode.builder().modelId("cohere.command").build()) // "cohere.command-light" is also available to use
                // .servingMode(DedicatedServingMode.builder().endpointId("custom-model-endpoint").build()) // for custom model from Dedicated AI Cluster
                .compartmentId(AIApplication.COMPARTMENT_ID)
                .inferenceRequest(cohereLlmInferenceRequest)
                .build();
        GenerateTextRequest generateTextRequest = GenerateTextRequest.builder()
                .generateTextDetails(generateTextDetails)
                .build();
        GenerateTextResponse generateTextResponse = generativeAiInferenceClient.generateText(generateTextRequest);
        System.out.println(generateTextResponse.toString());
        return generateTextResponse.toString();
    }

}
