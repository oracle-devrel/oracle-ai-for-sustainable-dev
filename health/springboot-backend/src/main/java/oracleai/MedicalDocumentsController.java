package oracleai;

import oracleai.services.OracleDocumentAI;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.sql.DataSource;


@RestController
@RequestMapping("/medicaldocuments")
public class MedicalDocumentsController {
    private static final String INSERT_DOCUMENT_SQL =
            "INSERT INTO aidocument_results (id, date_loaded, documenttype, filename, jsondata) " +
            "VALUES (SYS_GUID(), SYSTIMESTAMP, ?, ?, ?)";

    JdbcTemplate jdbcTemplate;

    @Autowired
    public MedicalDocumentsController(DataSource dataSource) {
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @CrossOrigin
    @PostMapping("/analyzedocument")
    public String analyzedocument(@RequestParam("file") MultipartFile file)
            throws Exception {
        System.out.println("MedicalDocumentsController analyzing image file:" + file);
        String jsonData =  OracleDocumentAI.processDocument(file.getBytes());
        return parseAndInsertDocument(jsonData, file.getOriginalFilename());
    }

    /**
     * Parses the return and retains only the key value pair values and detectedDocumentType
     * The detectedDocumentType is retained in the JSON and the fileName is added to the JSON even though they are both redundant
     * as it makes it easier to query and return to the frontend (the frontend displays both)
     */
    private String parseAndInsertDocument(String jsonString, String fileName) {
        JSONObject jsonObject = new JSONObject(jsonString);
        JSONObject resultJson = new JSONObject();
        JSONArray documentTypesResult = new JSONArray();
        JSONArray documentFieldsResult = new JSONArray();
        resultJson.put("fileName", fileName);
        String documentType = "unknown";
        if (jsonObject.has("detectedDocumentTypes")) {
            JSONArray documentTypes = jsonObject.getJSONArray("detectedDocumentTypes");
            for (int i = 0; i < documentTypes.length(); i++) {
                JSONObject docType = documentTypes.getJSONObject(i);
                documentType = docType.getString("documentType");
                JSONObject docTypeObj = new JSONObject();
                docTypeObj.put("documentType", documentType);
                documentTypesResult.put(docTypeObj);
            }
            resultJson.put("detectedDocumentTypes", documentTypesResult);
        }
        if (jsonObject.has("pages")) {
            JSONArray pages = jsonObject.getJSONArray("pages");
            for (int i = 0; i < pages.length(); i++) {
                JSONObject page = pages.getJSONObject(i);
                if (page.has("documentFields")) {
                    JSONArray documentFields = page.getJSONArray("documentFields");
                    for (int j = 0; j < documentFields.length(); j++) {
                        JSONObject field = documentFields.getJSONObject(j);
                        JSONObject fieldLabel = field.getJSONObject("fieldLabel");
                        JSONObject fieldValue = field.getJSONObject("fieldValue");

                        String fieldName = fieldLabel.getString("name");
                        String text = fieldValue.optString("text", "");

                        JSONObject fieldResult = new JSONObject();
                        fieldResult.put("name", fieldName);
                        fieldResult.put("text", text);
                        documentFieldsResult.put(fieldResult);
                    }
                }
            }
            resultJson.put("documentFields", documentFieldsResult);
        }
        System.out.println(resultJson.toString());resultJson.toString();
        jdbcTemplate.update(INSERT_DOCUMENT_SQL, documentType, fileName, resultJson.toString());
        return resultJson.toString();
    }
}



