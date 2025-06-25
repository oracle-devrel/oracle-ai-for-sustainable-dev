
--VARIATIONS OF ENABLEMENT OF reservable ON ACCOUNT_BALANCE


ALTER TABLE FINANCIAL.ACCOUNTS
MODIFY ACCOUNT_BALANCE reservable;


ALTER TABLE FINANCIAL.ACCOUNTS
MODIFY ACCOUNT_BALANCE reservable DEFAULT 0
CONSTRAINT chk_balance_nonnegative CHECK (ACCOUNT_BALANCE >= 0);

CREATE TABLE "FINANCIAL"."ACCOUNTS"
(
    "ACCOUNT_ID" NUMBER(19,0) NOT NULL ENABLE,
    "ACCOUNT_BALANCE" NUMBER(19,0) reservable,
    "CUSTOMER_ID" VARCHAR2(255 CHAR),
    "ACCOUNT_NAME" VARCHAR2(255 CHAR),
    "ACCOUNT_OPENED_DATE" TIMESTAMP (6),
    "ACCOUNT_OTHER_DETAILS" VARCHAR2(255 CHAR),
    "ACCOUNT_TYPE" VARCHAR2(255 CHAR)
);

--with check balance constraint...
CREATE TABLE "FINANCIAL"."ACCOUNTS"
(
    "ACCOUNT_ID" NUMBER(19,0) NOT NULL ENABLE,
    "ACCOUNT_BALANCE" NUMBER(19,0) reservable
        CONSTRAINT chk_account_balance_nonnegative CHECK (ACCOUNT_BALANCE >= 0),
    "CUSTOMER_ID" VARCHAR2(255 CHAR),
    "ACCOUNT_NAME" VARCHAR2(255 CHAR),
    "ACCOUNT_OPENED_DATE" TIMESTAMP (6),
    "ACCOUNT_OTHER_DETAILS" VARCHAR2(255 CHAR),
    "ACCOUNT_TYPE" VARCHAR2(255 CHAR)
);

ALTER TABLE FINANCIAL.ACCOUNTS MODIFY ACCOUNT_BALANCE NOT reservable;

--Note, if you are using JPA you may get this for saves/udpates...
--[ORA-55735: Reservable and non-reservable columns cannot be updated in the same statement.
--] [update accounts set account_balance=?,customer_id=?,account_name=?,account_opened_date=?,account_other_details=?,account_type=? where account_id=?]
--org.springframework.orm.jpa.JpaSystemException: could not execute statement [ORA-55735: Reservable and non-reservable columns cannot be updated in the same statement.
--In which case you will need to instead do this...
-- entityManager.createNativeQuery("UPDATE accounts SET account_balance = account_balance - ? WHERE account_id = ?" )
