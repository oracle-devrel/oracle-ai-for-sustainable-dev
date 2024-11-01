package oracleai.digitaldouble;


import oracleai.services.ORDSCalls;
import oracleai.services.OracleObjectStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

import org.springframework.http.MediaType;

@Controller
@RequestMapping("/digitaldoubles")
public class DigitalDoublesController {

    private final ImageProcessor imageProcessor;

    @Autowired
    public DigitalDoublesController(ImageProcessor imageProcessor) {
        this.imageProcessor = imageProcessor;
    }
    private static final String DIRECTORY = "/tmp/images/";

    @GetMapping("/uploadordownload")
    public String digitaldouble(@RequestParam("action") String action, Model model) {
        return action.equals("uploading") ? "digitaldoubleupload" : "digitaldoubledownload";
    }


    @PostMapping("/uploadimageandvideo")
    public String uploadimageandvideo(
            @RequestParam("image") MultipartFile image,
            @RequestParam("video") MultipartFile video,
            @RequestParam("animstyle") String animstyle,
            @RequestParam("animprompt") String animprompt,
            @RequestParam("firstName") String firstName,
            @RequestParam("lastName") String lastName,
            @RequestParam("email") String email,
            @RequestParam("company") String company,
            @RequestParam("jobrole") String jobRole,
            @RequestParam("tshirtsize") String tshirtSize,
            @RequestParam("comments") String comments,
            Model model) throws IOException {

        String commentsWithAnimStyleAndPrompt = animstyle + " " + animprompt + " " + comments;
        System.out.println("image = " + image + ", video = " + video + ", animstyle = " + animstyle +
                ", firstName = " + firstName + ", lastName = " + lastName +
                ", email = " + email + ", company = " + company +
                ", jobRole = " + jobRole + ", tshirtSize = " + tshirtSize +
                ", comments = " + comments + ", model = " + model +
                "\ncomments with animstyle and prompt = " + commentsWithAnimStyleAndPrompt);
        ORDSCalls.insertDigitalDoubleData(
                image, null, firstName, lastName, email, company, jobRole, tshirtSize, commentsWithAnimStyleAndPrompt);

        String fullVideoName ="";
        if (!video.isEmpty()) {
            fullVideoName = email + "_" + animstyle + "_" + video.getOriginalFilename();
            OracleObjectStore.sendToObjectStorage(fullVideoName, video.getInputStream());
        }
            imageProcessor.handleImageUpload(email, image, fullVideoName);

            return "digitaldoubledownload";

    }

    @GetMapping("/images/{filename:.+}")
    public ResponseEntity<byte[]> getImage(@PathVariable String filename) throws IOException {
        try {
            File file = new File(DIRECTORY, filename);
            byte[] fileContent = org.apache.commons.io.FileUtils.readFileToByteArray(file);
            return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(fileContent);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }


    @PostMapping("/downloaddigitaldouble")
    public String downloaddigitaldouble(@RequestParam("email") String email, Model model) throws Exception {
        model.addAttribute("fbxlink", ORDSCalls.getDigitalDoubleData(email));
        model.addAttribute("fbxtext", "FBX 3D Model");
        model.addAttribute("mp4link", ImageProcessor.objectStoreLocation + email + ".mp4");
        model.addAttribute("mp4text", "MP4 Animation");
        return "digitaldoubleresults";
    }


}
