package oracleai;

import com.oracle.bmc.aivision.model.FaceDetectionFeature;
import oracleai.services.OracleVisionAI;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;


@Controller
@RequestMapping("/facerecognition")
public class FaceRecognition {


    @PostMapping("/facerecognition")
    public String facerecognition(@RequestParam("file") MultipartFile multipartFile, Model model)
            throws Exception {
        model.addAttribute("results",
                OracleVisionAI.processImage(multipartFile.getBytes(), FaceDetectionFeature.builder().shouldReturnLandmarks(true).build()));
        return "resultspage";
    }
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

