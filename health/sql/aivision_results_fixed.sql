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

BEGIN
    EXECUTE IMMEDIATE 'DROP FUNCTION VISIONAI_RESULTS_TEXT_SEARCH';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -4043 THEN -- Function doesn't exist
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

-- Utility queries to check the index (commented out)
-- select index_name, index_type, status from user_indexes where index_name = 'AIVISIONRESULTSINDEX';
-- select idx_name, idx_table, idx_text_name from ctx_user_indexes;
-- select token_text from dr$aivisionresultsindex$i;

-- Create the search function
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

-- Add some sample data for testing (optional)
INSERT INTO aivision_results (id, date_loaded, label, textfromai, jsondata)
VALUES (
    SYS_GUID(),
    SYSTIMESTAMP,
    'sample',
    'This is sample text from AI vision analysis containing medical data and health information',
    '{"confidence": 0.95, "objects": ["medical_equipment", "patient"], "analysis": "sample analysis"}'
);
/

INSERT INTO aivision_results (id, date_loaded, label, textfromai, jsondata)
VALUES (
    SYS_GUID(),
    SYSTIMESTAMP,
    'test',
    'Another sample text with cancer research data and treatment information',
    '{"confidence": 0.87, "objects": ["xray", "tumor"], "analysis": "tumor detection analysis"}'
);
/

COMMIT;
/

-- Test the function
SELECT * FROM TABLE(VISIONAI_RESULTS_TEXT_SEARCH('medical'));
/

SELECT * FROM TABLE(VISIONAI_RESULTS_TEXT_SEARCH('cancer'));
/

-- Show table structure and data
DESCRIBE aivision_results;
/

SELECT COUNT(*) as total_records FROM aivision_results;
/

SELECT id, label, SUBSTR(textfromai, 1, 50) as text_preview 
FROM aivision_results 
ORDER BY date_loaded DESC;
/
