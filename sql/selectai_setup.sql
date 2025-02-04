
-- Create user with privs
SET DEFINE OFF;
CREATE USER SELECT_AI_USER IDENTIFIED BY "Oracle##2025AI";
GRANT RESOURCE TO SELECT_AI_USER;
GRANT CREATE SESSION TO SELECT_AI_USER;
GRANT CREATE VIEW TO SELECT_AI_USER;
GRANT CREATE TABLE TO SELECT_AI_USER;
GRANT CONNECT TO SELECT_AI_USER;
GRANT ALTER SYSTEM TO SELECT_AI_USER;
GRANT ALTER USER TO SELECT_AI_USER;
ALTER USER SELECT_AI_USER QUOTA 10M ON temp;
GRANT CONSOLE_DEVELOPER TO SELECT_AI_USER;
GRANT DWROLE TO SELECT_AI_USER;
GRANT EXECUTE ON DBMS_CLOUD TO SELECT_AI_USER;
GRANT EXECUTE ON DBMS_CLOUD_AI TO SELECT_AI_USER;


-- Generate and obtain a key from OCI Generative AI, Google Gemini, Azure OpenAI, Anthropic, ...
BEGIN
  DBMS_CLOUD.create_credential(
    credential_name => 'OCI_GENERATIVE_AI_CRED’,
    user_ocid       => '<UserOCID>',
    tenancy_ocid    => '<TenancyOCID>',
    fingerprint     => '<Fingerprint>',
    private_key     => '<PrivateKey>'
  );
END;
/

-- Generate and Select AI (NL2SQL) profile....

BEGIN
    dbms_cloud_ai.create_profile(
        profile_name => 'MOVIES_AND_GAMES',
        attributes =>
            '{"provider": "ocigenai",
            "credential_name": "OCI_GENERATIVE_AI_CRED",
            "comments": "true",
            "object_list": [
                {"owner": "MOVIESTREAM", "name": "MOVIES"},
                {"owner": "MOVIESTREAM", "name": "VIDEOGAMES"}
             ]}'
    );
END;
/

-- Optionally comment on tables, fields, ... to provide additional meta to Select AI / LLM'
COMMENT ON TABLE VIDEOGAMES IS 'Video game information including genre, developer, sales, etc. information';


-- Generate and Select AI (NL2SQL) profile for V....

BEGIN
  DBMS_CLOUD_AI.CREATE_PROFILE(
    profile_name => 'OPENAI_TEXT_TRANSFORMER',
    attributes   => '{"provider": "openai",
                      "credential_name": "OPENAI_CRED",
                      "embedding_model": "text-embedding-ada-002" }');
END;

BEGIN
  DBMS_CLOUD_AI.CREATE_VECTOR_INDEX(
    index_name  => 'MY_VECTOR_INDEX',
    attributes  => '{"vector_db_provider": "oracle",
                     "location": "https:.../my_namespace/my_bucket/my_data_folder",
                     "object_storage_credential_name": "OCI_CRED",
                     "profile_name": "OPENAI_TEXT_TRANSFORMER",
                     "vector_dimension": 1536,
                     "vector_distance_metric": "cosine",
                     "chunk_overlap":128,
                     "chunk_size":1024}');
END;

BEGIN
  DBMS_CLOUD_AI.CREATE_PROFILE(
    profile_name => 'OPENAI_GPT',
    attributes   => '{"provider": "openai",
                      "credential_name": "OPENAI_CRED",
                      "vector_index_name": "MY_VECTOR_INDEX",
                      "temperature": 0.2,
                      "max_tokens": 4096,
                      "model": "gpt-3.5-turbo",
                      "embedding_model": "text-embedding-ada-002",
                      "enable_sources": true }');
END;