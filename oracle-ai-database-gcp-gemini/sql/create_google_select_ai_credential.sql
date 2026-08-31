-- Create the Google AI Studio credential used by Select AI.
-- Connect as FINANCIAL. SQLcl prompts without echoing the API key.

set echo off verify off feedback on serveroutput on
whenever sqlerror exit sql.sqlcode rollback

accept google_api_key char prompt 'Google AI Studio API key: ' hide

begin
    if sys_context('USERENV', 'SESSION_USER') <> 'FINANCIAL' then
        raise_application_error(-20001, 'Connect as FINANCIAL before running this script.');
    end if;

    dbms_cloud.create_credential(
        credential_name => 'GOOGLE_AI_CRED',
        username        => 'GOOGLE',
        password        => '&google_api_key'
    );

    dbms_output.put_line('Created FINANCIAL.GOOGLE_AI_CRED');
end;
/

undefine google_api_key
exit success
