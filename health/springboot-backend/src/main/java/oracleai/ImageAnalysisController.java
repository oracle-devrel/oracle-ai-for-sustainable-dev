package oracleai;

import com.oracle.bmc.aivision.model.*;
import oracleai.services.OracleVisionAI;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/imageanalysis")
public class ImageAnalysisController {


    @CrossOrigin
    @PostMapping("/analyzeimage")
    public String analyzeimage(@RequestParam("model") String model, @RequestParam("file") MultipartFile file)
            throws Exception {
        System.out.println("ImageAnalysisController analyzing image file:" + file);
        return processImage(model, file.getBytes());
    }

    String processImage(String model, byte[] bytes) throws Exception {
        String modelId ="";
        if (model.equals("breastcancer")) modelId = AIApplication.VISIONAI_XRAY_BREASTCANCER_MODEL_OCID;
        else if (model.equals("covid")) modelId = AIApplication.VISIONAI_XRAY_PNEUMONIA_MODEL_OCID;
        else if (model.equals("lungcancer")) modelId = AIApplication.VISIONAI_XRAY_LUNGCANCER_MODEL_OCID;
        ImageFeature classifyFeature = ImageClassificationFeature.builder()
                .modelId(modelId)
                .maxResults(10)
                .build();
        JSONObject jsonObject = new JSONObject(OracleVisionAI.processImage(bytes, classifyFeature));
        JSONArray labelsArray = jsonObject.getJSONArray("labels");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < labelsArray.length(); i++) {
            JSONObject label = labelsArray.getJSONObject(i);
            String name = label.getString("name");
            double confidence = label.getDouble("confidence");
            result.append(name).append(" chance ").append(confidence * 10).append("%");
            if (i < labelsArray.length() - 1) {
                result.append(", ");
            }
        }

        System.out.println(result);
        return result.toString();
    }
}



