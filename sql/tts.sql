create or replace FUNCTION speech_synthesizespeech (oci_cred IN VARCHAR2) RETURN BLOB AS
    speech_endpoint  varchar2(500) := 'https://speech.aiservice.us-phoenix-1.oci.oraclecloud.com/20220101/actions/synthesizeSpeech';
    resp dbms_cloud_types.RESP;
    request_json CLOB;
    request_body BLOB;
BEGIN
    request_json := to_clob(
        '{
          "audioConfig": {
            "configType": "BASE_AUDIO_CONFIG"
          },
          "compartmentId": "ocid1.compartment.oc1..{change me}",
          "configuration": {
            "modelDetails": {
              "modelName": "TTS_2_NATURAL",
              "voiceId": "Annabelle"
            },
            "modelFamily": "ORACLE",
            "speechSettings": {
              "outputFormat": "MP3",
              "sampleRateInHz": 23600,
              "speechMarkTypes": ["WORD"],
              "textType": "TEXT"
            }
          },
          "isStreamEnabled": false,
          "text": "In Python, the placement of your function (subroutine) definitions matters due to how the interpreter reads and executes the code. Here is why:"
        }'
    );

    request_body := apex_util.clob_to_blob(p_clob => request_json,p_charset => 'AL32UTF8');

    resp := dbms_cloud.send_request(
        credential_name => oci_cred,
        uri => speech_endpoint,
        method => dbms_cloud.METHOD_POST,
        body => request_body
    );

    --RETURN request_body;
    RETURN dbms_cloud.get_response_raw(resp);
END speech_synthesizespeech;