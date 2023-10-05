CREATE OR REPLACE PROCEDURE openai_call AS
  ctx DBMS_MLE.CONTEXT_HANDLE_T;
  SNIPPET CLOB;
BEGIN
  ctx := DBMS_MLE.CREATE_CONTEXT();
  SNIPPET := q'~
  (async () => {
   await import('mle-js-fetch');
   const oracledb = require("mle-js-oracledb");
   const conn = oracledb.defaultConnection();
       for (var row of conn.execute("select conversation_id, name, dialogue from interlocutor where dialogue IS NOT NULL").rows) {
          const fetchUrl = "http://192.168.205.1:8080/databasejs/getreply?textcontent=" + row[2];
          const answer = await fetch(fetchUrl).then(response => response.json());
          const dialogue = JSON.stringify(answer, undefined, 4);
          conn.execute( "update conversation_dv set dialogue = :dialogue ", {dialogue: dialogue} );
       };
  })();
  ~';
  DBMS_MLE.EVAL(ctx, 'JAVASCRIPT', SNIPPET, 'js_mode=module');
  DBMS_MLE.DROP_CONTEXT(ctx);
EXCEPTION
  WHEN OTHERS THEN
    DBMS_OUTPUT.PUT_LINE(SQLERRM);
END;
/