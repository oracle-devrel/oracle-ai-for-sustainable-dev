package oracleai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.bmc.aivision.model.ImageTextDetectionFeature;
import oracleai.services.ORDSCalls;
import oracleai.services.OracleGenAI;
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
        String objectDetectionResults = OracleVisionAI.processImage(file.getBytes(), ImageTextDetectionFeature.builder().build());
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
}