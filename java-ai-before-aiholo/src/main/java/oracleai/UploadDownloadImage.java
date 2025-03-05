package oracleai;

import oracleai.digitaldouble.ImageStore;
import oracleai.services.ORDSCalls;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


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

    @GetMapping("/downloadimages")
    public String getImageStoreData(Model model) {
        ImageStore[] imageStores = ORDSCalls.getImageStoreData();
        model.addAttribute("images", imageStores);
        return "images";
    }
}
