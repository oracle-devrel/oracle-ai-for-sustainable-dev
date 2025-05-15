/*
Copyright (c) 2023, Oracle and/or its affiliates. **

The Universal Permissive License (UPL), Version 1.0 **

Subject to the condition set forth below, permission is hereby granted to any person obtaining a copy of this software, associated documentation and/or data
(collectively the "Software"), free of charge and under any and all copyright rights in the Software, and any and all patent rights owned or freely licensable by each
licensor hereunder covering either the unmodified Software as contributed to or provided by such licensor, or (ii) the Larger Works (as defined below), to deal in both **
(a) the Software, and (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if one is included with the Software (each a "Larger Work" to which the
Software is contributed by such licensors), **
without restriction, including without limitation the rights to copy, create derivative works of, display, perform, and distribute the Software and make, use, sell,
offer for sale, import, export, have made, and have sold the Software and the Larger Work(s), and to sublicense the foregoing rights on either these or other terms. **

This license is subject to the following condition: The above copyright notice and either this complete permission notice or at a minimum a reference to the UPL must be
included in all copies or substantial portions of the Software. **

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package com.example.hotelsb;

import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.sql.SQLException;

@Configuration
@ComponentScan("com.oracle")
public class DataSourceConfig {
    @Value("${departmentDataSource.url}")
    private String url;
    @Value("${departmentDataSource.user}")
    private String username;
    @Value("${departmentDataSource.password}")
    private String password;
    @Value("${departmentDataSource.oracleucp.min-pool-size}")
    private String minPoolSize;
    @Value("${departmentDataSource.oracleucp.initial-pool-size:10}")
    private String initialPoolSize;

    @Value("${departmentDataSource.oracleucp.max-pool-size}")
    private String maxPoolSize;

    @Value("${departmentDataSource.oracleucp.data-source-name}")
    private String dataSourceName;

    @Value("${departmentDataSource.oracleucp.connection-pool-name}")
    private String connectionPoolName;

    @Value("${departmentDataSource.oracleucp.connection-factory-class-name}")
    private String connectionFactoryClassName;

    private static final Logger logger = LoggerFactory.getLogger(DataSourceConfig.class);

    @Bean(name = "ucpDataSource")
    @Primary
    public DataSource getDataSource() {
        PoolDataSource pds = null;
        try {
            pds = PoolDataSourceFactory.getPoolDataSource();

            pds.setConnectionFactoryClassName(connectionFactoryClassName);
            pds.setURL(url);
            pds.setUser(username);
            pds.setPassword(password);
            pds.setMinPoolSize(Integer.valueOf(minPoolSize));
            pds.setInitialPoolSize(Integer.valueOf(initialPoolSize));
            pds.setMaxPoolSize(Integer.valueOf(maxPoolSize));

            pds.setDataSourceName(dataSourceName);
            pds.setConnectionPoolName(connectionPoolName);

            logger.info("DataSourceConfig: ucpDataSource created");
        } catch (SQLException ex) {
            logger.error("Error connecting to the database: " + ex.getMessage());
        }
        return pds;
    }
}
