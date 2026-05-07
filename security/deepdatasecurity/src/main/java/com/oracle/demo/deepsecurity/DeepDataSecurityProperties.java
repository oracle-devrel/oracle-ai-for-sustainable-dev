package com.oracle.demo.deepsecurity;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "deepsec")
public class DeepDataSecurityProperties {

    private String jdbcUrl;
    private String username;
    private String password;
    private String databaseAccessToken;
    private String sql = "select employee_id || ':' || first_name || ' ' || last_name from employees fetch first 10 rows only";
    private String dataRoles = "employee_role";
    private String attributesJson = "{\"hr.hcm_context\":{\"emp_id\":3,\"service_center_id\":5}}";

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDatabaseAccessToken() {
        return databaseAccessToken;
    }

    public void setDatabaseAccessToken(String databaseAccessToken) {
        this.databaseAccessToken = databaseAccessToken;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public String getDataRoles() {
        return dataRoles;
    }

    public void setDataRoles(String dataRoles) {
        this.dataRoles = dataRoles;
    }

    public String getAttributesJson() {
        return attributesJson;
    }

    public void setAttributesJson(String attributesJson) {
        this.attributesJson = attributesJson;
    }
}
