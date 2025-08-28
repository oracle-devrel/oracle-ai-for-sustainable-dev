
BEGIN
  DBMS_CLOUD_ADMIN.ENABLE_MONGO_API;
END;
/

--  example URL format... mongodb://[user:password@]IJ1TYZIR3WPWLPE-FINANCIALDB.adb.eu-frankfurt-1.oraclecloudapps.com:27017/[user]?authMechanism=PLAIN&authSource=$external&ssl=true&retryWrites=false&loadBalanced=true

-- also see https://docs.oracle.com/en/database/oracle/oracle-database/23/jsnvu/json-relational-duality-developers-guide.pdf


--ACCOUNT_ID            NOT NULL NUMBER(19)
--ACCOUNT_BALANCE                NUMBER(19)
--CUSTOMER_ID                    VARCHAR2(255 CHAR)
--ACCOUNT_NAME                   VARCHAR2(255 CHAR)
--ACCOUNT_OPENED_DATE            TIMESTAMP(6)
--ACCOUNT_OTHER_DETAILS          VARCHAR2(255 CHAR)
--ACCOUNT_TYPE                   VARCHAR2(255 CHAR)


CREATE JSON RELATIONAL DUALITY VIEW accounts_dv AS
  SELECT JSON {
    '_id'                  : ACCOUNT_ID,
    'accountBalance'       : ACCOUNT_BALANCE,
    'customerId'           : CUSTOMER_ID,
    'accountName'          : ACCOUNT_NAME,
    'accountOpenedDate'    : ACCOUNT_OPENED_DATE,
    'accountOtherDetails'  : ACCOUNT_OTHER_DETAILS,
    'accountType'          : ACCOUNT_TYPE
  }
  FROM accounts
  WITH INSERT UPDATE DELETE CHECK;


