package oracleai.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.oracle.bmc.aidocument.AIServiceDocumentClient;
import com.oracle.bmc.aidocument.model.*;
import com.oracle.bmc.aidocument.requests.AnalyzeDocumentRequest;
import com.oracle.bmc.aidocument.responses.AnalyzeDocumentResponse;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class OracleDocumentAI {
    public static String processDocument(byte[] bytes) throws Exception {
        AuthenticationDetailsProvider provider = AuthProvider.getAuthenticationDetailsProvider();
        AIServiceDocumentClient aiServiceDocumentClient = AIServiceDocumentClient.builder().build(provider);
        List<DocumentFeature> features = new ArrayList<>();
        features.add(DocumentClassificationFeature.builder().build());
        features.add(DocumentKeyValueExtractionFeature.builder().build());
//        features.add(DocumentLanguageClassificationFeature.builder().build());
//        features.add(DocumentTableExtractionFeature.builder().build());
//        features.add(DocumentTextExtractionFeature.builder().build());
        InlineDocumentDetails inlineImageDetails = InlineDocumentDetails.builder()
                .data(bytes)
                .build();
        AnalyzeDocumentDetails analyzeImageDetails = AnalyzeDocumentDetails.builder()
                .document(inlineImageDetails)
                .features(features)
                .build();
        AnalyzeDocumentRequest request = AnalyzeDocumentRequest.builder()
                .analyzeDocumentDetails(analyzeImageDetails)
                .build();
        AnalyzeDocumentResponse response = aiServiceDocumentClient.analyzeDocument(request);
        ObjectMapper mapper = new ObjectMapper();
        mapper.setFilterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));
        AnalyzeDocumentResult analyzeDocumentResult = response.getAnalyzeDocumentResult();
        String json = mapper.writeValueAsString(analyzeDocumentResult);
        System.out.println("OracleDocumentAI.processDocument json:" + json);
        return json;
    }

}
