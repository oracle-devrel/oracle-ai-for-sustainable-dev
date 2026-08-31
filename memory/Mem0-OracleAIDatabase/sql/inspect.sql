SET PAGESIZE 100
SET LINESIZE 220
SET LONG 100000
SET LONGCHUNKSIZE 100000

PROMPT Collection metadata
SELECT table_name
FROM   user_tables
WHERE  table_name = UPPER('&collection_name');

SELECT column_name, data_type
FROM   user_tab_columns
WHERE  table_name = UPPER('&collection_name')
ORDER  BY column_id;

PROMPT Vector indexes
SELECT index_name, index_type, status
FROM   user_indexes
WHERE  table_name = UPPER('&collection_name')
ORDER  BY index_name;

PROMPT Stored Mem0 payloads
SELECT id,
       JSON_SERIALIZE(payload RETURNING CLOB PRETTY) AS payload
FROM   &collection_name
ORDER  BY id
FETCH  FIRST 20 ROWS ONLY;

