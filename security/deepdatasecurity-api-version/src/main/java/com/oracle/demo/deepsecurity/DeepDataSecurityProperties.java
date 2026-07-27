package com.oracle.demo.deepsecurity;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "deepsec")
public class DeepDataSecurityProperties {

    private String sql = "select employee_id || ':' || first_name || ' ' || last_name from hr.employees fetch first 10 rows only";
    private String sessionInitSql = "alter session disable parallel query";
    private String attributesJson = "";
    private EntraId entraId = new EntraId();

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public String getSessionInitSql() {
        return sessionInitSql;
    }

    public void setSessionInitSql(String sessionInitSql) {
        this.sessionInitSql = sessionInitSql;
    }

    public String getAttributesJson() {
        return attributesJson;
    }

    public void setAttributesJson(String attributesJson) {
        this.attributesJson = attributesJson;
    }

    public EntraId getEntraId() {
        return entraId;
    }

    public void setEntraId(EntraId entraId) {
        this.entraId = entraId;
    }

    public static class EntraId {

        private String tenantId;
        private String clientId;
        private String clientSecret;
        private String databaseScope;

        public String getTenantId() {
            return tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getDatabaseScope() {
            return databaseScope;
        }

        public void setDatabaseScope(String databaseScope) {
            this.databaseScope = databaseScope;
        }
    }
}
