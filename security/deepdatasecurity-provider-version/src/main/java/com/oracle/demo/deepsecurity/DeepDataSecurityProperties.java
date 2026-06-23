package com.oracle.demo.deepsecurity;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "deepsec")
public class DeepDataSecurityProperties {

    private String sql = "select employee_id || ':' || first_name || ' ' || last_name from hr.employees fetch first 10 rows only";

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }
}
