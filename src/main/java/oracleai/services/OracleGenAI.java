package oracleai.services;

import com.oracle.bmc.Region;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.generativeai.GenerativeAiClient;
import com.oracle.bmc.generativeai.model.GenerateTextDetails;
import com.oracle.bmc.generativeai.model.GenerateTextResult;
import com.oracle.bmc.generativeai.model.OnDemandServingMode;
import com.oracle.bmc.generativeai.requests.GenerateTextRequest;
import com.oracle.bmc.generativeai.responses.GenerateTextResponse;
import oracleai.AIApplication;

import java.util.Arrays;
import java.util.List;

public class OracleGenAI {


    public static String chat(String textcontent) throws Exception {
        AuthenticationDetailsProvider provider = AuthProvider.getAuthenticationDetailsProvider();
        //GenAI is only available in US_CHICAGO_1 for current beta, thus the override
        GenerativeAiClient generativeAiClient = GenerativeAiClient.builder().region(Region.US_CHICAGO_1).build(provider);
        List<String> prompts = Arrays.asList(textcontent);
        GenerateTextDetails generateTextDetails = GenerateTextDetails.builder()
                .servingMode(OnDemandServingMode.builder().modelId("cohere.command").build()) // "cohere.command-light" is also available to use
                // .servingMode(DedicatedServingMode.builder().endpointId("custom-model-endpoint").build()) // for custom model from Dedicated AI Cluster
                .compartmentId(AIApplication.COMPARTMENT_ID)
                .prompts(prompts)
                .maxTokens(300)
                .temperature(0.75)
                .frequencyPenalty(1.0)
                .topP(0.7)
                .isStream(false)
                .isEcho(false)
                .build();
        GenerateTextRequest generateTextRequest = GenerateTextRequest.builder()
                .generateTextDetails(generateTextDetails)
                .build();
        GenerateTextResponse generateTextResponse = generativeAiClient.generateText(generateTextRequest);
        GenerateTextResult result = generateTextResponse.getGenerateTextResult();
        if(result !=null && result.getGeneratedTexts().size() > 0 ) {
            String all_results ="";
            for (List<com.oracle.bmc.generativeai.model.GeneratedText> list : result.getGeneratedTexts()) {
                for (com.oracle.bmc.generativeai.model.GeneratedText text:list){
                    all_results = all_results+text.getText();
                }
            }
            return all_results;
        }
        return "We could not find a result for your text. Try a different image.";
    }

}
