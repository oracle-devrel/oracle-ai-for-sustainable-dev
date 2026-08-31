-- Create the OpenAI credential used by the parallel Select AI profile.
-- Connect as FINANCIAL and pass the API key as the first script parameter.
-- Callers should feed the invocation through SQLcl standard input so the key
-- is not exposed in the SQLcl process arguments.

set echo off verify off feedback on serveroutput on
whenever sqlerror exit sql.sqlcode rollback

define openai_api_key = '&1'

declare
    l_count number;
begin
    if sys_context('USERENV', 'SESSION_USER') <> 'FINANCIAL' then
        raise_application_error(-20001, 'Connect as FINANCIAL before running this script.');
    end if;

    select count(*)
      into l_count
      from user_credentials
     where credential_name = 'OPENAI_CRED';

    if l_count > 0 then
        raise_application_error(
            -20002,
            'Credential OPENAI_CRED already exists. Audit or explicitly drop it before replacement.'
        );
    end if;

    dbms_cloud.create_credential(
        credential_name => 'OPENAI_CRED',
        username        => 'OPENAI',
        password        => '&openai_api_key'
    );

    dbms_output.put_line('Created FINANCIAL.OPENAI_CRED');
end;
/

undefine openai_api_key
exit success
