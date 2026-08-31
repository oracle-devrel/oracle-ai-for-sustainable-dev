-- Database-side authorization used by both the local demonstration and the
-- separately permissioned Gemini Enterprise A2A agents. The deployed full and
-- environmental endpoints fix their database profiles in server configuration;
-- prompt text and A2UI action payloads cannot select a database identity.
-- Replace <DB_USERNAME2> and <DB_PASSWORD2> or run agent-service/setup-deepsec.sh.
CREATE ROLE inventory_deepsec_login_role;
GRANT CREATE SESSION TO inventory_deepsec_login_role;

CREATE DATA ROLE IF NOT EXISTS inventory_environmental_role;
GRANT inventory_deepsec_login_role TO inventory_environmental_role;

CREATE END USER IF NOT EXISTS <DB_USERNAME2> IDENTIFIED BY "<DB_PASSWORD2>";
GRANT DATA ROLE inventory_environmental_role TO <DB_USERNAME2>;

CREATE OR REPLACE DATA GRANT financial.inventory_environmental_read
  AS SELECT
  ON financial.stockout_transfer_recommendation_v
  WHERE category_name = 'Environmental Monitoring'
  TO inventory_environmental_role;

SET USE DATA GRANTS ONLY
  ON financial.stockout_transfer_recommendation_v ENABLED;

-- Verification when connected as <DB_USERNAME2>. This must return exactly one
-- row whose value is Environmental Monitoring.
SELECT DISTINCT category_name
  FROM financial.stockout_transfer_recommendation_v
 ORDER BY category_name;
