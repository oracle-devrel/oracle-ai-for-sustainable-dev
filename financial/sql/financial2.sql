
-- DROP TABLE IF EXISTS
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE accounts CASCADE CONSTRAINTS';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -942 THEN
            RAISE;
        END IF;
END;
/

-- CREATE ACCOUNTS TABLE
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

-- INSERT DATA INTO ACCOUNTS TABLE
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
    'acc_2001',
    'Checking',
    'BankB Checking',
    'depository',
    'checking',
    '1234',
    1200.50,
    1250.00,
    NULL,
    'verified'
);


COMMIT;
/

BEGIN
    ORDS.ENABLE_OBJECT(
        P_ENABLED      => TRUE,
        P_SCHEMA      => 'FINANCIAL2',
        P_OBJECT      =>  'ACCOUNTS',
        P_OBJECT_TYPE      => 'TABLE',
        P_OBJECT_ALIAS      => 'accounts',
        P_AUTO_REST_AUTH      => FALSE
    );
    COMMIT;
END;

