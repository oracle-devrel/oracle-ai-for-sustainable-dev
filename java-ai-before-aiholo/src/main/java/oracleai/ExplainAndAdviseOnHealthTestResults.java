package oracleai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.bmc.aivision.model.ImageTextDetectionFeature;
import com.oracle.bmc.generativeaiinference.model.OnDemandServingMode;
import oracleai.services.ORDSCalls;
import oracleai.services.OracleGenAI;
import oracleai.services.OracleObjectStore;
import oracleai.services.OracleVisionAI;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.ui.Model;

@Controller
@RequestMapping("/health")
public class ExplainAndAdviseOnHealthTestResults {


    @PostMapping("/analyzedoc")
    public String analyzedoc(@RequestParam("file") MultipartFile multipartFile,
                             @RequestParam("opts") String opts, Model model)
            throws Exception {
        System.out.println("analyzedocmultipartFile = " + multipartFile + ", opts = " + opts);
        String concatenatedText;
        if (opts.equals("inline")) {
            String objectDetectionResults = OracleVisionAI.processImage(
                    multipartFile.getBytes(), ImageTextDetectionFeature.builder().build());
            OracleVisionAI.ImageData imageData =
                    new ObjectMapper().readValue(objectDetectionResults, OracleVisionAI.ImageData.class);
            concatenatedText = concatenateText(imageData);
        }  else {
            OracleObjectStore.sendToObjectStorage(multipartFile.getOriginalFilename(), multipartFile.getInputStream());
            concatenatedText = ORDSCalls.analyzeImageInObjectStore(
                    AIApplication.ORDS_ENDPOINT_URL + "VISIONAI_TEXTDETECTION/",
                    AIApplication.OCI_VISION_SERVICE_ENDPOINT,
                    AIApplication.COMPARTMENT_ID,
                    AIApplication.OBJECTSTORAGE_BUCKETNAME,
                    AIApplication.OBJECTSTORAGE_NAMESPACE,
                    multipartFile.getOriginalFilename(), //"objectdetectiontestimage.jpg"
                    "TEXT_DETECTION",
                    "MedicalReportSummary");
        }
        System.out.println(concatenatedText);
        System.out.println("analyzedoc fullText = " + concatenatedText);
        OnDemandServingMode chatServingMode = OnDemandServingMode.builder()
                .modelId("cohere.command-r-16k")
                .build();
        String explanationOfResults =
                OracleGenAI.builder().compartment(AIApplication.COMPARTMENT_ID)
                        .servingMode(chatServingMode)
                        .build().chat("explain these test results in simple terms, in less than 100 words, " +
                        "and tell me what should I do to get better results: \"" + concatenatedText + "\"");
        System.out.println("ExplainAndAdviseOnHealthTestResults.analyzedoc explanationOfResults:" + explanationOfResults);
        model.addAttribute("results", "SUMMARY WITH ADVICE: " + explanationOfResults +
                " ...This is of course not a substitute for actual medical advice from a professional.");
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
