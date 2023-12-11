package oracleai.services;

import com.oracle.bmc.Region;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.responses.GetObjectResponse;
import com.oracle.bmc.objectstorage.responses.PutObjectResponse;
import oracleai.AIApplication;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class OracleObjectStore {


    public static void sendToObjectStorage(String fileName, InputStream inputStreamForFile) throws Exception {
        System.out.println("GenerateAPictureStoryUsingOnlySpeech.sendToObjectStorage fileToUpload:" + fileName);
        ObjectStorageClient client;
        AuthenticationDetailsProvider provider;
        if (true) {
            provider = new ConfigFileAuthenticationDetailsProvider(
                    System.getenv("OCICONFIG_FILE"), System.getenv("OCICONFIG_PROFILE"));
            client =
                    ObjectStorageClient.builder().region(Region.US_CHICAGO_1).build(provider);
//                    ObjectStorageClient.builder().region(Region.US_CHICAGO_1.US_PHOENIX_1).build(provider);
        } else {
//            aiServiceVisionClient = new AIServiceVisionClient(InstancePrincipalsAuthenticationDetailsProvider.builder().build());
        }
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .namespaceName(AIApplication.OBJECTSTORAGE_NAMESPACE)
                .bucketName(AIApplication.OBJECTSTORAGE_BUCKETNAME)
                .objectName(fileName)
//                .objectName(audioFilePath.getFileName().toString())
                .putObjectBody(inputStreamForFile) //InputStream
                .build();
        PutObjectResponse response = client.putObject(putObjectRequest);
        System.out.println("File uploaded successfully. Object Storage Location: " + fileName);
    }

    public static String getFromObjectStorage(String transcriptionJobId, String objectName) throws Exception {
        System.out.println("GenerateAPictureStoryUsingOnlySpeech.getFromObjectStorage objectName:" + objectName);
        ObjectStorageClient client;
        AuthenticationDetailsProvider provider;
        if (true) {
            provider = new ConfigFileAuthenticationDetailsProvider(
                    System.getenv("OCICONFIG_FILE"), System.getenv("OCICONFIG_PROFILE"));
            client =
                    ObjectStorageClient.builder().region(Region.US_CHICAGO_1).build(provider);
        } else {
//            aiServiceVisionClient = new AIServiceVisionClient(InstancePrincipalsAuthenticationDetailsProvider.builder().build());
        }
        GetObjectRequest putObjectRequest = GetObjectRequest.builder()
                .namespaceName(AIApplication.OBJECTSTORAGE_NAMESPACE)
                .bucketName(AIApplication.OBJECTSTORAGE_BUCKETNAME)
                .objectName(transcriptionJobId + "/" + objectName)
//                .objectName(audioFilePath.getFileName().toString())
//                .putObjectBody(new FileInputStream(fileToUpload)) //InputStream
                .build();
        GetObjectResponse response = client.getObject(putObjectRequest);
        String responseString = "";
        try (InputStream inputStream = response.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                responseString += line;
            }
        }
        return responseString;
    }
}
