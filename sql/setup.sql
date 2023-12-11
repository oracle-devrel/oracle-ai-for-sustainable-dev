--run as aiuser

CREATE TABLE aivision_results
    (id RAW (16) NOT NULL,
     date_loaded TIMESTAMP WITH TIME ZONE,
     label varchar2(20),
     jsondata CLOB
     CONSTRAINT ensure_aivision_results_json CHECK (jsondata IS JSON));
create index aivisionresultsindex on aivision_results(jsondata) indextype is ctxsys.context;
select index_name, index_type, status from user_indexes where index_name = 'AIVISIONAIRESULTSINDEX';
select idx_name, idx_table, idx_text_name from ctx_user_indexes;
select token_text from dr$aivisionresultsindex$i;


CREATE TABLE aispeech_results
    (id RAW (16) NOT NULL,
     date_loaded TIMESTAMP WITH TIME ZONE,
     label varchar2(20),
     jsondata CLOB
     CONSTRAINT aispeech_results_json CHECK (jsondata IS JSON));
create index aispeechresultsindex on aispeech_results(jsondata) indextype is ctxsys.context;
select index_name, index_type, status from user_indexes where index_name = 'AISPEECHRESULTSINDEX';
select idx_name, idx_table, idx_text_name from ctx_user_indexes;
select token_text from dr$aispeechresultsindex$i;

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

https://rddainsuh6u1okc-gd740878851.adb.us-ashburn-1.oraclecloudapps.com/ords/aiuser/call_analyze_image_api_objectstore
https://rddainsuh6u1okc-gd740878851.adb.us-ashburn-1.oraclecloudapps.com/ords/aiuser/call_analyze_image_api_objectstore