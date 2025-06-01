
BEGIN
    ORDS.ENABLE_OBJECT(
        P_ENABLED      => TRUE,
        P_SCHEMA      => 'FINANCIAL',
        P_OBJECT      =>  'account_detail',
        P_OBJECT_TYPE      => 'TABLE',
        P_OBJECT_ALIAS      => 'accounts',
        P_AUTO_REST_AUTH      => FALSE
    );
    COMMIT;
END;

--The full REST endpoint URL is typically built as:
--https://<ords-host>/ords/<base_path>/<module_name>/<template_path>

SELECT
  'https://<your-ords-host>/ords/' || m.uri_prefix || '/' || m.uri_template AS endpoint_url,
  m.module_name,
  t.uri_template,
  h.method,
  h.source_type,
  h.items_source
FROM
  user_ords_modules m
JOIN
  user_ords_templates t ON m.module_id = t.module_id
JOIN
  user_ords_handlers h ON t.template_id = h.template_id
ORDER BY
  m.module_name, t.uri_template, h.method;


BEGIN
  ords.enable_schema(
    p_enabled => TRUE,
    p_schema => 'FINANCIAL',
    p_url_mapping_type => 'BASE_PATH',
    p_url_mapping_pattern => 'hr'
  );
END;