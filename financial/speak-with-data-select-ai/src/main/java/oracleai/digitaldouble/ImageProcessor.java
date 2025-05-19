package oracleai.digitaldouble;

import oracleai.AIApplication;
import oracleai.services.ORDSCalls;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Service
public class ImageProcessor {

    private static final String DIRECTORY = "/tmp/images/";
    private static final BlockingQueue<ImageTask> queue = new LinkedBlockingQueue<>();

    // Inject DigitalDoubleService to make REST call
    private final DigitalDoubleService digitalDoubleService;

    @Autowired
    public ImageProcessor(DigitalDoubleService digitalDoubleService) {
        this.digitalDoubleService = digitalDoubleService;
        new Thread(this::processQueue).start();
    }

    public String handleImageUpload(String email, MultipartFile image, String fullVideoName) throws IOException {
        String imageFileNameWithEmailPrefix = "";
        if (image != null && !image.isEmpty()) {
            FileUtils.forceMkdir(new File(DIRECTORY));
            imageFileNameWithEmailPrefix = email + "_" + image.getOriginalFilename();
            Path path = Paths.get(DIRECTORY + imageFileNameWithEmailPrefix);
            image.transferTo(path);
        }
        queue.offer(new ImageTask(email, imageFileNameWithEmailPrefix, fullVideoName));
        return "Image is being processed";
    }

    public static String objectStoreLocation =
            "https://" + AIApplication.OBJECTSTORAGE_NAMESPACE + ".compat.objectstorage.us-ashburn-1.oraclecloud.com/" +
                    AIApplication.OBJECTSTORAGE_BUCKETNAME + "/anim/";

    private void processQueue() {
        while (true) {
            try {
                ImageTask task = queue.take();
                DigitalDoubleDownloadInfo digitalDoubleDownloadInfo;
                if (!task.getImageFileNameWithEmailPrefix().equals("")) {
                    digitalDoubleDownloadInfo = ORDSCalls.convertImageAndQueueResults(
                            AIApplication.DIGITAL_DOUBLES_IMAGES_ENDPOINT,
                            task.getImageFileNameWithEmailPrefix());
                } else {
                    digitalDoubleDownloadInfo = new DigitalDoubleDownloadInfo();
                }

                digitalDoubleDownloadInfo.animatedVideoLocation = objectStoreLocation + task.getFullVideoName();

                // Call the method to update Digital Double data
                digitalDoubleService.updateDigitalDoubleData(
                        new DigitalDoubleDownloadInfo(
                                digitalDoubleDownloadInfo.modelGlbUrl,
                                digitalDoubleDownloadInfo.modelFbxUrl,
                                digitalDoubleDownloadInfo.modelUsdzUrl,
                                digitalDoubleDownloadInfo.thumbnailUrl,
                                digitalDoubleDownloadInfo.animatedVideoLocation,
                                task.getEmail(),
                                ""  // Similar image can be passed here if available
                        )
                );

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Failed to process image for: " + e.getMessage());
            }
        }
    }

    private static class ImageTask {
        private final String email;
        private final String imageFileNameWithEmailPrefix;
        private final String fullVideoName;

        public ImageTask(String email, String imageFileNameWithEmailPrefix, String fullVideoName) {
            this.email = email;
            this.imageFileNameWithEmailPrefix = imageFileNameWithEmailPrefix;
            this.fullVideoName = fullVideoName;
        }

        public String getEmail() {
            return email;
        }

        public String getImageFileNameWithEmailPrefix() {
            return imageFileNameWithEmailPrefix;
        }

        public String getFullVideoName() {
            return fullVideoName;
        }
    }
}