package oracleai.services;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.oracle.bmc.aivision.AIServiceVisionClient;
import com.oracle.bmc.aivision.model.*;
import com.oracle.bmc.aivision.requests.AnalyzeImageRequest;
import com.oracle.bmc.aivision.responses.AnalyzeImageResponse;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

public class OracleVisionAI {

    public static String processImage(byte[] bytes) throws Exception {

        AuthenticationDetailsProvider provider = AuthProvider.getAuthenticationDetailsProvider();
        AIServiceVisionClient aiServiceVisionClient = AIServiceVisionClient.builder().build(provider);
        List<ImageFeature> features = new ArrayList<>();
        ImageFeature faceDetectionFeature = FaceDetectionFeature.builder()
                .maxResults(10)
                .build();
        ImageFeature classifyFeature = ImageClassificationFeature.builder()
                .maxResults(10)
                .build();
        ImageFeature detectImageFeature = ImageObjectDetectionFeature.builder()
                .maxResults(10)
                .build();
        ImageFeature textDetectImageFeature = ImageTextDetectionFeature.builder().build();
//        features.add(faceDetectionFeature);
//        features.add(classifyFeature);
//        features.add(detectImageFeature);
        features.add(textDetectImageFeature);
        InlineImageDetails inlineImageDetails = InlineImageDetails.builder()
                .data(bytes)
                .build();
        AnalyzeImageDetails analyzeImageDetails = AnalyzeImageDetails.builder()
                .image(inlineImageDetails)
                .features(features)
                .build();
        AnalyzeImageRequest request = AnalyzeImageRequest.builder()
                .analyzeImageDetails(analyzeImageDetails)
                .build();
        AnalyzeImageResponse response = aiServiceVisionClient.analyzeImage(request);
        ObjectMapper mapper = new ObjectMapper();
        mapper.setFilterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));

        String json = mapper.writeValueAsString(response.getAnalyzeImageResult());
//        System.out.println("AnalyzeImage Result");
//        System.out.println(json);
        return json;
    }


    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Setter
    public static class ImageData {
        private ImageText imageText;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Setter
    public static class ImageText {
        private List<Word> words;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Setter
    public static class Word {
        private String text;
    }
}
