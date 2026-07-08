-- Helper queries for checking the SQL objects a lakehouse demo user can see.
-- Run as the same database user configured in .env.

set linesize 200
set pagesize 100

column owner format a30
column object_name format a40
column object_type format a20
column table_name format a40
column column_name format a40
column data_type format a30

select owner, object_name, object_type
  from all_objects
 where object_type in ('TABLE', 'VIEW', 'MATERIALIZED VIEW')
   and (upper(object_name) like '%ICEBERG%' or upper(object_name) like '%LAKE%')
 order by owner, object_type, object_name;

prompt
prompt Replace the table name below with your configured ICEBERG_TABLE_NAME:
prompt

select owner, table_name, column_name, data_type, nullable
  from all_tab_columns
 where owner = upper(regexp_substr('&ICEBERG_TABLE_NAME', '^[^.]+'))
   and table_name = upper(regexp_substr('&ICEBERG_TABLE_NAME', '[^.]+$'))
 order by column_id;
