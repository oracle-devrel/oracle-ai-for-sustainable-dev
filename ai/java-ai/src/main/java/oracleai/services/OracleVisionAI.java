package oracleai.services;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.oracle.bmc.aivision.AIServiceVisionClient;
import com.oracle.bmc.aivision.model.*;
import com.oracle.bmc.aivision.requests.AnalyzeImageRequest;
import com.oracle.bmc.aivision.responses.AnalyzeImageResponse;
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

public class OracleVisionAI {

    /**
     * As written only supports on feature type per call. Examples include...
     ImageFeature faceDetectionFeature = FaceDetectionFeature.builder()
     .maxResults(10)
     .build();
     ImageFeature classifyFeature = ImageClassificationFeature.builder()
     .maxResults(10)
     .build();
     ImageFeature detectImageFeature = ImageObjectDetectionFeature.builder()
     .maxResults(10)
     .build();
     *
     */
    public static String processImage(byte[] bytes, ImageFeature feature) throws Exception {
        BasicAuthenticationDetailsProvider provider = AuthProvider.getAuthenticationDetailsProvider();
        AIServiceVisionClient aiServiceVisionClient = AIServiceVisionClient.builder().build(provider);
        List<ImageFeature> features = new ArrayList<>();
        features.add(feature);
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
        return json;
    }


    //For Text Detection....
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


    //For Image Detection...

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Setter
    public static class ImageAnalysisResult {
        private List<ImageObject> imageObjects;
        private List<OntologyClass> ontologyClasses;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Setter
    public static class ImageObject {
        private String name;
        private Double confidence;
        private BoundingPolygon boundingPolygon;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Setter
    public static class BoundingPolygon {
        private List<Vertex> normalizedVertices;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Setter
    public static class Vertex {
        private Double x;
        private Double y;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Setter
    public static class OntologyClass {
        private String name;
        private List<String> parentNames;
        private List<String> synonymNames;
    }

}
