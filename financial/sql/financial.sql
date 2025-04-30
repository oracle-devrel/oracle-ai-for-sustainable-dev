-- USER SQL
CREATE USER FINANCIAL IDENTIFIED BY Welcome12345;

-- ADD ROLES
GRANT CONNECT TO FINANCIAL;
GRANT CONSOLE_DEVELOPER TO FINANCIAL;
GRANT DWROLE TO FINANCIAL;
GRANT GRAPH_DEVELOPER TO FINANCIAL;
GRANT OML_DEVELOPER TO FINANCIAL;
GRANT RESOURCE TO FINANCIAL;
ALTER USER FINANCIAL DEFAULT ROLE CONSOLE_DEVELOPER,DWROLE,GRAPH_DEVELOPER,OML_DEVELOPER;

-- REST ENABLE
BEGIN
    ORDS_ADMIN.ENABLE_SCHEMA(
        p_enabled => TRUE,
        p_schema => 'FINANCIAL',
        p_url_mapping_type => 'BASE_PATH',
        p_url_mapping_pattern => 'financial',
        p_auto_rest_auth=> TRUE
    );
    -- ENABLE DATA SHARING
    C##ADP$SERVICE.DBMS_SHARE.ENABLE_SCHEMA(
            SCHEMA_NAME => 'FINANCIAL',
            ENABLED => TRUE
    );
    commit;
END;
/

-- ENABLE GRAPH
ALTER USER FINANCIAL GRANT CONNECT THROUGH GRAPH$PROXY_USER;

-- ENABLE OML
ALTER USER FINANCIAL GRANT CONNECT THROUGH OML$PROXY;

-- QUOTA
ALTER USER FINANCIAL QUOTA UNLIMITED ON DATA;



BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE accounts CASCADE CONSTRAINTS';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -942 THEN
            RAISE;
        END IF;
END;
/

-- 1. Accounts Table
CREATE TABLE accounts (
    account_id         VARCHAR2(64) PRIMARY KEY,
    name               VARCHAR2(255),
    official_name      VARCHAR2(255),
    type               VARCHAR2(50),
    subtype            VARCHAR2(50),
    mask               VARCHAR2(10),
    available_balance  NUMBER(18, 2),
    current_balance    NUMBER(18, 2),
    limit_balance      NUMBER(18, 2),
    verification_status VARCHAR2(50)
);

INSERT INTO accounts (
    account_id,
    name,
    official_name,
    type,
    subtype,
    mask,
    available_balance,
    current_balance,
    limit_balance,
    verification_status
) VALUES (
    'acc_1001',
    'Checking',
    'Gold Standard 0% Interest Checking',
    'depository',
    'checking',
    '1234',
    1200.50,
    1250.00,
    NULL,
    'verified'
);

INSERT INTO accounts (
    account_id,
    name,
    official_name,
    type,
    subtype,
    mask,
    available_balance,
    current_balance,
    limit_balance,
    verification_status
) VALUES (
    'acc_1002',
    'Savings',
    'Premier High-Yield Savings',
    'depository',
    'savings',
    '5678',
    5000.00,
    5000.00,
    NULL,
    'verified'
);

INSERT INTO accounts (
    account_id,
    name,
    official_name,
    type,
    subtype,
    mask,
    available_balance,
    current_balance,
    limit_balance,
    verification_status
) VALUES (
    'acc_1003',
    'Credit Card',
    'Diamond Elite Credit Card',
    'credit',
    'credit card',
    '9012',
    NULL,
    200.00,
    5000.00,
    'pending_automatic_verification'
);