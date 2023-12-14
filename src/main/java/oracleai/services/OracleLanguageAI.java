package oracleai.services;

import com.oracle.bmc.ailanguage.AIServiceLanguageClient;
import com.oracle.bmc.ailanguage.model.DetectLanguageSentimentsDetails;
import com.oracle.bmc.ailanguage.model.DetectLanguageSentimentsResult;
import com.oracle.bmc.ailanguage.model.SentimentAspect;
import com.oracle.bmc.ailanguage.requests.DetectLanguageSentimentsRequest;
import com.oracle.bmc.ailanguage.responses.DetectLanguageSentimentsResponse;
import com.oracle.bmc.model.BmcException;

import java.io.IOException;

public class OracleLanguageAI {


    public static String sentimentAnalysis(String textcontent) throws IOException {
        System.out.println("OracleLanguageAI.sentiments analyze text for sentiment:" + textcontent);
        AIServiceLanguageClient languageClient =
                AIServiceLanguageClient.builder().build(AuthProvider.getAuthenticationDetailsProvider());
        DetectLanguageSentimentsDetails details =
                DetectLanguageSentimentsDetails.builder()
                        .text(textcontent)
                        .build();
        DetectLanguageSentimentsRequest detectLanguageSentimentsRequest =
                DetectLanguageSentimentsRequest.builder()
                        .detectLanguageSentimentsDetails(details)
                        .build();
        DetectLanguageSentimentsResponse response = null;
        try {
            response = languageClient.detectLanguageSentiments(detectLanguageSentimentsRequest);
        } catch (BmcException e) {
            System.err.println("Failed to detect language and sentiments: " + e.getMessage());
        }
        DetectLanguageSentimentsResult detectLanguageSentimentsResult = response.getDetectLanguageSentimentsResult();
        String sentimentReturn = "";
        for (SentimentAspect aspect : detectLanguageSentimentsResult.getAspects()) {
            sentimentReturn += "  sentiment:" + aspect.getSentiment();
            sentimentReturn += " text:" + aspect.getText();
            sentimentReturn += ", ";
        }
        return sentimentReturn;
    }
}
