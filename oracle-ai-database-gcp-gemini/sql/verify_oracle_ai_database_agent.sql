set serveroutput on size unlimited
set verify off

declare
  l_conversation_id varchar2(128);
  l_response        clob;
begin
  l_conversation_id := dbms_cloud_ai.create_conversation(
    attributes => '{"title":"Supply-chain managed-agent verification","retention_days":1,"conversation_length":4}'
  );

  l_response := dbms_cloud_ai_agent.run_team(
    team_name   => 'ORACLE_AI_DATABASE_AGENT',
    user_prompt => 'Using only the Oracle inventory risk demo tables, list the top products at risk of stockouts next quarter, including stockout probability, projected revenue impact, and primary region.',
    params      => json_object('conversation_id' value l_conversation_id)
  );

  dbms_output.put_line(dbms_lob.substr(l_response, 16000, 1));
end;
/

select agent_team_name, status
from user_ai_agent_teams
where agent_team_name = 'ORACLE_AI_DATABASE_AGENT';

select tool_name
from user_ai_agent_tools
where tool_name in (
  'SQL_TOOL',
  'DISTINCT_VALUES_CHECK',
  'RANGE_VALUES_CHECK',
  'GENERATE_CHART'
)
order by tool_name;
