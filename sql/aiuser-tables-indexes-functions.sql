--run as aiuser

CREATE TABLE aivision_results
    (id RAW (16) NOT NULL,
     date_loaded TIMESTAMP WITH TIME ZONE,
     label varchar2(20),
     jsondata CLOB
     CONSTRAINT ensure_aivision_results_json CHECK (jsondata IS JSON));
create index aivisionresultsindex on aivision_results(jsondata) indextype is ctxsys.context;
--select index_name, index_type, status from user_indexes where index_name = 'AIVISIONAIRESULTSINDEX';
--select idx_name, idx_table, idx_text_name from ctx_user_indexes;
--select token_text from dr$aivisionresultsindex$i;


CREATE TABLE aispeech_results
    (id RAW (16) NOT NULL,
     date_loaded TIMESTAMP WITH TIME ZONE,
     label varchar2(20),
     jsondata CLOB
     CONSTRAINT aispeech_results_json CHECK (jsondata IS JSON));
create index aispeechresultsindex on aispeech_results(jsondata) indextype is ctxsys.context;
--select index_name, index_type, status from user_indexes where index_name = 'AISPEECHRESULTSINDEX';
--select idx_name, idx_table, idx_text_name from ctx_user_indexes;
--select token_text from dr$aispeechresultsindex$i;

CREATE OR REPLACE FUNCTION execute_dynamic_sql(p_sql IN VARCHAR2) RETURN VARCHAR2 IS
    v_result VARCHAR2(4000);
BEGIN
    EXECUTE IMMEDIATE p_sql INTO v_result;
    RETURN v_result;
EXCEPTION
    WHEN OTHERS THEN
        RETURN SQLERRM;
END execute_dynamic_sql;
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


create or replace FUNCTION call_analyze_image_api_objectstore (
    p_endpoint VARCHAR2,
    p_compartment_ocid VARCHAR2,
    p_namespaceName VARCHAR2,
    p_bucketName VARCHAR2,
    p_objectName VARCHAR2,
    p_featureType VARCHAR2,
    p_label VARCHAR2
) RETURN CLOB IS
    resp DBMS_CLOUD_TYPES.resp;
    json_response CLOB;
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
--    dbms_output.put_line('json_response: ' || json_response);
    INSERT INTO aivision_results VALUES (SYS_GUID(), SYSTIMESTAMP, p_label, json_response );
    RETURN json_response;
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
