--This is a work in progress and only applicable to smaller image sizes
create or replace FUNCTION call_analyze_image_api_inline (
    p_endpoint VARCHAR2,
    p_compartment_ocid VARCHAR2,
    p_image_data CLOB
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
                    JSON_OBJECT('featureType' VALUE 'OBJECT_DETECTION')
                ),
                'image' VALUE JSON_OBJECT(
                    'source' VALUE 'INLINE',
                    'data' VALUE p_image_data
                ),
                'compartmentId' VALUE p_compartment_ocid
            )
        )
    );

    json_response := DBMS_CLOUD.get_response_text(resp);
     dbms_output.put_line('json_response: ' || json_response);
     INSERT INTO metering VALUES (SYS_GUID(), SYSTIMESTAMP, 'test', json_response );

    RETURN json_response;
EXCEPTION
    WHEN OTHERS THEN
        -- Handle exceptions if needed and return an error message or raise
        RAISE;
END call_analyze_image_api_inline;

BEGIN
    ORDS.ENABLE_OBJECT(
        P_ENABLED      => TRUE,
        P_SCHEMA      => 'AIUSER',
        P_OBJECT      =>  'CALL_ANALYZE_IMAGE_API_INLINE',
        P_OBJECT_TYPE      => 'FUNCTION',
        P_OBJECT_ALIAS      => 'call_analyze_image_api_inline',
        P_AUTO_REST_AUTH      => FALSE
    );
    COMMIT;
END;