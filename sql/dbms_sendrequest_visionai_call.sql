
 set serveroutput on
 declare
     resp DBMS_CLOUD_TYPES.resp;
     endpoint varchar2(64) := 'https://vision.aiservice.us-ashburn-1.oci.oraclecloud.com';
     mydata varchar2(32000);
 begin
     resp := DBMS_CLOUD.send_request(credential_name => 'OCI_KEY_CRED',uri =>
         endpoint || '/20220125/actions/analyzeImage',
         method => 'POST',
         body => UTL_RAW.cast_to_raw(
            JSON_OBJECT(
                'features' VALUE JSON_ARRAY(
                    JSON_OBJECT('featureType' VALUE 'OBJECT_DETECTION')
                ),
                'image' VALUE JSON_OBJECT(
                    'source' VALUE 'OBJECT_STORAGE',
                    'namespaceName' VALUE 'yournamespace',
                    'bucketName' VALUE 'yourbucketname',
                    'objectName' VALUE 'objectdetectiontestimage.jpg'
                ),
                'compartmentId' VALUE 'ocid1.compartment.oc1..yourcompartmentid'
            ))
         );
     mydata :=  DBMS_CLOUD.get_response_text(resp);
     dbms_output.put_line('result: ' || mydata);
     INSERT INTO aivision_results VALUES (SYS_GUID(), SYSTIMESTAMP, 'test', mydata );
     commit;
 end;








