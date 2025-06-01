
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
CREATE TABLE "FINANCIAL"."ACCOUNTS"
   (	"ACCOUNT_ID" NUMBER(19,0) ,
	"ACCOUNT_BALANCE" NUMBER(19,0) RESERVABLE,
	"CUSTOMER_ID" VARCHAR2(255 CHAR),
	"ACCOUNT_NAME" VARCHAR2(255 CHAR),
	"ACCOUNT_OPENED_DATE" TIMESTAMP (6),
	"ACCOUNT_OTHER_DETAILS" VARCHAR2(255 CHAR),
	"ACCOUNT_TYPE" VARCHAR2(255 CHAR)
   ) ;

CREATE TABLE TRANSFERS (
"TXN_ID" NUMBER,
"SRC_ACCT_ID" NUMBER,
"DST_ACCT_ID" NUMBER,
"DESCRIPTION" VARCHAR2(400),
"AMOUNT" NUMBER,
PRIMARY KEY ("TXN_ID"),
FOREIGN KEY ("SRC_ACCT_ID") REFERENCES "ACCOUNTS" ("ACCOUNT_ID"),
FOREIGN KEY ("DST_ACCT_ID") REFERENCES "ACCOUNTS" ("ACCOUNT_ID")
);

CREATE SEQUENCE TRANSFERS_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

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


BEGIN
  DBMS_CLOUD_ADMIN.ENABLE_MONGO_API;
END;
/

mongodb://[user:password@]IJ1TYZIR3WPWLPE-FINANCIALDB.adb.eu-frankfurt-1.oraclecloudapps.com:27017/[user]?authMechanism=PLAIN&authSource=$external&ssl=true&retryWrites=false&loadBalanced=true




--From https://docs.oracle.com/en/database/oracle/oracle-database/23/jsnvu/json-relational-duality-developers-guide.pdf


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


