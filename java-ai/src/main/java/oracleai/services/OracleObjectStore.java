package oracleai.services;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.responses.GetObjectResponse;
import com.oracle.bmc.objectstorage.responses.PutObjectResponse;
import oracleai.AIApplication;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class OracleObjectStore {


    public static void sendToObjectStorage(String fileName, InputStream inputStreamForFile) throws IOException {
        System.out.println("sendToObjectStorage fileToUpload:" + fileName);
        BasicAuthenticationDetailsProvider provider = AuthProvider.getAuthenticationDetailsProvider();
        ObjectStorageClient client = ObjectStorageClient.builder().build(provider);
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .namespaceName(AIApplication.OBJECTSTORAGE_NAMESPACE)
                .bucketName(AIApplication.OBJECTSTORAGE_BUCKETNAME)
                .objectName(fileName)
                .putObjectBody(inputStreamForFile) //InputStream
                .build();
        PutObjectResponse response = client.putObject(putObjectRequest);
        System.out.println("File uploaded successfully. Object Storage Location: " + fileName);
    }

    public static String getFromObjectStorage(String transcriptionJobId, String objectName) throws Exception {
        System.out.println("GenerateAPictureStoryUsingOnlySpeech.getFromObjectStorage objectName:" + objectName);
        BasicAuthenticationDetailsProvider provider = AuthProvider.getAuthenticationDetailsProvider();
        ObjectStorageClient client = ObjectStorageClient.builder().build(provider);
        GetObjectRequest putObjectRequest = GetObjectRequest.builder()
                .namespaceName(AIApplication.OBJECTSTORAGE_NAMESPACE)
                .bucketName(AIApplication.OBJECTSTORAGE_BUCKETNAME)
                .objectName(transcriptionJobId + "/" + objectName)
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
