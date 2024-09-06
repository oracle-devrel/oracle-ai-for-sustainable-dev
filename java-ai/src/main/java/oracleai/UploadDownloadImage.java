package oracleai;


import oracleai.services.ORDSCalls;
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

import org.apache.commons.io.FileUtils;
import org.springframework.http.MediaType;

@Controller
@RequestMapping("/transferimage")
public class UploadDownloadImage {

    @PostMapping("/uploadimage")
    public String uploadImage(@RequestParam("image") MultipartFile image, Model model) {
        ORDSCalls.uploadImage(image);
        System.out.println("Image upload complete for: " + image.getOriginalFilename());
        ImageStore[] imageStores = ORDSCalls.getImageStoreData();
        model.addAttribute("images", imageStores);
        return "images";
    }

//    @PostMapping("/uploadimageandvideo")
//    public String uploadimageandvideo(@RequestParam("image") MultipartFile image, Model model) {
////        ORDSCalls.uploadImage(image);
////        System.out.println("Image upload complete for: " + image.getOriginalFilename());
//        System.out.println("convertImage(): " + ORDSCalls.convertImage());
//        ImageStore[] imageStores = ORDSCalls.getImageStoreData();
//        model.addAttribute("images", imageStores);
//        return "images";
//    }


    private static final String DIRECTORY = "/tmp/images/";

    @PostMapping("/uploadimageandvideo")
    public ResponseEntity<String> uploadImage(@RequestParam("image") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Cannot upload empty file");
        }

        try {
            FileUtils.forceMkdir(new File(DIRECTORY));
            Path path = Paths.get(DIRECTORY + file.getOriginalFilename());
            file.transferTo(path);

            return ResponseEntity.ok("File uploaded and available at: " + "/images/" + file.getOriginalFilename());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Could not upload the file: " + e.getMessage());
        }
    }

    @GetMapping("/images/{filename:.+}")
    public ResponseEntity<byte[]> getImage(@PathVariable String filename) throws IOException {
        try {
            File file = new File(DIRECTORY, filename);
            byte[] fileContent = FileUtils.readFileToByteArray(file);
            return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(fileContent);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }










    @GetMapping("/downloadimages")
    public String getImageStoreData(Model model) {
        ImageStore[] imageStores = ORDSCalls.getImageStoreData();
        model.addAttribute("images", imageStores);
        return "images";
    }
}
