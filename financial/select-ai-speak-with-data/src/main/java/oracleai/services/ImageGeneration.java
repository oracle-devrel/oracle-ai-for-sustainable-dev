package oracleai.services;

import com.theokanning.openai.image.CreateImageRequest;
import com.theokanning.openai.service.OpenAiService;

import java.time.Duration;

public class ImageGeneration {

    static public String imagegeneration(String imagedescription) throws Exception {
        OpenAiService service =
                new OpenAiService(System.getenv("OPENAI_KEY"), Duration.ofSeconds(60));
        CreateImageRequest openairequest = CreateImageRequest.builder()
                .prompt(imagedescription)
                .build();
        String imageLocation = service.createImage(openairequest).getData().get(0).getUrl();
        System.out.println("Image is located at:" + imageLocation);
        service.shutdownExecutor();
        return imageLocation;
    }

}
