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
        AuthenticationDetailsProvider provider = AuthProvider.getAuthenticationDetailsProvider();
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
//        System.out.println("AnalyzeImage Result");
//        System.out.println(json);
        return json;
    }


    //For Text Detection....
/**
    {
        "imageObjects": [
        {
            "name": "Wine Glass",
                "confidence": 0.9297104,
                "boundingPolygon": {
            "normalizedVertices": [
            {
                "x": 0.6124005305039788,
                    "y": 0.02100673801030519
            },
            {
                "x": 0.7443633952254642,
                    "y": 0.02100673801030519
            },
            {
                "x": 0.7443633952254642,
                    "y": 0.19421323820848196
            },
            {
                "x": 0.6124005305039788,
                    "y": 0.19421323820848196
            }
        ]
        }
        },
        {
            "name": "Spoon",
                "confidence": 0.88298225,
                "boundingPolygon": {
            "normalizedVertices": [
            {
                "x": 0.6114058355437666,
                    "y": 0.40745144669044786
            },
            {
                "x": 0.919761273209549,
                    "y": 0.40745144669044786
            },
            {
                "x": 0.919761273209549,
                    "y": 0.622671422909235
            },
            {
                "x": 0.6114058355437666,
                    "y": 0.622671422909235
            }
        ]
        }
        }
  ],
        "labels": null,
            "ontologyClasses": [
        {
            "name": "Wine Glass",
                "parentNames": [
            "Tableware",
                    "Glass"
      ],
            "synonymNames": []
        },
        {
            "name": "Spoon",
                "parentNames": [
            "Tableware",
                    "Cutlery",
                    "Kitchen utensil"
      ],
            "synonymNames": []
        },
        {
            "name": "Glass",
                "parentNames": [],
            "synonymNames": []
        },
        {
            "name": "Kitchen utensil",
                "parentNames": [
            "Kitchenware"
      ],
            "synonymNames": []
        },
        {
            "name": "Cutlery",
                "parentNames": [],
            "synonymNames": []
        },
        {
            "name": "Kitchenware",
                "parentNames": [],
            "synonymNames": []
        }
  ],
        "imageText": null,
            "objectProposals": null,
            "detectedFaces": null,
            "imageClassificationModelVersion": null,
            "objectDetectionModelVersion": "1.3.557",
            "textDetectionModelVersion": null,
            "objectProposalModelVersion": null,
            "faceDetectionModelVersion": null,
            "errors": []
    }
 */

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
    public class BoundingPolygon {
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
