SET PAGESIZE 100
SET LINESIZE 180

SELECT SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AS current_schema,
       SYS_CONTEXT('USERENV', 'DB_NAME') AS database_name
FROM dual;

SELECT model_name,
       mining_function,
       algorithm
FROM user_mining_models
WHERE UPPER(model_name) = UPPER('allminilm');

SELECT table_name
FROM user_tables
WHERE table_name LIKE 'OAMJ_TEST_%'
ORDER BY table_name;
