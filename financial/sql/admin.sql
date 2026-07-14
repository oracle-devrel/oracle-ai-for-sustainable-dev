-- USER SQL
CREATE USER FINANCIAL IDENTIFIED BY Welcome12345;

-- ADD ROLES
GRANT CONNECT TO FINANCIAL;
GRANT CONSOLE_DEVELOPER TO FINANCIAL;
GRANT DWROLE TO FINANCIAL;
GRANT GRAPH_DEVELOPER TO FINANCIAL;
GRANT OML_DEVELOPER TO FINANCIAL;
GRANT RESOURCE TO FINANCIAL;
ALTER USER FINANCIAL DEFAULT ROLE CONSOLE_DEVELOPER, DWROLE, GRAPH_DEVELOPER, OML_DEVELOPER;

--For TxEventQ/OKafka
grant resource, connect, unlimited tablespace to testuser;
grant aq_user_role to FINANCIAL;
grant execute on dbms_aq to  FINANCIAL;
grant execute on dbms_aqadm to FINANCIAL;
grant select on gv_$session to FINANCIAL;
grant select on v_$session to FINANCIAL;
grant select on gv_$instance to FINANCIAL;
grant select on gv_$listener_network to FINANCIAL;
grant select on sys.dba_rsrc_plan_directives to FINANCIAL;
grant select on gv_$pdbs to FINANCIAL;
grant select on user_queue_partition_assignment_table to FINANCIAL;
exec dbms_aqadm.grant_priv_for_rm_plan('FINANCIAL');
commit;

--GRANT AQ_USER_ROLE to FINANCIAL;
--GRANT CONNECT, RESOURCE, unlimited tablespace to FINANCIAL;
--GRANT EXECUTE on DBMS_AQ to FINANCIAL;
--GRANT EXECUTE on DBMS_AQADM to FINANCIAL;
--GRANT EXECUTE on DBMS_AQIN to FINANCIAL;
--GRANT EXECUTE on DBMS_TEQK to FINANCIAL;
--GRANT SELECT on GV$SESSION to FINANCIAL;
--GRANT SELECT on V$SESSION to FINANCIAL;
--GRANT SELECT on GV$INSTANCE to FINANCIAL;
--GRANT SELECT on GV$LISTENER_NETWORK to FINANCIAL;
--GRANT SELECT on GV$PDBS to FINANCIAL;
--GRANT SELECT on SYS.DBA_RSRC_PLAN_DIRECTIVES to FINANCIAL;
--EXEC DBMS_AQADM.GRANT_PRIV_FOR_RM_PLAN('FINANCIAL');


--For TxEventQ/OKafka on cloud
grant resource, connect, unlimited tablespace to FINANCIAL;
grant aq_user_role to FINANCIAL;
grant execute on dbms_aq to  FINANCIAL;
grant execute on dbms_aqadm to FINANCIAL;
exec dbms_aqadm.GRANT_PRIV_FOR_RM_PLAN('FINANCIAL');
commit;

-- REST ENABLE
BEGIN
    ORDS_ADMIN.ENABLE_SCHEMA(
        p_enabled => TRUE,
        p_schema => 'FINANCIAL',
        p_url_mapping_type => 'BASE_PATH',
        p_url_mapping_pattern => 'financial',
        p_auto_rest_auth => TRUE
    );
END;
/
-- ENABLE DATA SHARING
BEGIN
    C##ADP$SERVICE.DBMS_SHARE.ENABLE_SCHEMA(
        SCHEMA_NAME => 'FINANCIAL',
        ENABLED => TRUE
    );
END;
/

-- ENABLE GRAPH
ALTER USER FINANCIAL GRANT CONNECT THROUGH GRAPH$PROXY_USER;

-- ENABLE OML
ALTER USER FINANCIAL GRANT CONNECT THROUGH OML$PROXY;

-- QUOTA
ALTER USER FINANCIAL QUOTA UNLIMITED ON DATA;


