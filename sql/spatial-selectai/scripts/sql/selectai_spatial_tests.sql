--
-- Author: Karin Patenge
-- Date: Jan 28, 2025
--

-- General remarks:
--   The script requires an Autonomous Database 23ai.
--   The spatial datasets belong to the DB user SPATIALUSER.
--   The application user for the tests with SELECT AI is APEXUSER.

set serveroutput on

--
-- Grant privileges for datasets
--   ToDo: Replace the DB schema/users references according to match your Autonomous DB schemas/users.
--

-- Grant SELECT privilege to APEXUSER on SPATIALUSER objects
grant select on SPATIALUSER.US_COUNTIES to APEXUSER;
grant select on SPATIALUSER.US_HOSPITALS to APEXUSER;
grant select on SPATIALUSER.US_CITIES to APEXUSER;
grant select on SPATIALUSER.US_AIRPORTS to APEXUSER;
grant select on SPATIALUSER.US_STATES to APEXUSER;
grant select on SPATIALUSER.USGS_EARTHQUAKES to APEXUSER;

-- Grants EXECUTE privilege to the application user
grant execute on DBMS_CLOUD to APEXUSER;
grant execute on DBMS_CLOUD_AI to APEXUSER;

--
-- Clean up
--

BEGIN
  DBMS_CLOUD_AI.DROP_PROFILE('ASKTOM_COHERE');
  DBMS_CLOUD_AI.DROP_PROFILE('ASKTOM_OPENAI');
  DBMS_CLOUD.DROP_CREDENTIAL('OPENAI_CRED');
  DBMS_CLOUD.DROP_CREDENTIAL('COHERE_CRED');
  DBMS_NETWORK_ACL_ADMIN.REMOVE_HOST_ACE('api.cohere.ai');
  DBMS_NETWORK_ACL_ADMIN.REMOVE_HOST_ACE('api.openai.com');
END;
/

--
-- Set up Network ACLs for several AI services endpoints
-- Documentation: https://docs.oracle.com/en-us/iaas/autonomous-database-serverless/doc/configure-dbms_cloud_ai-package.html
--

-- api.cohere.ai
BEGIN
    DBMS_NETWORK_ACL_ADMIN.APPEND_HOST_ACE(
         host => 'api.cohere.ai',
         ace  => xs$ace_type(privilege_list => xs$name_list('http'),
                             principal_name => 'APEXUSER',
                             principal_type => xs_acl.ptype_db)
   );
END;
/

-- api.openai.com
BEGIN
    DBMS_NETWORK_ACL_ADMIN.APPEND_HOST_ACE(
         host => 'api.openai.com',
         ace  => xs$ace_type(privilege_list => xs$name_list('http'),
                             principal_name => 'APEXUSER',
                             principal_type => xs_acl.ptype_db)
   );
END;
/

--
-- More providers, check here:
-- https://docs.oracle.com/en/cloud/paas/autonomous-database/serverless/adbsb/select-ai-manage-profiles.html#GUID-D9EFE56B-402D-4A8B-90E0-96C99FCF81AD
--

---------------------------
-- Connect as user APEXUSER
---------------------------

-- Check existing credential
SELECT credential_name, username, comments FROM all_credentials;

-- Create credentials
BEGIN
    DBMS_CLOUD.create_credential('COHERE_CRED', 'COHERE', '<cohere_api_key>');
END;
/
BEGIN
    DBMS_CLOUD.create_credential('OPENAI_CRED', 'OPENAI', '<openai_api_key>');
END;
/

-- Re-check
SELECT credential_name, username, comments FROM all_credentials;

--
-- Create profiles using credentials
--

-- Check existing profiles
select * from user_cloud_ai_profiles;

-- Drop any existing profiles
BEGIN
  DBMS_CLOUD_AI.DROP_PROFILE('ASKTOM_COHERE');
  DBMS_CLOUD_AI.DROP_PROFILE('ASKTOM_OPENAI');
END;
/

-- Cohere
BEGIN
  DBMS_CLOUD_AI.CREATE_PROFILE(
    profile_name => 'ASKTOM_COHERE',
    attributes =>
    '{
      "provider": "cohere",
      "credential_name": "COHERE_CRED",
      "object_list": [{"owner": "SPATIALUSER", "name": "US_AIRPORTS"},
                      {"owner": "SPATIALUSER", "name": "US_CITIES"},
                      {"owner": "SPATIALUSER", "name": "US_COUNTIES"},
                      {"owner": "SPATIALUSER", "name": "US_HOSPITALS"},
                      {"owner": "SPATIALUSER", "name": "US_STATES"},
                      {"owner": "SPATIALUSER", "name": "USGS_EARTHQUAKES"}]
    }'
  );
END;
/

-- OpenAI
BEGIN
  DBMS_CLOUD_AI.CREATE_PROFILE(
    profile_name => 'ASKTOM_OPENAI',
    attributes =>
    '{
      "provider": "openai",
      "credential_name": "OPENAI_CRED",
      "model": "gpt-4",
      "object_list": [{"owner": "SPATIALUSER", "name": "US_AIRPORTS"},
                      {"owner": "SPATIALUSER", "name": "US_CITIES"},
                      {"owner": "SPATIALUSER", "name": "US_COUNTIES"},
                      {"owner": "SPATIALUSER", "name": "US_HOSPITALS"},
                      {"owner": "SPATIALUSER", "name": "US_STATES"},
                      {"owner": "SPATIALUSER", "name": "USGS_EARTHQUAKES"}]
    }'
  );
END;
/

--Re-check existing profiles
select * from user_cloud_ai_profiles;


--------------------------------------------------------
-- Execute the following statements using profile OPENAI
--------------------------------------------------------

-- Enable AI profile in current session
EXEC DBMS_CLOUD_AI.SET_PROFILE('ASKTOM_OPENAI');
SELECT DBMS_CLOUD_AI.GET_PROFILE() from dual;

--------------------------
-- Queries using SELECT AI
--------------------------

-- First tests
SELECT DBMS_CLOUD_AI.GENERATE(prompt       => 'what is the total number of cities in us',
                              profile_name => 'ASKTOM_OPENAI',
                              action       => 'showsql') as query
FROM dual;

SELECT DBMS_CLOUD_AI.GENERATE(prompt       => 'what is the total number of cities in us',
                              profile_name => 'ASKTOM_OPENAI',
                              action       => 'runsql') as run
FROM dual;

SELECT DBMS_CLOUD_AI.GENERATE(prompt       => 'which earthquakes happened near Juneau International Airport',
                              profile_name => 'ASKTOM_OPENAI',
                              action       => 'chat') as chat
FROM dual;

SELECT DBMS_CLOUD_AI.GENERATE(prompt       => 'which earthquakes happened near Juneau International Airport',
                              profile_name => 'ASKTOM_OPENAI',
                              action       => 'showsql') as query
FROM dual;

SELECT DBMS_CLOUD_AI.GENERATE(prompt       => 'which earthquakes happened near Anchorage, Alaska',
                              profile_name => 'ASKTOM_OPENAI',
                              action       => 'chat') as chat
FROM dual;

SELECT DBMS_CLOUD_AI.GENERATE(prompt       => 'which earthquakes happened near Anchorage, Alaska',
                              profile_name => 'ASKTOM_OPENAI',
                              action       => 'showsql') as query
FROM dual;

SELECT DBMS_CLOUD_AI.GENERATE(prompt       => 'which earthquakes happened near Anchorage, Alaska',
                              profile_name => 'ASKTOM_OPENAI',
                              action       => 'runsql') as run
FROM dual;

SELECT DBMS_CLOUD_AI.GENERATE(prompt       => 'how many earthquakes with a magnitude of 5 or higher were registered in Alaska between Jan 1, 2000 and Dec 31, 2020',
                              profile_name => 'ASKTOM_OPENAI',
                              action       => 'showsql') as query
FROM dual;

SELECT DBMS_CLOUD_AI.GENERATE(prompt       => 'how many earthquakes with a magnitude of 5 or higher were registered in Alaska between Jan 1, 2000 and Dec 31, 2020',
                              profile_name => 'ASKTOM_OPENAI',
                              action       => 'runsql') as run
FROM dual;

SELECT DBMS_CLOUD_AI.GENERATE(prompt       => 'show me the the longitude and latitude of Juneau International Airport, Alaska for the WGS84 coordinate reference system. if needed, convert the DMS coordinates to DD coordinates.',
                              profile_name => 'ASKTOM_OPENAI',
                              action       => 'runsql') as query
FROM dual;

-- Spatial topological searches

SELECT DBMS_CLOUD_AI.GENERATE(prompt       => 'Which earthquakes interact with the state of California',
                              profile_name => 'ASKTOM_OPENAI',
                              action       => 'showsql') as query
FROM dual;

SELECT DBMS_CLOUD_AI.GENERATE(prompt       => 'Which counties overlap the state of Wyoming',
                              profile_name => 'ASKTOM_OPENAI',
                              action       => 'showsql') as query
FROM dual;

SELECT DBMS_CLOUD_AI.GENERATE(prompt       => 'Which counties are inside the state of Wyoming',
                              profile_name => 'ASKTOM_OPENAI',
                              action       => 'showsql') as query
FROM dual;

SELECT DBMS_CLOUD_AI.GENERATE(prompt       => 'Which counties are inside the state of Wyoming. Use SDO_INSIDE.',
                              profile_name => 'ASKTOM_OPENAI',
                              action       => 'showsql') as query
FROM dual;

SELECT DBMS_CLOUD_AI.GENERATE(prompt       => 'Which counties are inside and overlap the state of Wyoming. Use the corresponding Spatial functions.',
                              profile_name => 'ASKTOM_OPENAI',
                              action       => 'showsql') as query
FROM dual;

SELECT DBMS_CLOUD_AI.GENERATE(prompt       => 'Which counties are neighbors of county Hot Springs',
                              profile_name => 'ASKTOM_OPENAI',
                              action       => 'showsql') as query
FROM dual;

SELECT DBMS_CLOUD_AI.GENERATE(prompt       => 'Which counties interact with county Hot Springs',
                              profile_name => 'ASKTOM_OPENAI',
                              action       => 'showsql') as query
FROM dual;

SELECT DBMS_CLOUD_AI.GENERATE(prompt       => 'Which counties interact with Hot Springs',
                              profile_name => 'ASKTOM_OPENAI',
                              action       => 'showsql') as query
FROM dual;

SELECT DBMS_CLOUD_AI.GENERATE(prompt       => 'Find all cities inside New Hampshire. Use SDO_INSIDE.',
                              profile_name => 'ASKTOM_OPENAI',
                              action       => 'showsql') as query
FROM dual;

-- Within distance searches

SELECT DBMS_CLOUD_AI.GENERATE(prompt       => 'Find all cities within 100 miles distance of Nashua',
                              profile_name => 'ASKTOM_OPENAI',
                              action       => 'showsql') as query
FROM dual;

SELECT DBMS_CLOUD_AI.GENERATE(prompt       => 'Find all cities within 100 miles distance of Nashua in New Hampshire',
                              profile_name => 'ASKTOM_OPENAI',
                              action       => 'showsql') as query
FROM dual;

-- Nearest neighbor searches

SELECT DBMS_CLOUD_AI.GENERATE(prompt       => 'Find the nearest cities to all earthquakes with a magnitude higher than 7',
                              profile_name => 'ASKTOM_OPENAI',
                              action       => 'showsql') as query
FROM dual;

SELECT DBMS_CLOUD_AI.GENERATE(prompt       => 'Find the nearest cities to all earthquakes with a magnitude higher than 7. Use SDO_NUM_RES=1 for SDO_NN.',
                              profile_name => 'ASKTOM_OPENAI',
                              action       => 'showsql') as query
FROM dual;

SELECT DBMS_CLOUD_AI.GENERATE(prompt       => 'Find the nearest cities to all earthquakes with a magnitude higher than 7. Use SDO_NUM_RES=1 for SDO_NN. Make sure to use the geometries in the right order within SDO_NN.',
                              profile_name => 'ASKTOM_OPENAI',
                              action       => 'showsql') as query
FROM dual;


--
-- Check potential improvements
--

-- 1. Better SQL generation through better metadata
--   Add table and column comments

comment on table USGS_EARTHQUAKES is 'Earthquake records from around the world collected from the United States Geological Survey (USGS). Important details about the earthquake such as distance, gap, magnitude, depth and significance are included to properly describe the earthquake. Additionally, data about exact geological coordinates and a relative description of the earthquake’s location is included. The earthquakes collected are from the past month.';
comment on column USGS_EARTHQUAKES.ID is 'A unique identifier for this earthquake';
comment on column USGS_EARTHQUAKES.IMPACT_GAP is 'In general, the smaller this number, the more reliable is the calculated horizontal position of the earthquake. Specifically, this means the largest azimuthal gap between azimuthally adjacent stations (in degrees). Earthquake locations in which the azimuthal gap exceeds 180 degrees typically have large location and depth uncertainties. Ranges from 0 to 180.';
comment on column USGS_EARTHQUAKES.IMPACT_MAGNITUDE is 'Earthquake magnitude is a measure of the size of an earthquake at its source. It is a logarithmic measure. At the same distance from the earthquake, the amplitude of the seismic waves from which the magnitude is determined are approximately 10 times as large during a magnitude 5 earthquake as during a magnitude 4 earthquake. The total amount of energy released by the earthquake usually goes up by a larger factor; for many commonly used magnitude types, the total energy of an average earthquake goes up by a factor of approximately 32 for each unit increase in magnitude. Typically ranges from -1 (very tiny) to 10 (incredibly powerful).';
comment on column USGS_EARTHQUAKES.IMPACT_SIGNIFICANCE is 'A number describing how significant the event is. Larger numbers indicate a more significant event. This value is determined on a number of factors, including magnitude, maximum MMI, felt reports, and estimated impact. Ranges from 0 to 1000.';
comment on column USGS_EARTHQUAKES.LOCATION_DEPTH is 'Depth of the event in kilometers.';
comment on column USGS_EARTHQUAKES.LOCATION_DISTANCE is 'The rough distance that this earthquake occurred away from the reporting station, measured in degrees between. 1 degree is approximately 111.2 kilometers. In general, the smaller this number, the more reliable is the calculated depth of the earthquake. In general, this number is between 0.4-7.1.';
comment on column USGS_EARTHQUAKES.LOCATION_FULL is 'The full name of earthquake location.';
comment on column USGS_EARTHQUAKES.LOCATION_LATITUDE is 'Decimal degrees latitude (up and down on the globe). Negative values for southern latitudes. Ranges from -90 to 90.';
comment on column USGS_EARTHQUAKES.LOCATION_LONGITUDE is 'Decimal degrees longitude (east and west on the globe). Negative values for western latitudes. Ranges from -180 to 180.';
comment on column USGS_EARTHQUAKES.LOCATION_NAME is 'A best guess for the name of the state (or country, in some cases) that this earthquake was reported in.';
comment on column USGS_EARTHQUAKES.TIME_DAY is 'Day of the month for this earthquake.';
comment on column USGS_EARTHQUAKES.TIME_EPOCH is 'A number that represents the time that this earthquake occurred. Epoch''s are the number of seconds since a particular date (January 1st, 1970), and are a convenient way to store date/times.';
comment on column USGS_EARTHQUAKES.TIME_FULL is 'The full date/time representation for when this earthquake occurred.';
comment on column USGS_EARTHQUAKES.TIME_HOUR is 'The hour that this earthquake occurred.';
comment on column USGS_EARTHQUAKES.TIME_MINUTE is 'The minute that this earthquake occurred.';
comment on column USGS_EARTHQUAKES.TIME_MONTH is 'The month that this earthquake occurred.';
comment on column USGS_EARTHQUAKES.TIME_SECOND is 'The second that this earthquake occurred.';
comment on column USGS_EARTHQUAKES.TIME_YEAR is 'The year that this earthquake occurred.';
comment on column USGS_EARTHQUAKES.GEOMETRY is 'The SDO_GEOMETRY representation of the earthquake location with coordinates in WGS84 (SRID = 4326).';

comment on table US_AIRPORTS is 'This dataset contains all airports in the United States. The data source is https://ourairports.com/data/.';
comment on column US_AIRPORTS.IATA_CODE is 'An IATA airport code, also known as an IATA location identifier, IATA station code, or simply a location identifier, is a three-letter geocode designating many airports and metropolitan areas around the world, defined by the International Air Transport Association (IATA).';
comment on table US_CITIES is 'This dataset contains all cities located in the United States';
comment on table US_COUNTIES is 'This dataset contains all US administrative boundaries on county level published as Open Data.';
comment on table US_HOSPITALS is 'This dataset contains information about 7596 hospitals in the US, including latitude, longitude, staff, beds, ownership, among others. All records were extracted from the U.S. Department of Homeland Security. Source: https://www.kaggle.com/datasets/andrewmvd/us-hospital-locations';
comment on table US_STATES is 'This dataset represents US States and equivalent entities, which are the primary governmental divisions of the United States. The TIGER/Line sgeometries are an extract of selected geographic and cartographic information from the U.S. Census Bureau''s Master Address File / Topologically Integrated Geographic Encoding and Referencing (MAF/TIGER) Database (MTDB). The MTDB represents a seamless national file with no overlaps or gaps between parts, however, each TIGER/Line geometry is designed to stand alone as an independent data set, or they can be combined to cover the entire nation. In addition to the fifty States, the Census Bureau treats the District of Columbia, Puerto Rico, and each of the Island Areas (American Samoa, the Commonwealth of the Northern Mariana Islands, Guam, and the U.S. Virgin Islands) as the statistical equivalents of States for the purpose of data presentation.';

-- Re-create the profile using comments

BEGIN
  DBMS_CLOUD_AI.DROP_PROFILE('ASKTOM_OPENAI');
  DBMS_CLOUD_AI.CREATE_PROFILE(
    profile_name => 'ASKTOM_OPENAI',
    attributes =>
    '{
      "provider": "openai",
      "credential_name": "OPENAI_CRED",
      "comments": "true",
      "model": "gpt-4",
      "object_list": [{"owner": "SPATIALUSER", "name": "US_AIRPORTS"},
                      {"owner": "SPATIALUSER", "name": "US_CITIES"},
                      {"owner": "SPATIALUSER", "name": "US_COUNTIES"},
                      {"owner": "SPATIALUSER", "name": "US_HOSPITALS"},
                      {"owner": "SPATIALUSER", "name": "US_STATES"},
                      {"owner": "SPATIALUSER", "name": "USGS_EARTHQUAKES"}]
    }'
  );
  DBMS_CLOUD_AI.SET_PROFILE('ASKTOM_OPENAI');
END;
/

-- Spatial topological searches

SELECT DBMS_CLOUD_AI.GENERATE(prompt       => 'Which earthquakes interact with the state of California',
                              profile_name => 'ASKTOM_OPENAI',
                              action       => 'showsql') as query
FROM dual;

SELECT DBMS_CLOUD_AI.GENERATE(prompt       => 'Which counties overlap the state of Wyoming',
                              profile_name => 'ASKTOM_OPENAI',
                              action       => 'showsql') as query
FROM dual;

SELECT DBMS_CLOUD_AI.GENERATE(prompt       => 'Which counties are inside the state of Wyoming',
                              profile_name => 'ASKTOM_OPENAI',
                              action       => 'showsql') as query
FROM dual;

SELECT DBMS_CLOUD_AI.GENERATE(prompt       => 'Which counties are inside the state of Wyoming. Use SDO_INSIDE.',
                              profile_name => 'ASKTOM_OPENAI',
                              action       => 'showsql') as query
FROM dual;

SELECT DBMS_CLOUD_AI.GENERATE(prompt       => 'Which counties are inside and overlap the state of Wyoming. Use the corresponding Spatial functions.',
                              profile_name => 'ASKTOM_OPENAI',
                              action       => 'showsql') as query
FROM dual;

SELECT DBMS_CLOUD_AI.GENERATE(prompt       => 'Which counties are neighbors of county Hot Springs',
                              profile_name => 'ASKTOM_OPENAI',
                              action       => 'showsql') as query
FROM dual;

SELECT DBMS_CLOUD_AI.GENERATE(prompt       => 'Which counties interact with county Hot Springs',
                              profile_name => 'ASKTOM_OPENAI',
                              action       => 'showsql') as query
FROM dual;

SELECT DBMS_CLOUD_AI.GENERATE(prompt       => 'Which counties interact with Hot Springs',
                              profile_name => 'ASKTOM_OPENAI',
                              action       => 'showsql') as query
FROM dual;

SELECT DBMS_CLOUD_AI.GENERATE(prompt       => 'Find all cities inside New Hampshire. Use SDO_INSIDE.',
                              profile_name => 'ASKTOM_OPENAI',
                              action       => 'showsql') as query
FROM dual;

-- Within distance searches

SELECT DBMS_CLOUD_AI.GENERATE(prompt       => 'Find all cities within 100 miles distance of Nashua',
                              profile_name => 'ASKTOM_OPENAI',
                              action       => 'showsql') as query
FROM dual;

SELECT DBMS_CLOUD_AI.GENERATE(prompt       => 'Find all cities within 100 miles distance of Nashua in New Hampshire',
                              profile_name => 'ASKTOM_OPENAI',
                              action       => 'showsql') as query
FROM dual;

-- Nearest neighbor searches

SELECT DBMS_CLOUD_AI.GENERATE(prompt       => 'Find the nearest cities to all earthquakes with a magnitude higher than 7',
                              profile_name => 'ASKTOM_OPENAI',
                              action       => 'showsql') as query
FROM dual;

SELECT DBMS_CLOUD_AI.GENERATE(prompt       => 'Find the nearest cities to all earthquakes with a magnitude higher than 7. Use SDO_NUM_RES=1 for SDO_NN.',
                              profile_name => 'ASKTOM_OPENAI',
                              action       => 'showsql') as query
FROM dual;

SELECT DBMS_CLOUD_AI.GENERATE(prompt       => 'Find the nearest cities to all earthquakes with a magnitude higher than 7. Use SDO_NUM_RES=1 for SDO_NN. Make sure to use the geometries in the right order within SDO_NN.',
                              profile_name => 'ASKTOM_OPENAI',
                              action       => 'showsql') as query
FROM dual;

-- General questions regarding Oracle Spatial

SELECT DBMS_CLOUD_AI.GENERATE(prompt       => 'What are best practices to create a spatial index for large point datasets stored in the Oracle Database',
                              profile_name => 'ASKTOM_OPENAI',
                              action       => 'chat') as chat
FROM dual;

SELECT DBMS_CLOUD_AI.GENERATE(prompt       => 'Generate the appropriate CREATE INDEX statement for a point table named MY_POINTS based on best practices for create spatial indexes in an Oracle Database version 19c or higher.',
                              profile_name => 'ASKTOM_OPENAI',
                              action       => 'chat') as chat
FROM dual;

SELECT DBMS_CLOUD_AI.GENERATE(prompt       => 'Generate the appropriate CREATE INDEX statement for a point table named MY_POINTS using a CBTREE index.',
                              profile_name => 'ASKTOM_OPENAI',
                              action       => 'chat') as chat
FROM dual;


--
-- End of January session
--