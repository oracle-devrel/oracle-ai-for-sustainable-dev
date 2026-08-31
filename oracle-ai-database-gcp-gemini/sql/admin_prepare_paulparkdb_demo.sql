-- Run once as ADMIN on paulparkdb before completing the FINANCIAL-owned demo.
-- These grants are deliberately limited to the three capabilities used by the
-- property-graph and Select AI agent setup.

set echo on feedback on serveroutput on
whenever sqlerror exit sql.sqlcode rollback

begin
    if sys_context('USERENV', 'SESSION_USER') <> 'ADMIN' then
        raise_application_error(-20001, 'Connect as ADMIN before running this script.');
    end if;
end;
/

grant create property graph to FINANCIAL;

-- DBMS_CLOUD is exposed through a PUBLIC synonym whose target name is
-- versioned on Autonomous Database (for example DBMS_CLOUD$PDBCS_260724_0).
-- Grant the real target instead of assuming a stable package object name.
declare
    l_owner dba_synonyms.table_owner%type;
    l_name  dba_synonyms.table_name%type;
begin
    select table_owner, table_name
      into l_owner, l_name
      from dba_synonyms
     where owner = 'PUBLIC'
       and synonym_name = 'DBMS_CLOUD';

    execute immediate
        'grant execute on ' || l_owner || '.' || l_name || ' to FINANCIAL';
end;
/

grant execute on dbms_cloud_ai to FINANCIAL;
grant execute on dbms_cloud_ai_agent to FINANCIAL;

begin
    dbms_network_acl_admin.append_host_ace(
        host => 'generativelanguage.googleapis.com',
        ace  => xs$ace_type(
            privilege_list => xs$name_list('http'),
            principal_name => 'FINANCIAL',
            principal_type => xs_acl.ptype_db
        )
    );
exception
    when others then
        if sqlcode = -24243 then
            dbms_output.put_line('SKIP: FINANCIAL already has the Google API host ACE.');
        else
            raise;
        end if;
end;
/

begin
    dbms_network_acl_admin.append_host_ace(
        host => 'api.openai.com',
        ace  => xs$ace_type(
            privilege_list => xs$name_list('http'),
            principal_name => 'FINANCIAL',
            principal_type => xs_acl.ptype_db
        )
    );
exception
    when others then
        if sqlcode = -24243 then
            dbms_output.put_line('SKIP: FINANCIAL already has the OpenAI API host ACE.');
        else
            raise;
        end if;
end;
/

prompt ADMIN preparation complete for FINANCIAL.
exit success
