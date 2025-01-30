# AskTOM Spatial Series - January 2025 session

## Part: SELECT AI for spatial queries

My tests with `SELECT AI` to generate SQL queries use a few spatial datasets. You need to load the data first.

The data for the tables

* US_COUNTIES
* US_STATES

are stored in [this folder](./data). Use [Oracle Spatial Studio](https://www.oracle.com/database/technologies/spatial-studio/get-started.html) to load them into your 23ai Autonomous Database. If you haven´t yet installed Spatial Studio, you can download it from [here](https://www.oracle.com/database/technologies/spatial-studio/oracle-spatial-studio-downloads.html).

Use the following scripts to create the remaining tables and fill them with data:

* [US_AIRPORTS](./scripts/sql/us_airports.sql)
* [US_CITIES](./scripts/sql/us_cities.sql)
* [US_HOSPITALS](./scripts/sql/us_hospitals.sql)
* [USGS_EARTHQUAKES](./scripts/sql/usgs_earthquakes.sql)

Once you have all datasets in place, you can use [this script](./scripts/sql/selectai_spatial_tests.sql) to use `SELECT AI` for testing the generation SQL queries. The tests aim to figure out

1. how well use the generated SQL queries incorporate spatial operators and functions to analyze the data and
2. what you can do avoid hallucinations and to improve the generated SQL queries.
