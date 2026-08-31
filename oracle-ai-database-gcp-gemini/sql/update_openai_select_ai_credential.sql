-- Rotate the API key stored in FINANCIAL.OPENAI_CRED without recreating the
-- Select AI profile. Connect as FINANCIAL and pass the new key as the first
-- script parameter through SQLcl standard input.

set echo off verify off feedback on serveroutput on
whenever sqlerror exit sql.sqlcode rollback

define replacement_key = '&1'

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

    if l_count = 0 then
        raise_application_error(-20002, 'Credential OPENAI_CRED does not exist.');
    end if;

    dbms_cloud.update_credential(
        credential_name => 'OPENAI_CRED',
        attribute       => 'PASSWORD',
        value           => '&replacement_key'
    );

    dbms_output.put_line('Updated FINANCIAL.OPENAI_CRED');
end;
/

undefine replacement_key
exit success
