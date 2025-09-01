package oracleai.services;

import com.oracle.bmc.Region;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
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
        AuthenticationDetailsProvider provider = AuthProvider.getAuthenticationDetailsProvider();
        ObjectStorageClient client = ObjectStorageClient.builder().build(provider);

        // Set region to match the configuration in AIApplication
        client.setRegion(Region.fromRegionId(AIApplication.OBJECTSTORAGE_REGION));

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .namespaceName(AIApplication.OBJECTSTORAGE_NAMESPACE)
                    .bucketName(AIApplication.OBJECTSTORAGE_BUCKETNAME)
                    .objectName(fileName)
                    .putObjectBody(inputStreamForFile) // InputStream
                    .build();
            PutObjectResponse response = client.putObject(putObjectRequest);
            System.out.println("File uploaded successfully. Object Storage Location: " + fileName);
        } catch (Exception e) {
            System.err.println("Failed to upload to Object Storage. Bucket: " + AIApplication.OBJECTSTORAGE_BUCKETNAME +
                    ", Namespace: " + AIApplication.OBJECTSTORAGE_NAMESPACE +
                    ", Region: " + AIApplication.OBJECTSTORAGE_REGION);
            System.err.println("Error: " + e.getMessage());
            throw e; // Re-throw to maintain original behavior
        }
    }

    public static String getFromObjectStorage(String transcriptionJobId, String objectName) throws Exception {
        System.out.println("GenerateAPictureStoryUsingOnlySpeech.getFromObjectStorage objectName:" + objectName);
        System.out.println("Attempting to retrieve: " + transcriptionJobId + "/" + objectName);

        AuthenticationDetailsProvider provider = AuthProvider.getAuthenticationDetailsProvider();
        ObjectStorageClient client = ObjectStorageClient.builder().build(provider);

        // Set region to match the configuration in AIApplication
        client.setRegion(Region.fromRegionId(AIApplication.OBJECTSTORAGE_REGION));

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .namespaceName(AIApplication.OBJECTSTORAGE_NAMESPACE)
                    .bucketName(AIApplication.OBJECTSTORAGE_BUCKETNAME)
                    .objectName(transcriptionJobId + "/" + objectName)
                    .build();
            GetObjectResponse response = client.getObject(getObjectRequest);
            String responseString = "";
            try (InputStream inputStream = response.getInputStream();
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    responseString += line;
                }
            }
            return responseString;
        } catch (Exception e) {
            System.err.println("Failed to retrieve from Object Storage:");
            System.err.println("  Bucket: " + AIApplication.OBJECTSTORAGE_BUCKETNAME);
            System.err.println("  Namespace: " + AIApplication.OBJECTSTORAGE_NAMESPACE);
            System.err.println("  Region: " + AIApplication.OBJECTSTORAGE_REGION);
            System.err.println("  Object path: " + transcriptionJobId + "/" + objectName);
            System.err.println("  Error: " + e.getMessage());

            // Return a properly formatted error JSON with escaped characters
            String escapedMessage = e.getMessage()
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
            return "{\"error\": \"File not found or not yet available\", \"message\": \"" + escapedMessage + "\"}";
        }
    }
}
