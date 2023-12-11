package oracleai;

import com.fasterxml.jackson.databind.ObjectMapper;
import oracleai.services.ORDSCalls;
import oracleai.services.OracleGenAI;
import oracleai.services.OracleObjectStore;
import oracleai.services.OracleVisionAI;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.ui.Model;

@Controller
@RequestMapping("/health")
public class ExplainAndAdviseOnHealthTestResults {

    private static Logger log = LoggerFactory.getLogger(ExplainAndAdviseOnHealthTestResults.class);

    @PostMapping("/analyzedoc")
    public String analyzedoc(@RequestParam("file") MultipartFile multipartFile,
                             @RequestParam("opts") String opts, Model model)
            throws Exception {
        log.info("analyzing image file:" + multipartFile);
        if (opts.equals("inline")) return analyzedocInline(multipartFile, model);
        else return analyzeDocObjectStorage(multipartFile, model);
    }

    @NotNull
    private static String analyzeDocObjectStorage(MultipartFile multipartFile, Model model) throws Exception {
     //   OracleObjectStore.sendToObjectStorage(multipartFile.getOriginalFilename(), multipartFile.getInputStream());
        String objectDetectionResults = ORDSCalls.analyzeImageInObjectStore(
                AIApplication.ORDS_ENDPOINT_ANALYZE_IMAGE_OBJECTSTORE,
                AIApplication.OCI_VISION_SERVICE_ENDPOINT,
                AIApplication.COMPARTMENT_ID,
                AIApplication.OBJECTSTORAGE_BUCKETNAME,
                AIApplication.OBJECTSTORAGE_NAMESPACE,
                multipartFile.getOriginalFilename(), //"objectdetectiontestimage.jpg"
                "TEXT_DETECTION",
                "MedicalReportSummary");
        ObjectMapper mapper = new ObjectMapper();
        OracleVisionAI.ImageData imageData = mapper.readValue(objectDetectionResults, OracleVisionAI.ImageData.class);
        String concatenatedText = concatenateText(imageData);
        System.out.println(concatenatedText);
        log.info("fullText = " + concatenatedText);
        String explanationOfResults =
                OracleGenAI.chat("explain these test results in simple terms, in less than 100 words, " +
                        "and tell me what should I do to get better results: \"" + concatenatedText + "\"");
        System.out.println("ExplainAndAdviseOnHealthTestResults.analyzedoc explanationOfResults:" + explanationOfResults);
        model.addAttribute("results", explanationOfResults);
        return "resultspage";
    }

    public String analyzedocInline(MultipartFile file, Model model)
            throws Exception {
        log.info("analyzing image file:" + file);
        String objectDetectionResults = OracleVisionAI.processImage(file.getBytes());
        ObjectMapper mapper = new ObjectMapper();
        OracleVisionAI.ImageData imageData = mapper.readValue(objectDetectionResults, OracleVisionAI.ImageData.class);
        String concatenatedText = concatenateText(imageData);
        System.out.println(concatenatedText);
        log.info("fullText = " + concatenatedText);
        String explanationOfResults =
                OracleGenAI.chat("explain these test results in simple terms, in less than 100 words, " +
                        "and tell me what should I do to get better results: \"" + concatenatedText + "\"");
        System.out.println("ExplainAndAdviseOnHealthTestResults.analyzedoc explanationOfResults:" + explanationOfResults);
        model.addAttribute("results", explanationOfResults);
        return "resultspage";
    }

    private static String concatenateText(OracleVisionAI.ImageData imageData) {
        if (imageData.getImageText() == null || imageData.getImageText().getWords() == null) return "";
        StringBuilder sb = new StringBuilder();
        for (OracleVisionAI.Word word : imageData.getImageText().getWords()) {
            sb.append(word.getText()).append(" ");
        }
        return sb.toString().trim();
    }


/**



 @Data class ImageObject {
 private String name;
 private double confidence;
 private BoundingPolygon boundingPolygon;
 }

 @Data class BoundingPolygon {
 private List<Point> normalizedVertices;
 }

 @Data class Point {
 private double x;
 private double y;

 public Point(double x, double y) {
 this.x = x;
 this.y = y;
 }
 }

 @Data class Label {
 private String name;
 private double confidence;
 }

 @Data class OntologyClass {
 private String name;
 private List<String> parentNames;
 private List<String> synonymNames;
 }

 @Data class ImageText {
 private List<Word> words;
 private List<Line> lines;
 }

 @Data class Word {
 private String text;
 private double confidence;
 private BoundingPolygon boundingPolygon;
 }

 @Data class Line {
 private String text;
 private double confidence;
 private BoundingPolygon boundingPolygon;
 private List<Integer> wordIndexes;
 }

 @Data class ImageAnalysis {
 private List<ImageObject> imageObjects;
 private List<Label> labels;
 private List<OntologyClass> ontologyClasses;
 private ImageText imageText;
 private String imageClassificationModelVersion;
 private String objectDetectionModelVersion;
 private String textDetectionModelVersion;
 private List<String> errors;
 }


 private ImageAnalysis parseJsonToImageAnalysis(String jsonString) {
 JSONObject json = new JSONObject(jsonString);
 JSONArray imageObjectsArray = json.getJSONArray("imageObjects");
 List<ImageObject> imageObjects = new ArrayList<>();
 for (int i = 0; i < imageObjectsArray.length(); i++) {
 JSONObject imageObjectJson = imageObjectsArray.getJSONObject(i);
 ImageObject imageObject = new ImageObject();
 imageObject.setName(imageObjectJson.getString("name"));
 imageObject.setConfidence(imageObjectJson.getDouble("confidence"));

 JSONObject boundingPolygonJson = imageObjectJson.getJSONObject("boundingPolygon");
 JSONArray normalizedVerticesArray = boundingPolygonJson.getJSONArray("normalizedVertices");
 List<Point> normalizedVertices = new ArrayList<>();
 for (int j = 0; j < normalizedVerticesArray.length(); j++) {
 JSONObject vertexJson = normalizedVerticesArray.getJSONObject(j);
 Point vertex = new Point(vertexJson.getDouble("x"), vertexJson.getDouble("y"));
 normalizedVertices.add(vertex);
 }
 BoundingPolygon boundingPolygon = new BoundingPolygon();
 boundingPolygon.setNormalizedVertices(normalizedVertices);
 imageObject.setBoundingPolygon(boundingPolygon);

 imageObjects.add(imageObject);
 }
 JSONArray labelsArray = json.getJSONArray("labels");
 List<Label> labels = new ArrayList<>();
 for (int i = 0; i < labelsArray.length(); i++) {
 JSONObject labelJson = labelsArray.getJSONObject(i);
 Label label = new Label();
 label.setName(labelJson.getString("name"));
 label.setConfidence(labelJson.getDouble("confidence"));
 labels.add(label);
 }
 JSONArray ontologyClassesArray = json.getJSONArray("ontologyClasses");
 List<OntologyClass> ontologyClasses = new ArrayList<>();
 for (int i = 0; i < ontologyClassesArray.length(); i++) {
 JSONObject ontologyClassJson = ontologyClassesArray.getJSONObject(i);
 OntologyClass ontologyClass = new OntologyClass();
 ontologyClass.setName(ontologyClassJson.getString("name"));
 JSONArray parentNamesArray = ontologyClassJson.getJSONArray("parentNames");
 List<String> parentNames = new ArrayList<>();
 for (int j = 0; j < parentNamesArray.length(); j++) {
 parentNames.add(parentNamesArray.getString(j));
 }
 ontologyClass.setParentNames(parentNames);
 ontologyClasses.add(ontologyClass);
 }
 JSONObject imageTextJson = json.getJSONObject("imageText");
 JSONArray wordsArray = imageTextJson.getJSONArray("words");
 List<Word> words = new ArrayList<>();
 for (int i = 0; i < wordsArray.length(); i++) {
 JSONObject wordJson = wordsArray.getJSONObject(i);
 Word word = new Word();
 word.setText(wordJson.getString("text"));
 word.setConfidence(wordJson.getDouble("confidence"));
 JSONObject boundingPolygonJson = wordJson.getJSONObject("boundingPolygon");
 JSONArray normalizedVerticesArray = boundingPolygonJson.getJSONArray("normalizedVertices");
 List<Point> normalizedVertices = new ArrayList<>();
 for (int j = 0; j < normalizedVerticesArray.length(); j++) {
 JSONObject vertexJson = normalizedVerticesArray.getJSONObject(j);
 Point vertex = new Point(vertexJson.getDouble("x"), vertexJson.getDouble("y"));
 normalizedVertices.add(vertex);
 }
 BoundingPolygon boundingPolygon = new BoundingPolygon();
 boundingPolygon.setNormalizedVertices(normalizedVertices);
 word.setBoundingPolygon(boundingPolygon);
 words.add(word);
 }
 JSONArray linesArray = imageTextJson.getJSONArray("lines");
 List<Line> lines = new ArrayList<>();
 for (int i = 0; i < linesArray.length(); i++) {
 JSONObject lineJson = linesArray.getJSONObject(i);
 Line line = new Line();
 line.setText(lineJson.getString("text"));
 line.setConfidence(lineJson.getDouble("confidence"));
 JSONObject boundingPolygonJson = lineJson.getJSONObject("boundingPolygon");
 JSONArray normalizedVerticesArray = boundingPolygonJson.getJSONArray("normalizedVertices");
 List<Point> normalizedVertices = new ArrayList<>();
 for (int j = 0; j < normalizedVerticesArray.length(); j++) {
 JSONObject vertexJson = normalizedVerticesArray.getJSONObject(j);
 Point vertex = new Point(vertexJson.getDouble("x"), vertexJson.getDouble("y"));
 normalizedVertices.add(vertex);
 }
 BoundingPolygon boundingPolygon = new BoundingPolygon();
 boundingPolygon.setNormalizedVertices(normalizedVertices);
 line.setBoundingPolygon(boundingPolygon);
 JSONArray wordIndexesArray = lineJson.getJSONArray("wordIndexes");
 List<Integer> wordIndexes = new ArrayList<>();
 for (int j = 0; j < wordIndexesArray.length(); j++) {
 wordIndexes.add(wordIndexesArray.getInt(j));
 }
 line.setWordIndexes(wordIndexes);
 lines.add(line);
 }

 String imageClassificationModelVersion = json.getString("imageClassificationModelVersion");
 String objectDetectionModelVersion = json.getString("objectDetectionModelVersion");
 String textDetectionModelVersion = json.getString("textDetectionModelVersion");
 List<String> errors = new ArrayList<>();
 JSONArray errorsArray = json.getJSONArray("errors");
 for (int i = 0; i < errorsArray.length(); i++) {
 errors.add(errorsArray.getString(i));
 }

 ImageText imageText = new ImageText();
 imageText.setWords(words);
 imageText.setLines(lines);

 ImageAnalysis imageAnalysis = new ImageAnalysis();
 imageAnalysis.setImageObjects(imageObjects);
 imageAnalysis.setLabels(labels);
 imageAnalysis.setOntologyClasses(ontologyClasses);
 imageAnalysis.setImageText(imageText);
 imageAnalysis.setImageClassificationModelVersion(imageClassificationModelVersion);
 imageAnalysis.setObjectDetectionModelVersion(objectDetectionModelVersion);
 imageAnalysis.setTextDetectionModelVersion(textDetectionModelVersion);
 imageAnalysis.setErrors(errors);

 return imageAnalysis;
 }
 */
}


/**
 * {
 * "ontologyClasses": [],
 * "detectedFaces": [
 * {
 * "confidence": 0.9453162,
 * "boundingPolygon": {
 * "normalizedVertices": [
 * {
 * "x": 0.43885306576845223,
 * "y": 0.33600531005859374
 * },
 * {
 * "x": 0.5433995575670001,
 * "y": 0.33600531005859374
 * },
 * {
 * "x": 0.5433995575670001,
 * "y": 0.404624267578125
 * },
 * {
 * "x": 0.43885306576845223,
 * "y": 0.404624267578125
 * }
 * ]
 * },
 * "qualityScore": 0.887661,
 * "landmarks": [
 * {
 * "type": "LEFT_EYE",
 * "x": 0.46573874,
 * "y": 0.36125
 * },
 * {
 * "type": "RIGHT_EYE",
 * "x": 0.5149893,
 * "y": 0.36175
 * },
 * {
 * "type": "NOSE_TIP",
 * "x": 0.4898287,
 * "y": 0.37575
 * },
 * {
 * "type": "LEFT_EDGE_OF_MOUTH",
 * "x": 0.46734476,
 * "y": 0.3845
 * },
 * {
 * "type": "RIGHT_EDGE_OF_MOUTH",
 * "x": 0.51338327,
 * "y": 0.38475
 * }
 * ]
 * }
 * ],
 * "faceDetectionModelVersion": "1.0.29",
 * "errors": []
 * }
 */

