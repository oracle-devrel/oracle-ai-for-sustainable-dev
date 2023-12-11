--run as aiuser

CREATE TABLE aivision_results
    (id RAW (16) NOT NULL,
     date_loaded TIMESTAMP WITH TIME ZONE,
     label varchar2(20),
     textfromai varchar2(32767),
     jsondata CLOB
     CONSTRAINT ensure_aivision_results_json CHECK (jsondata IS JSON));
/

create index aivisionresultsindex on aivision_results(textfromai) indextype is ctxsys.context;
--select index_name, index_type, status from user_indexes where index_name = 'AIVISIONAIRESULTSINDEX';
--select idx_name, idx_table, idx_text_name from ctx_user_indexes;
--select token_text from dr$aivisionresultsindex$i;
/

BEGIN
    ORDS.ENABLE_OBJECT(
        P_ENABLED      => TRUE,
        P_SCHEMA      => 'AIUSER',
        P_OBJECT      =>  'EXECUTE_DYNAMIC_SQL',
        P_OBJECT_TYPE      => 'FUNCTION',
        P_OBJECT_ALIAS      => 'EXECUTE_DYNAMIC_SQL',
        P_AUTO_REST_AUTH      => FALSE
    );
    COMMIT;
END;

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

CREATE OR REPLACE FUNCTION call_analyze_image_api_objectstore (
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
END call_analyze_image_api_objectstore;
/

BEGIN
    ORDS.ENABLE_OBJECT(
        P_ENABLED      => TRUE,
        P_SCHEMA      => 'AIUSER',
        P_OBJECT      =>  'CALL_ANALYZE_IMAGE_API_OBJECTSTORE',
        P_OBJECT_TYPE      => 'FUNCTION',
        P_OBJECT_ALIAS      => 'call_analyze_image_api_objectstore',
        P_AUTO_REST_AUTH      => FALSE
    );
    COMMIT;
END;
/
