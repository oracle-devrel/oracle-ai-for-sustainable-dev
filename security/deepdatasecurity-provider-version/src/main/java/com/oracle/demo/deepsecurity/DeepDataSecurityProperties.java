package com.oracle.demo.deepsecurity;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "deepsec")
public class DeepDataSecurityProperties {

    private String sql = "select employee_id || ':' || first_name || ' ' || last_name from hr.employees fetch first 10 rows only";
    private String sessionInitSql = "alter session disable parallel query";
    private Browser browser = new Browser();

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

    public Browser getBrowser() {
        return browser;
    }

    public void setBrowser(Browser browser) {
        this.browser = browser;
    }

    public static class Browser {

        private String tenantId = "";
        private String clientId = "";
        private String scope = "";

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

        public String getScope() {
            return scope;
        }

        public void setScope(String scope) {
            this.scope = scope;
        }
    }
}
