
login as sysdba


DROP USER IF EXISTS transactions CASCADE ;

CREATE USER transactions identified BY WElcome_#12;

GRANT CONNECT, RESOURCE    TO transactions;

GRANT SELECT_CATALOG_ROLE  TO transactions;

GRANT UNLIMITED TABLESPACE TO transactions;

GRANT ALL PRIVILEGES       TO transactions;

GRANT DBA                  TO transactions;




--login as transactions

rem
rem Connect to pdb as transactions
rem

CREATE OR REPLACE FUNCTION country_cd_from_account_id(p_account_id IN NUMBER) RETURN VARCHAR2
IS

BEGIN

  RETURN
    CASE
      WHEN p_account_id BETWEEN        0 AND  1999999 THEN 'AW'
      WHEN p_account_id BETWEEN  2000000 AND  3999999 THEN 'CA'
      WHEN p_account_id BETWEEN  4000000 AND  5999999 THEN 'CR'
      WHEN p_account_id BETWEEN  6000000 AND  7999999 THEN 'HT'
      WHEN p_account_id BETWEEN  8000000 AND  9999999 THEN 'MX'
      WHEN p_account_id BETWEEN 10000000 AND 11999999 THEN 'US'
      WHEN p_account_id BETWEEN 12000000 AND 13999999 THEN 'AR'
      WHEN p_account_id BETWEEN 14000000 AND 15999999 THEN 'BO'
      WHEN p_account_id BETWEEN 16000000 AND 17999999 THEN 'BR'
      WHEN p_account_id BETWEEN 18000000 AND 19999999 THEN 'CL'
      WHEN p_account_id BETWEEN 20000000 AND 21999999 THEN 'CO'
      WHEN p_account_id BETWEEN 22000000 AND 23999999 THEN 'VE'
    ELSE
      'US'
    END;

END country_cd_from_account_id;







rem
rem Connect to pdb as transactions
rem

DROP TABLE IF EXISTS accounts;
DROP TABLE IF EXISTS payments;

DROP SEQUENCE IF EXISTS payment_id_seq ;

rem ####################################################################################################################

CREATE SEQUENCE payment_id_seq;

rem ####################################################################################################################
rem accounts

CREATE TABLE accounts
(
 country_cd        VARCHAR2(10) NOT NULL
,account_id        NUMBER(38,0) NOT NULL
,user_id           NUMBER(38,0) NOT NULL
,balance           NUMBER       NOT NULL
,last_modified_utc TIMESTAMP    NOT NULL
,CONSTRAINT accounts_pk PRIMARY KEY (account_id)
);

rem ####################################################################################################################
rem payments

CREATE TABLE PAYMENTS
(
 id          NUMBER(38,0) NOT NULL
,country_cd  VARCHAR2(10) NOT NULL
,account_id  NUMBER(38,0) NOT NULL
,amount      NUMBER       NOT NULL
,created_utc TIMESTAMP    NOT NULL
,CONSTRAINT payments_pk PRIMARY KEY (id)
,CONSTRAINT payments_uk UNIQUE      (account_id, created_utc)
);












rem
rem Connect to pdb as transactions
rem

set autocommit on timing on verify off

TRUNCATE TABLE transactions.accounts;

rem AW
define initial_value =        0
define final_value   =  1999999

INSERT INTO transactions.accounts
SELECT transactions.country_cd_from_account_id(n)
      ,n
      ,n
      ,10000
      ,SYS_EXTRACT_UTC(SYSTIMESTAMP)
FROM   (
        SELECT &&initial_value + LEVEL - 1 AS n
        FROM dual
        CONNECT BY LEVEL <= &&final_value - &&initial_value + 1
       );

rem CA
define initial_value =  2000000
define final_value   =  3999999
run

rem CR
define initial_value =  4000000
define final_value   =  5999999
run

rem HT
define initial_value =  6000000
define final_value   =  7999999
run

rem MX
define initial_value =  8000000
define final_value   =  9999999
run

rem US
define initial_value = 10000000
define final_value   = 11999999
run

rem AR
define initial_value = 12000000
define final_value   = 13999999
run

rem BO
define initial_value = 14000000
define final_value   = 15999999
run

rem BR
define initial_value = 16000000
define final_value   = 17999999
run

rem CL
define initial_value = 18000000
define final_value   = 19999999
run

rem CO
define initial_value = 20000000
define final_value   = 21999999
run

rem VE
define initial_value = 22000000
define final_value   = 23999999
run

EXECUTE DBMS_STATS.GATHER_SCHEMA_STATS(ownname => 'TRANSACTIONS');









--
--Now loging to truecache instance as trasnactions user..
--
--sqlplus transactions/<PASSWORDFROMSTEP1*>@truedb:1521/SALES1_TC


SELECT DATABASE_ROLE FROM V$DATABASE;

--should be database role of true cache


