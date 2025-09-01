-- Drop existing objects if they exist to avoid conflicts
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE aivision_results CASCADE CONSTRAINTS';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -942 THEN -- Table doesn't exist
            RAISE;
        END IF;
END;
/

-- Create the table with a unique constraint name
CREATE TABLE aivision_results
    (id RAW (16) NOT NULL,
     date_loaded TIMESTAMP WITH TIME ZONE,
     label varchar2(20),
     textfromai varchar2(32767),
     jsondata CLOB
     CONSTRAINT aivision_results_json_chk CHECK (jsondata IS JSON));
/

-- Create the full-text search index
CREATE INDEX aivisionresultsindex ON aivision_results(textfromai) 
INDEXTYPE IS ctxsys.context;
/

--select index_name, index_type, status from user_indexes where index_name = 'AIVISIONRESULTSINDEX';
--select idx_name, idx_table, idx_text_name from ctx_user_indexes;
--select token_text from dr$aivisionresultsindex$i;

-- Create the search function with improved syntax
CREATE OR REPLACE FUNCTION VISIONAI_RESULTS_TEXT_SEARCH(p_sql IN VARCHAR2) 
RETURN SYS_REFCURSOR 
AS 
    refcursor SYS_REFCURSOR;
BEGIN
    OPEN refcursor FOR
        SELECT textfromai FROM AIVISION_RESULTS 
        WHERE CONTAINS(textfromai, p_sql) > 0;
    RETURN refcursor;
END VISIONAI_RESULTS_TEXT_SEARCH;
/


-- Enable ORDS for VISIONAI_RESULTS_TEXT_SEARCH function
BEGIN
    ORDS.ENABLE_OBJECT(
        P_ENABLED      => TRUE,
        P_SCHEMA      => 'ADMIN',
        P_OBJECT      =>  'VISIONAI_RESULTS_TEXT_SEARCH',
        P_OBJECT_TYPE      => 'FUNCTION',
        P_OBJECT_ALIAS      => 'VISIONAI_RESULTS_TEXT_SEARCH',
        P_AUTO_REST_AUTH      => FALSE
    );
    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Successfully enabled ORDS for VISIONAI_RESULTS_TEXT_SEARCH');
EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Warning: Could not enable ORDS for VISIONAI_RESULTS_TEXT_SEARCH');
        DBMS_OUTPUT.PUT_LINE('Error: ' || SQLERRM);
        ROLLBACK;
END;
/

--Easy Text Search over Multiple Tables and Views with DBMS_SEARCH in 23c
--workshop: https://apexapps.oracle.com/pls/apex/r/dbpm/livelabs/view-workshop?wid=3721

-- Create OCI credential with error handling
BEGIN
    -- Drop credential if it exists
    BEGIN
        DBMS_CLOUD.DROP_CREDENTIAL(credential_name => 'OCI_KEY_CRED');
    EXCEPTION
        WHEN OTHERS THEN
            NULL; -- Ignore errors if credential doesn't exist
    END;
    
    -- Create new credential
    DBMS_CLOUD.CREATE_CREDENTIAL (
        credential_name => 'OCI_KEY_CRED',
        user_ocid => 'ocid1.user.oc1..[youruserocid]',
        tenancy_ocid => 'ocid1.tenancy.oc1..[yourtenancyocid]',
        private_key => '[yourprivatekey you can read this from file or put the contents of your pem without header, footer, and line wraps]',
        fingerprint => '[7f:yourfingerprint]'
    );
EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Warning: Could not create OCI credential. Functions will not work until credential is properly configured.');
        DBMS_OUTPUT.PUT_LINE('Error: ' || SQLERRM);
END;
/

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

-- Enable ORDS for VISIONAI_TEXTDETECTION function
BEGIN
    ORDS.ENABLE_OBJECT(
        P_ENABLED      => TRUE,
        P_SCHEMA      => 'ADMIN',
        P_OBJECT      =>  'VISIONAI_TEXTDETECTION',
        P_OBJECT_TYPE      => 'FUNCTION',
        P_OBJECT_ALIAS      => 'VISIONAI_TEXTDETECTION',
        P_AUTO_REST_AUTH      => FALSE
    );
    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Successfully enabled ORDS for VISIONAI_TEXTDETECTION');
EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Warning: Could not enable ORDS for VISIONAI_TEXTDETECTION');
        DBMS_OUTPUT.PUT_LINE('Error: ' || SQLERRM);
        ROLLBACK;
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

-- Enable ORDS for VISIONAI_OBJECTDETECTION function
BEGIN
    ORDS.ENABLE_OBJECT(
        P_ENABLED      => TRUE,
        P_SCHEMA      => 'ADMIN',
        P_OBJECT      =>  'VISIONAI_OBJECTDETECTION',
        P_OBJECT_TYPE      => 'FUNCTION',
        P_OBJECT_ALIAS      => 'VISIONAI_OBJECTDETECTION',
        P_AUTO_REST_AUTH      => FALSE
    );
    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Successfully enabled ORDS for VISIONAI_OBJECTDETECTION');
EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Warning: Could not enable ORDS for VISIONAI_OBJECTDETECTION');
        DBMS_OUTPUT.PUT_LINE('Error: ' || SQLERRM);
        ROLLBACK;
END;
/

-- Add sample data for testing (optional)
INSERT INTO aivision_results (id, date_loaded, label, textfromai, jsondata)
VALUES (
    SYS_GUID(),
    SYSTIMESTAMP,
    'sample',
    'Medical equipment detected: stethoscope, blood pressure monitor, patient chart with health information',
    '{"confidence": 0.95, "objects": ["medical_equipment", "patient"], "analysis": "medical equipment detection"}'
);
/

INSERT INTO aivision_results (id, date_loaded, label, textfromai, jsondata)
VALUES (
    SYS_GUID(),
    SYSTIMESTAMP,
    'xray',
    'X-ray analysis showing chest cavity, lung structure, possible anomaly in upper left quadrant',
    '{"confidence": 0.87, "objects": ["xray", "chest", "lung"], "analysis": "chest x-ray analysis"}'
);
/

COMMIT;
/

-- Test queries to verify functionality
-- SELECT * FROM TABLE(VISIONAI_RESULTS_TEXT_SEARCH('medical'));
-- SELECT * FROM TABLE(VISIONAI_RESULTS_TEXT_SEARCH('chest'));
-- SELECT * FROM TABLE(VISIONAI_RESULTS_TEXT_SEARCH('equipment AND patient'));

-- Utility queries to check table and index status
-- SELECT COUNT(*) as total_records FROM aivision_results;
-- SELECT id, label, SUBSTR(textfromai, 1, 50) as text_preview FROM aivision_results ORDER BY date_loaded DESC;

-- Verification queries
SET SERVEROUTPUT ON;
BEGIN
    DBMS_OUTPUT.PUT_LINE('=== VERIFICATION RESULTS ===');
    
    -- Check if table exists
    FOR rec IN (SELECT COUNT(*) as cnt FROM user_tables WHERE table_name = 'AIVISION_RESULTS') LOOP
        IF rec.cnt > 0 THEN
            DBMS_OUTPUT.PUT_LINE('✓ Table AIVISION_RESULTS exists');
        ELSE
            DBMS_OUTPUT.PUT_LINE('✗ Table AIVISION_RESULTS not found');
        END IF;
    END LOOP;
    
    -- Check if index exists
    FOR rec IN (SELECT COUNT(*) as cnt FROM user_indexes WHERE index_name = 'AIVISIONRESULTSINDEX') LOOP
        IF rec.cnt > 0 THEN
            DBMS_OUTPUT.PUT_LINE('✓ Index AIVISIONRESULTSINDEX exists');
        ELSE
            DBMS_OUTPUT.PUT_LINE('✗ Index AIVISIONRESULTSINDEX not found');
        END IF;
    END LOOP;
    
    -- Check if functions exist
    FOR rec IN (SELECT COUNT(*) as cnt FROM user_objects WHERE object_name = 'VISIONAI_RESULTS_TEXT_SEARCH' AND object_type = 'FUNCTION') LOOP
        IF rec.cnt > 0 THEN
            DBMS_OUTPUT.PUT_LINE('✓ Function VISIONAI_RESULTS_TEXT_SEARCH exists');
        ELSE
            DBMS_OUTPUT.PUT_LINE('✗ Function VISIONAI_RESULTS_TEXT_SEARCH not found');
        END IF;
    END LOOP;
    
    FOR rec IN (SELECT COUNT(*) as cnt FROM user_objects WHERE object_name = 'VISIONAI_TEXTDETECTION' AND object_type = 'FUNCTION') LOOP
        IF rec.cnt > 0 THEN
            DBMS_OUTPUT.PUT_LINE('✓ Function VISIONAI_TEXTDETECTION exists');
        ELSE
            DBMS_OUTPUT.PUT_LINE('✗ Function VISIONAI_TEXTDETECTION not found');
        END IF;
    END LOOP;
    
    FOR rec IN (SELECT COUNT(*) as cnt FROM user_objects WHERE object_name = 'VISIONAI_OBJECTDETECTION' AND object_type = 'FUNCTION') LOOP
        IF rec.cnt > 0 THEN
            DBMS_OUTPUT.PUT_LINE('✓ Function VISIONAI_OBJECTDETECTION exists');
        ELSE
            DBMS_OUTPUT.PUT_LINE('✗ Function VISIONAI_OBJECTDETECTION not found');
        END IF;
    END LOOP;
    
    -- Check data count
    FOR rec IN (SELECT COUNT(*) as cnt FROM aivision_results) LOOP
        DBMS_OUTPUT.PUT_LINE('✓ Sample data records: ' || rec.cnt);
    END LOOP;
    
    DBMS_OUTPUT.PUT_LINE('=== END VERIFICATION ===');
END;
/
