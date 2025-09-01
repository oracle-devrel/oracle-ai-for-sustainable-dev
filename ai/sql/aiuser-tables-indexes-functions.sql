CREATE TABLE aivision_results
    (id RAW (16) NOT NULL,
     date_loaded TIMESTAMP WITH TIME ZONE,
     label varchar2(20),
     textfromai varchar2(32767),
     jsondata CLOB
     CONSTRAINT ensure_aivision_results_json CHECK (jsondata IS JSON));
/

create index aivisionresultsindex on aivision_results(textfromai) indextype is ctxsys.context;
/

--select index_name, index_type, status from user_indexes where index_name = 'AIVISIONAIRESULTSINDEX';
--select idx_name, idx_table, idx_text_name from ctx_user_indexes;
--select token_text from dr$aivisionresultsindex$i;


CREATE OR REPLACE FUNCTION VISIONAI_RESULTS_TEXT_SEARCH(p_sql IN VARCHAR2) RETURN SYS_REFCURSOR AS refcursor SYS_REFCURSOR;
BEGIN
    OPEN refcursor FOR
        select textfromai from AIVISION_RESULTS where contains ( textfromai, p_sql ) > 0;
    RETURN refcursor;
END VISIONAI_RESULTS_TEXT_SEARCH;
/


BEGIN
    ORDS.ENABLE_OBJECT(
        P_ENABLED      => TRUE,
        P_SCHEMA      => 'AIUSER',
        P_OBJECT      =>  'VISIONAI_RESULTS_TEXT_SEARCH',
        P_OBJECT_TYPE      => 'FUNCTION',
        P_OBJECT_ALIAS      => 'VISIONAI_RESULTS_TEXT_SEARCH',
        P_AUTO_REST_AUTH      => FALSE
    );
    COMMIT;
END;
/

--Easy Text Search over Multiple Tables and Views with DBMS_SEARCH in 23c
--workshop: https://apexapps.oracle.com/pls/apex/r/dbpm/livelabs/view-workshop?wid=3721

BEGIN
dbms_cloud.create_credential (
    credential_name => 'OCI_KEY_CRED',
    user_ocid => 'ocid1.user.oc1..[youruserocid]',
    tenancy_ocid => 'ocid1.tenancy.oc1..[yourtenancyocid]',
    private_key => '[yourprivatekey you can read this from file or put the contents of your pem without header, footer, and line wraps]'
    fingerprint => '[7f:yourfingerprint]'
);
END;

--The following two functions (text and object detection) are identical
--except for the json_table parsing for the textfromai field,
--and so technically the p_featureType is not necessary and the function could be made to handle/parse both types,
--however, for readability they are broken into two distinct functions.


CREATE OR REPLACE FUNCTION VISIONAI_TEXTDETECTION (
    p_endpoint VARCHAR2,
    p_compartment_ocid VARCHAR2,
    p_namespaceName VARCHAR2,
    p_bucketName VARCHAR2,
    p_objectName VARCHAR2,
    p_featureType VARCHAR2,
    p_label VARCHAR2
) RETURN VARCHAR2 IS
    resp DBMS_CLOUD_TYPES.resp;
    json_response CLOB;
    v_textfromai VARCHAR2(32767);
BEGIN
    resp := DBMS_CLOUD.send_request(
        credential_name => 'OCI_KEY_CRED',
        uri => p_endpoint || '/20220125/actions/analyzeImage',
        method => 'POST',
        body => UTL_RAW.cast_to_raw(
            JSON_OBJECT(
                'features' VALUE JSON_ARRAY(
                    JSON_OBJECT('featureType' VALUE p_featureType)
                ),
                'image' VALUE JSON_OBJECT(
                    'source' VALUE 'OBJECT_STORAGE',
                    'namespaceName' VALUE p_namespaceName,
                    'bucketName' VALUE p_bucketName,
                    'objectName' VALUE p_objectName
                ),
                'compartmentId' VALUE p_compartment_ocid
            )
        )
    );
    json_response := DBMS_CLOUD.get_response_text(resp);
    SELECT LISTAGG(text, ', ') WITHIN GROUP (ORDER BY ROWNUM)
    INTO v_textfromai
    FROM JSON_TABLE(json_response, '$.imageText.words[*]'
        COLUMNS (
            text VARCHAR2(100) PATH '$.text'
        )
    );
    INSERT INTO aivision_results (id, date_loaded, label, textfromai, jsondata)
    VALUES (SYS_GUID(), SYSTIMESTAMP, p_label, v_textfromai, json_response);
    RETURN v_textfromai;
EXCEPTION
    WHEN OTHERS THEN
        RAISE;
END VISIONAI_TEXTDETECTION;
/

BEGIN
    ORDS.ENABLE_OBJECT(
        P_ENABLED      => TRUE,
        P_SCHEMA      => 'AIUSER',
        P_OBJECT      =>  'VISIONAI_TEXTDETECTION',
        P_OBJECT_TYPE      => 'FUNCTION',
        P_OBJECT_ALIAS      => 'VISIONAI_TEXTDETECTION',
        P_AUTO_REST_AUTH      => FALSE
    );
    COMMIT;
END;
/

CREATE OR REPLACE FUNCTION VISIONAI_OBJECTDETECTION (
    p_endpoint VARCHAR2,
    p_compartment_ocid VARCHAR2,
    p_namespaceName VARCHAR2,
    p_bucketName VARCHAR2,
    p_objectName VARCHAR2,
    p_featureType VARCHAR2,
    p_label VARCHAR2
) RETURN VARCHAR2 IS
    resp DBMS_CLOUD_TYPES.resp;
    json_response CLOB;
    v_textfromai VARCHAR2(32767);
BEGIN
    resp := DBMS_CLOUD.send_request(
        credential_name => 'OCI_KEY_CRED',
        uri => p_endpoint || '/20220125/actions/analyzeImage',
        method => 'POST',
        body => UTL_RAW.cast_to_raw(
            JSON_OBJECT(
                'features' VALUE JSON_ARRAY(
                    JSON_OBJECT('featureType' VALUE p_featureType)
                ),
                'image' VALUE JSON_OBJECT(
                    'source' VALUE 'OBJECT_STORAGE',
                    'namespaceName' VALUE p_namespaceName,
                    'bucketName' VALUE p_bucketName,
                    'objectName' VALUE p_objectName
                ),
                'compartmentId' VALUE p_compartment_ocid
            )
        )
    );
    json_response := DBMS_CLOUD.get_response_text(resp);
    SELECT LISTAGG(name, ', ') WITHIN GROUP (ORDER BY ROWNUM)
    INTO v_textfromai
    FROM JSON_TABLE(json_response, '$.imageObjects[*]'
        COLUMNS (
            name VARCHAR2(100) PATH '$.name'
        )
    );
    INSERT INTO aivision_results (id, date_loaded, label, textfromai, jsondata)
    VALUES (SYS_GUID(), SYSTIMESTAMP, p_label, v_textfromai, json_response);
    RETURN v_textfromai;
EXCEPTION
    WHEN OTHERS THEN
        RAISE;
END VISIONAI_OBJECTDETECTION;
/

BEGIN
    ORDS.ENABLE_OBJECT(
        P_ENABLED      => TRUE,
        P_SCHEMA      => 'AIUSER',
        P_OBJECT      =>  'VISIONAI_OBJECTDETECTION',
        P_OBJECT_TYPE      => 'FUNCTION',
        P_OBJECT_ALIAS      => 'VISIONAI_OBJECTDETECTION',
        P_AUTO_REST_AUTH      => FALSE
    );
    COMMIT;
END;
/


CREATE TABLE  image_store (
    id NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    image BLOB,
    image_name VARCHAR2(100)
)
/

create or replace PROCEDURE insert_image(p_image_name IN VARCHAR2, p_image BLOB) IS
BEGIN
    INSERT INTO image_store (image_name, image) VALUES (p_image_name, p_image);
END;
/

BEGIN
    ORDS.ENABLE_OBJECT(
        P_ENABLED      => TRUE,
        P_SCHEMA      => 'AIUSER',
        P_OBJECT      =>  'INSERT_IMAGE',
        P_OBJECT_TYPE      => 'PROCEDURE',
        P_OBJECT_ALIAS      => 'insert_image',
        P_AUTO_REST_AUTH      => FALSE
    );
    COMMIT;
END;
/

--curl --location --request POST \
--'https://yourORDSendpoint.adb.us-ashburn-1.oraclecloudapps.com/ords/aiuser/insert_image/' \
----header 'Content-Type: application/json' \
----data-binary '{
--  "p_image_name": "<VALUE>",
--  "p_image": "<VALUE>"
--}'

BEGIN
    ORDS.ENABLE_OBJECT(
        P_ENABLED      => TRUE,
        P_SCHEMA      => 'AIUSER',
        P_OBJECT      =>  'IMAGE_STORE',
        P_OBJECT_TYPE      => 'TABLE',
        P_OBJECT_ALIAS      => 'image_store',
        P_AUTO_REST_AUTH      => FALSE
    );
    COMMIT;
END;
/

--curl --location \
--'https://yourORDSendpoint.adb.us-ashburn-1.oraclecloudapps.com/ords/aiuser/image_store/'
