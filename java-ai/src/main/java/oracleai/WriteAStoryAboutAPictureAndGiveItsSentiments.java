package oracleai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.bmc.aivision.model.*;
import oracleai.services.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.ui.Model;

@Controller
@RequestMapping("/tellastory")
public class WriteAStoryAboutAPictureAndGiveItsSentiments {

    @PostMapping("/tellastory")
    public String tellastory(@RequestParam("file") MultipartFile multipartFile, @RequestParam("opts") String opts,
                             @RequestParam("genopts") String genopts, Model model)
            throws Exception {
        System.out.println("WriteAStoryAboutAPictureAndGiveItsSentiments.tellastory  file = " +
                multipartFile.getOriginalFilename());
        String fullText = "";
        if(opts.equals("inline")) {
            String objectDetectionResults =
                    OracleVisionAI.processImage(multipartFile.getBytes(),
                            ImageObjectDetectionFeature.builder().maxResults(10).build());
            OracleVisionAI.ImageAnalysisResult imageAnalysis =
                    new ObjectMapper().readValue(objectDetectionResults, OracleVisionAI.ImageAnalysisResult.class);
            for (OracleVisionAI.ImageObject image : imageAnalysis.getImageObjects())  fullText += image.getName() + ", ";
            System.out.println("WriteAStoryAboutAPictureAndGiveItsSentiments.tellastory images = " + fullText);
        }
        else {
            OracleObjectStore.sendToObjectStorage(multipartFile.getOriginalFilename(), multipartFile.getInputStream());
            fullText = ORDSCalls.analyzeImageInObjectStore(
                    AIApplication.ORDS_ENDPOINT_URL + "VISIONAI_OBJECTDETECTION/",
                    AIApplication.OCI_VISION_SERVICE_ENDPOINT,
                    AIApplication.COMPARTMENT_ID,
                    AIApplication.OBJECTSTORAGE_BUCKETNAME,
                    AIApplication.OBJECTSTORAGE_NAMESPACE,
                    multipartFile.getOriginalFilename(), //"objectdetectiontestimage.jpg"
                    "OBJECT_DETECTION",
                    "TellAStory");
        }
        String generatedstory = OracleGenAI.builder().build().chat("using strong negative and positive sentiments, " +
                        "write a story that is " + genopts + " and includes  "  + fullText );
        model.addAttribute("results", "STORY: " + generatedstory +
                "          --->SENTIMENT ANALYSIS: " + OracleLanguageAI.sentimentAnalysis(generatedstory) );
        return "resultspage";
    }

}



