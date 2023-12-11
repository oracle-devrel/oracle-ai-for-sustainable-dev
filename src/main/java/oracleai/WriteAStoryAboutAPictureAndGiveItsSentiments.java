package oracleai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.bmc.aivision.model.*;
import oracleai.services.ORDSCalls;
import oracleai.services.OracleGenAI;
import oracleai.services.OracleLanguageAI;
import oracleai.services.OracleVisionAI;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.ui.Model;

@RestController
@RequestMapping("/tellastory")
public class WriteAStoryAboutAPictureAndGiveItsSentiments {

    @PostMapping("/tellastory")
    public String tellastory(@RequestParam("file") MultipartFile multipartFile ,
                             @RequestParam("genopts") String genopts, Model model)
            throws Exception {
        System.out.println("WriteAStoryAboutAPictureAndGiveItsSentiments.tellastory  file = " +
                multipartFile.getOriginalFilename());
        String objectDetectionResults;
        if(genopts.equals("inline")) objectDetectionResults =
                OracleVisionAI.processImage(multipartFile.getBytes(),
                ImageObjectDetectionFeature.builder().maxResults(10).build());
        else objectDetectionResults = ORDSCalls.analyzeImageInObjectStore(
                AIApplication.ORDS_ENDPOINT_ANALYZE_IMAGE_OBJECTSTORE,
                AIApplication.OCI_VISION_SERVICE_ENDPOINT,
                AIApplication.COMPARTMENT_ID,
                AIApplication.OBJECTSTORAGE_BUCKETNAME,
                AIApplication.OBJECTSTORAGE_NAMESPACE,
                multipartFile.getOriginalFilename(), //"objectdetectiontestimage.jpg"
                "OBJECT_DETECTION",
                "TellAStory");
        ObjectMapper mapper = new ObjectMapper();
        OracleVisionAI.ImageAnalysisResult imageAnalysis =
                mapper.readValue(objectDetectionResults, OracleVisionAI.ImageAnalysisResult.class);
        String fullText = "";
        for (OracleVisionAI.ImageObject image : imageAnalysis.getImageObjects())  fullText += image.getName() + ", ";
        System.out.println("WriteAStoryAboutAPictureAndGiveItsSentiments.tellastory images = " + fullText);
        String generatedstory =
                OracleGenAI.chat("using strong negative and positive sentiments, " +
                        "write a story that is " + genopts + " and includes  "  + fullText );
        model.addAttribute("results", "story:" + generatedstory +
        "<br><br>sentiment analysis:" + OracleLanguageAI.sentimentAnalysis(generatedstory) );
        return "resultspage";
    }

}



