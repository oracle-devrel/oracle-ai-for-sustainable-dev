package oracleai;

import com.oracle.bmc.retrier.RetryConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@SpringBootApplication
public class AIApplication {

    public static final String COMPARTMENT_ID="ocid1.compartment.oc1..aaaaaaaafnah3ogykjsg34qruhixhb2drls6zhsejzm7mubi2i5qj66slcoq";
    public static final String OBJECTSTORAGE_NAMESPACE="oradbclouducm";
    public static final String OBJECTSTORAGE_BUCKETNAME="doc";
    public static final String ORDS_OMLOPSENDPOINT_URL="https://rddainsuh6u1okc-ragdb.adb.us-ashburn-1.oraclecloudapps.com/ords/omlopsuser/";
    public static final String ORDS_ENDPOINT_URL="https://rddainsuh6u1okc-gd740878851.adb.us-ashburn-1.oraclecloudapps.com/ords/aiuser/";
    public static final String OCI_VISION_SERVICE_ENDPOINT="https://vision.aiservice.myregion.oci.oraclecloud.com";
    public static final String OCI_SPEECH_SERVICE_ENDPOINT="https://speech.aiservice.myregion.oci.oraclecloud.com";
    public static final String OCI_GENAI_SERVICE_ENDPOINT="https://inference.generativeai.us-chicago-1.oci.oraclecloud.com";
    public static final String OPENAI_KEY="sk-proj-708e3KQqGY9fGfoJ4edWT3BlbkFJMGcsVq7JBOWPg4mxn0Y8";
    public static final String THREEDEY = "msy_oCS1X5nuRxS06AjdlTJ0vCHg3OFyOhpaCMoa";
    public static String OCICONFIG_FILE = "~/.oci/config";
    public static final String OCICONFIG_PROFILE = "DEFAULT";
    public static final String OCICONFIG_FILECONTENTS = "[DEFAULT]\n" +
            "user = ocid1.user.oc1..aaaaaaaasd37w2te2a2c4ijpaf4axsxmky6hinmygkqm2csijpvvbspzfssq\n" +
            "fingerprint = 97:86:0a:a9:1a:af:c0:20:59:31:0b:a9:20:cf:f5:bc\n" +
            "tenancy = ocid1.tenancy.oc1..aaaaaaaaj4ccqe763dizkrcdbs5x7ufvmmojd24mb6utvkymyo4xwxyv3gfa\n" +
            "region = us-ashburn-1\n" +
            "key_file = /Users/pparkins/.ssh/oracleidentitycloudservice_paul.parkinson-05-07-03-14.pem\n";
//    public static final String COMPARTMENT_ID = System.getenv("COMPARTMENT_ID");
//    public static final String OBJECTSTORAGE_NAMESPACE = System.getenv("OBJECTSTORAGE_NAMESPACE");
//    public static final String OBJECTSTORAGE_BUCKETNAME = System.getenv("OBJECTSTORAGE_BUCKETNAME");
//    public static final String ORDS_ENDPOINT_URL = System.getenv("ORDS_ENDPOINT_URL");
//    public static final String OCI_VISION_SERVICE_ENDPOINT = System.getenv("OCI_VISION_SERVICE_ENDPOINT");
//    public static final String OCI_SPEECH_SERVICE_ENDPOINT = System.getenv("OCI_SPEECH_SERVICE_ENDPOINT");
//    public static final String OCI_GENAI_SERVICE_ENDPOINT = System.getenv("OCI_GENAI_SERVICE_ENDPOINT");
//    public static final String OCICONFIG_FILE = System.getenv("OCICONFIG_FILE");
//    public static final String OCICONFIG_PROFILE = System.getenv("OCICONFIG_PROFILE");

    static {
        System.out.println("AIApplication.static initializer COMPARTMENT_ID:" + COMPARTMENT_ID);
        System.out.println("AIApplication.static initializer OBJECTSTORAGE_NAMESPACE:" + OBJECTSTORAGE_NAMESPACE);
        System.out.println("AIApplication.static initializer OBJECTSTORAGE_BUCKETNAME:" + OBJECTSTORAGE_BUCKETNAME);
        System.out.println("AIApplication.static initializer ORDS_ENDPOINT_URL:" + ORDS_ENDPOINT_URL);
        System.out.println("AIApplication.static initializer OCI_VISION_SERVICE_ENDPOINT:" + OCI_VISION_SERVICE_ENDPOINT);

        String configContent = "-----BEGIN PRIVATE KEY-----\n" +
                "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDIMHqvSkZnLcOC\n" +
                "+g46kZUA1Zat9GYJ7Y2wHDYqZaSGMc3jwEo8jHCJfVaPh+0sq4kIDtAsog09XR+s\n" +
                "LUitRee8SfO+hWcIYwZhYHglrfeUnAio4HQXZCiU3DC2WTD4DsF0WS9jbxuWLg89\n" +
                "svvO5Ab+w44x1tE3sEj/lnRMHCsyEmrUBLE0mHdGdapvWZPlnKcc3IkWlA3CeZiq\n" +
                "tuUYZdC/yQSW9K7L1JLyo91+is22miENVwfrGgpiMln43Q0Wrti3/LqITq2Ucvd6\n" +
                "e4/9lSPao59v2UdEFuZmEWR3KDluZ35csC1DEF05x0k6E9No8SFFU7JcEV8pXpg8\n" +
                "fvrO2TddAgMBAAECggEAUsFXtibqO7T7YaWlRCjyg8nH5Fln5SrCq869xYEHJkJo\n" +
                "boILLkSQMQYTRnpWXWT73Mzi2dCT0I4yjaaaqKaBSaD4lgXntdvZw19xy4HDGzf1\n" +
                "jHpdA15wODleGcN9Ls5KwqIrqNtBeC/KNSNMXRZ+ncqReaDoFDFXu/CiWuQ6JxQi\n" +
                "gjX/nSPRjJaKXQwQSxQxVr6DI+dG1fzmln0MGYvsTw7CagFY4V0mIgNzMZhB5Jtz\n" +
                "0ceJYPQv4fBhBYFgw6NLuXrweFaFVwAAZRWUVlMKUQ5+FhMDjGwybgbwMISTHJZb\n" +
                "WktmQh+Ek1nw89u9tMOOkz7fn+2RKUjxJRoBqVNMsQKBgQDv2kPVX3g1cRwGxHgS\n" +
                "oeKUbLpDXY5SvB8A9+f8kz3eAzyLYs9JNcwT+XN3ZKTZcFcYsUJHfNU9IYaHScHX\n" +
                "sOjNof+zNfbKWTnLujU4iGAXMVpnkRvYM4TPAlsllt0n/gS2ByM3ztX7cIOGAhCa\n" +
                "XiQGp294sShaRDr0YhuMT80XMwKBgQDVqqOf0Z3Bi8vL7OO46PipPnqzKRFr+qOm\n" +
                "JIsTZTed0ORzhuLiZ4YGaI/wRxIugv03odxUGXmYscuocmGkGIQEiibQVWoXCW6L\n" +
                "0jWyjMK+r1Nue0kKh5ikIAEZzWMvqm2rL7o8C3AHRdH9hdSQ/cl7UCq6Q8nTgUwP\n" +
                "XdrYLvI3LwKBgQCoZvkFkRhXfWkOH1emzV7Gk2hb8A4DbO3fwi9xmPfxLxiNTPz5\n" +
                "C/qTc6tsgo9z03pzbdF5W95kI2vPBSxCgoWSC1H6w8LY47i+n6jKXoYJAq/U039X\n" +
                "VyTCK4dPZxM0BxfLrks93c1D0wWlS0HZAIwO1/ReKxSH/CvgdhmddFItHQKBgG37\n" +
                "+2P+G62QT2R2WTZKvg0oezKUFkK17t1L/EcMmBdleuM48LtIPMY5trYhb2t/w+aK\n" +
                "LAciYeRGySW5UyeL8xD/KEGfZg/brArPNxLkwC41w8WgMpX3/IunacmlXsQff3pa\n" +
                "BAx6er1TpGdTJ3tHGBqgxo3A7TgxWzyhvFABwm7DAoGAT49+Sdn8Uk3HRyC2v2Zy\n" +
                "psvKfgt9tyXhixUhyPh2JSUAyEyxTP7okqjNtu4eHq+oxMhwSVwOdfxvQyb5NVQu\n" +
                "sMZ6x5oz6yXR6UkoRwsqHdNhT205hZLJGjNEa8gK5dlG23jqLpB+B2GGt6LOtxnF\n" +
                "XbXBbFxwvY/wy+xtdtQA6W0=\n" +
                "-----END PRIVATE KEY-----%";

        String pemFilePath = makeFileAndReturnFilePath(
                "oracleidentitycloudservice_paul.parkinson-05-07-03-14","pem",configContent);
        configContent = "[DEFAULT]\n" +
                "user = ocid1.user.oc1..aaaaaaaasd37w2te2a2c4ijpaf4axsxmky6hinmygkqm2csijpvvbspzfssq\n" +
                "fingerprint = 97:86:0a:a9:1a:af:c0:20:59:31:0b:a9:20:cf:f5:bc\n" +
                "tenancy = ocid1.tenancy.oc1..aaaaaaaaj4ccqe763dizkrcdbs5x7ufvmmojd24mb6utvkymyo4xwxyv3gfa\n" +
                "region = us-ashburn-1\n" +
                "key_file = "+pemFilePath;
        OCICONFIG_FILE = makeFileAndReturnFilePath("config", "", configContent);
        System.out.println("----------------OCICONFIG_FILE:" + OCICONFIG_FILE);
    }

    private static String makeFileAndReturnFilePath(String fileName, String suffix, String configContent) {
        String filePath;
        try {
            File configFile = File.createTempFile(fileName, suffix);
            try (FileWriter writer = new FileWriter(configFile)) {
                writer.write(configContent);
            }
            filePath = configFile.getAbsolutePath();
            System.out.println("Config file created at: " + filePath);
            return filePath;
        } catch (IOException e) {
            System.err.println("Error creating config file: " + e.getMessage());
            return "";
        }
    }

    public static void main(String[] args) {
//        RetryConfiguration retryConfiguration = RetryConfiguration.builder()
//                .terminationStrategy(RetryUtils.createExponentialBackoffStrategy(500, 5)) // Configure limits
//                .build();
        SpringApplication.run(AIApplication.class, args);
    }

}
