package oracleai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.bmc.aivision.model.ImageTextDetectionFeature;
import oracleai.services.ORDSCalls;
import oracleai.services.OracleGenAI;
import oracleai.services.OracleVisionAI;
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
        String concatenatedText;
        if (opts.equals("inline")) {
            String objectDetectionResults = OracleVisionAI.processImage(
                    multipartFile.getBytes(), ImageTextDetectionFeature.builder().build());
            OracleVisionAI.ImageData imageData =
                    new ObjectMapper().readValue(objectDetectionResults, OracleVisionAI.ImageData.class);
            concatenatedText = concatenateText(imageData);
        }  else concatenatedText = ORDSCalls.analyzeImageInObjectStore(
                AIApplication.ORDS_ENDPOINT_URL + "call_analyze_image_api_objectstore",
                AIApplication.OCI_VISION_SERVICE_ENDPOINT,
                AIApplication.COMPARTMENT_ID,
                AIApplication.OBJECTSTORAGE_BUCKETNAME,
                AIApplication.OBJECTSTORAGE_NAMESPACE,
                multipartFile.getOriginalFilename(), //"objectdetectiontestimage.jpg"
                "TEXT_DETECTION",
                "MedicalReportSummary");
        System.out.println(concatenatedText);
        log.info("fullText = " + concatenatedText);
        String explanationOfResults =
                OracleGenAI.chat("explain these test results in simple terms, in less than 100 words, " +
                        "and tell me what should I do to get better results: \"" + concatenatedText + "\"");
        System.out.println("ExplainAndAdviseOnHealthTestResults.analyzedoc explanationOfResults:" + explanationOfResults);
        model.addAttribute("results", "SUMMARY WITH ADVICE: " + explanationOfResults);
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