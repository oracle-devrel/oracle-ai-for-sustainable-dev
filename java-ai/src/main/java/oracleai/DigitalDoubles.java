package oracleai;


import oracleai.services.ORDSCalls;
import oracleai.services.OracleObjectStore;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.http.MediaType;

@Controller
@RequestMapping("/digitaldoubles")
public class DigitalDoubles {

    @GetMapping("/uploadordownload")
    public String digitaldouble(@RequestParam("action") String action, Model model) {
        return action.equals("uploading")?"digitaldoubleupload":"digitaldoubledownload";
    }

    private static final String DIRECTORY = "/tmp/images/";
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
        System.out.println("image = " + image + ", video = " + video +", animstyle = " + animstyle +
                ", firstName = " + firstName + ", lastName = " + lastName +
                ", email = " + email + ", company = " + company +
                ", jobRole = " + jobRole + ", tshirtSize = " + tshirtSize +
                ", comments = " + comments + ", model = " + model +
                "\ncomments with animstyle and prompt = " + commentsWithAnimStyleAndPrompt);
        if (!image.isEmpty()) {
            ORDSCalls.insertDigitalDoubleData(
                    image,null, firstName, lastName, email, company,jobRole, tshirtSize, commentsWithAnimStyleAndPrompt);
            if (!video.isEmpty()) {
                OracleObjectStore.sendToObjectStorage(
                        email + "_" + video.getOriginalFilename()+ "_" + animstyle, video.getInputStream());
            }
            try {
                org.apache.commons.io.FileUtils.forceMkdir(new File(DIRECTORY));
                Path path = Paths.get(DIRECTORY + image.getOriginalFilename());
                image.transferTo(path);
                String fbxUrl = ORDSCalls.convertImage("http://129.80.168.144/digitaldoubles/images/",
                        image.getOriginalFilename());
                model.addAttribute("resultlink", fbxUrl);
                model.addAttribute("resulttext", "Click here for your FBX 3D model");
                return "resultswithlinkpage";
//            return ResponseEntity.ok(
//                    ORDSCalls.convertImage("http://129.80.168.144/transferimage/images/" + file.getOriginalFilename())
//            );
//            return ResponseEntity.ok("File uploaded and available at: " + "/images/" + file.getOriginalFilename());
            } catch (Exception e) {
                return e.toString();
//            ResponseEntity.internalServerError().body("Could not upload the file: " + e.getMessage());
            }
            // Save or process the image
        } else {
            model.addAttribute("resultlink", "http://129.80.168.144/UploadDigitalDouble.html");
            model.addAttribute("resulttext",
                    "Image not provided or is empty. Click here to try again.");
            return "resultswithlinkpage";
        }
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
    public String downloaddigitaldouble(@RequestParam("email") String email, Model model) {
        System.out.println("DigitalDoubles.downloaddigitaldouble lookup email:" + email);
        model.addAttribute("resultlink", email);
        model.addAttribute("resulttext", "Click here for your FBX 3D model");
        return "resultswithlinkpage";
//        return "digitaldoubleresults";
    }

}
