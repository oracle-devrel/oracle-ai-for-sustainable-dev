package oracleai;

import com.fasterxml.jackson.databind.ObjectMapper;
import oracleai.geojson.Feature;
import oracleai.geojson.FeatureCollection;

import java.sql.Clob;
import java.sql.ResultSet;

import oracleai.json.AnnualDeathCause;
import oracleai.json.CancerOpenResearch;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/data")
public class HealthDataController {

    private String schema = ""; // including "." suffix, eg "DEMOUSER.", if other than current user for some
                                // reason

    JdbcTemplate jdbcTemplate;

    @Autowired
    public HealthDataController(DataSource dataSource) {
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    /**
     * descr ANNUAL_DEATH_CAUSE;
     * Name Null? Type
     * ----------------------------------- -------- ------------
     * ID NOT NULL NUMBER
     * ENTITY VARCHAR2(50)
     * CODE VARCHAR2(50)
     * YEAR NUMBER
     * DEATHS_MENINGITIS NUMBER
     * DEATHS_ALZHEIMERS_DISEASE NUMBER
     * DEATHS_PARKINSON_DISEASE NUMBER
     * DEATHS_NUTRITIONAL_DEFICIENCIES NUMBER
     * DEATHS_MALARIA NUMBER
     * DEATHS_MATERNAL_DISORDERS NUMBER
     * DEATHS_HIV_AIDS NUMBER
     * DEATHS_DRUG_DISORDERS NUMBER
     * DEATHS_TUBERCULOSIS NUMBER
     * DEATHS_CARDIOVASCULAR NUMBER
     * DEATHS_RESPIRATORY_INFECTIONS NUMBER
     * DEATHS_NEONATAL_DISORDERS NUMBER
     * DEATHS_ALCOHOL NUMBER
     * DEATHS_DIARRHEAL_DISEASES NUMBER
     * DEATHS_ENVIRONMENTAL_CHANGES NUMBER
     * DEATHS_NEOPLASMS NUMBER
     * DEATHS_DIABETES NUMBER
     * DEATHS_CHRONIC_KIDNEY NUMBER
     * DEATHS_PROTEIN_MALNUTRITION NUMBER
     * DEATHS_CHRONIC_RESPIRATORY_DISEASES NUMBER
     * DEATHS_LIVER_DISEASES NUMBER
     * DEATHS_DIGESTIVE_DISEASES NUMBER
     * DEATHS_ACUTE_HEPATITIS NUMBER
     */
    @CrossOrigin
    @GetMapping("/getCausesOfDeath")
    public String getCausesOfDeath() throws Exception {
        String sql = "SELECT ENTITY, YEAR, DEATHS_MENINGITIS, DEATHS_ALZHEIMERS_DISEASE, " +
                "DEATHS_PARKINSON_DISEASE, DEATHS_NUTRITIONAL_DEFICIENCIES, DEATHS_MALARIA, " +
                "DEATHS_MATERNAL_DISORDERS " +
                "FROM " + schema + "ANNUAL_DEATH_CAUSE WHERE YEAR = 2019 " +
                "FETCH FIRST 100 ROWS ONLY"; // 7232 rows
        List<AnnualDeathCause> annualDeathCauses = new ArrayList<>();
        System.out.println("getCausesOfDeath ...");
        jdbcTemplate.query(sql, (RowMapper) (rs, rowNum) -> {
            AnnualDeathCause feature = new AnnualDeathCause(
                    rs.getString("ENTITY"),
                    rs.getInt("YEAR"),
                    rs.getInt("DEATHS_MENINGITIS"),
                    rs.getInt("DEATHS_ALZHEIMERS_DISEASE"),
                    rs.getInt("DEATHS_PARKINSON_DISEASE"),
                    rs.getInt("DEATHS_NUTRITIONAL_DEFICIENCIES"),
                    rs.getInt("DEATHS_MALARIA"),
                    rs.getInt("DEATHS_MATERNAL_DISORDERS"));
            annualDeathCauses.add(feature);
            return "";
        });

        System.out.println("getCausesOfDeath size:" + annualDeathCauses.size());
        String json = new ObjectMapper().writeValueAsString(annualDeathCauses);
        return json;
    }

    /**
     * descr CANCER_OPEN_RESEARCH
     * 
     * Name Null? Type
     * ----------------------- ----- ---------------
     * COLUMN_1 NUMBER
     * DATE_RW DATE
     * STUDY VARCHAR2(4000)
     * STUDY_LINK VARCHAR2(256)
     * JOURNAL VARCHAR2(64)
     * SEVERE VARCHAR2(64)
     * SEVERE_LOWER_BOUND NUMBER
     * SEVERE_UPPER_BOUND NUMBER
     * SEVERE_P_VALUE NUMBER
     * SEVERE_SIGNIFICANT VARCHAR2(64)
     * SEVERE_ADJUSTED VARCHAR2(64)
     * SEVERE_CALCULATED VARCHAR2(64)
     * FATALITY VARCHAR2(64)
     * FATALITY_LOWER_BOUND NUMBER
     * FATALITY_UPPER_BOUND NUMBER
     * FATALITY_P_VALUE VARCHAR2(64)
     * FATALITY_SIGNIFICANT VARCHAR2(64)
     * FATALITY_ADJUSTED VARCHAR2(64)
     * FATALITY_CALCULATED VARCHAR2(64)
     * MULTIVARIATE_ADJUSTMENT VARCHAR2(4000)
     * STUDY_TYPE VARCHAR2(64)
     * SAMPLE_SIZE VARCHAR2(32767)
     * STUDY_POPULATION VARCHAR2(4000)
     * ADDED_ON DATE
     * CRITICAL_ONLY VARCHAR2(64)
     * Discharged vs. death? VARCHAR2(64)
     * 
     * 
     * DATE_RW DATE
     * SEVERE_LOWER_BOUND NUMBER
     */
    @CrossOrigin
    @GetMapping("/getCancerOpenResearch")
    public String getCancerOpenResearch() throws Exception {
        String sql = "SELECT " +
                "    TO_CHAR(DATE_RW, 'Month') AS Month_Name," +
                "    EXTRACT(MONTH FROM DATE_RW) AS Month_Number," +
                // " EXTRACT(YEAR FROM DATE_RW) AS Year," +
                "    SUM(SEVERE_LOWER_BOUND) AS Sum_Severe_Lower_Bound," +
                "    SUM(SEVERE_UPPER_BOUND) AS Sum_Severe_Upper_Bound," +
                "    SUM(FATALITY_LOWER_BOUND) AS Sum_Fatality_Lower_Bound," +
                "    SUM(FATALITY_UPPER_BOUND) AS Sum_Fatality_Upper_Bound " +
                "FROM " + schema + "CANCER_OPEN_RESEARCH " +
                "GROUP BY  TO_CHAR(DATE_RW, 'Month'), EXTRACT(MONTH FROM DATE_RW), EXTRACT(YEAR FROM DATE_RW) " +
                "ORDER BY EXTRACT(YEAR FROM DATE_RW), EXTRACT(MONTH FROM DATE_RW)"; // 37 rows
        List<CancerOpenResearch> cancerOpenResearches = new ArrayList<>();
        System.out.println("getCausesOfDeath ...");
        jdbcTemplate.query(sql, (RowMapper) (rs, rowNum) -> {
            CancerOpenResearch cancerOpenResearch = new CancerOpenResearch(
                    rs.getInt("Month_Number"),
                    rs.getString("Month_Name"),
                    rs.getInt("Sum_Severe_Lower_Bound"),
                    rs.getInt("Sum_Severe_Upper_Bound"),
                    rs.getInt("Sum_Fatality_Lower_Bound"),
                    rs.getInt("Sum_Fatality_Upper_Bound"));
            cancerOpenResearches.add(cancerOpenResearch);
            return "";
        });
        System.out.println("getCancerOpenResearch size:" + cancerOpenResearches.size());
        String json = new ObjectMapper().writeValueAsString(cancerOpenResearches);
        System.out.println("getCancerOpenResearch json:" + json);
        return json;
    }

    /**
     * descr US_HOSPITALS
     * Name Null? Type
     * ---------- -------- -------------------
     * ID_1 NOT NULL NUMBER
     * LATITUDE NUMBER
     * LONGITUDE NUMBER
     * OBJECTID NUMBER
     * ID NUMBER
     * NAME VARCHAR2(255)
     * ADDRESS VARCHAR2(255)
     * CITY VARCHAR2(50)
     * STATE VARCHAR2(50)
     * ZIP NUMBER
     * TELEPHONE VARCHAR2(50)
     * TYPE VARCHAR2(50)
     * STATUS VARCHAR2(50)
     * POPULATION NUMBER
     * COUNTY VARCHAR2(50)
     * COUNTYFIPS NUMBER
     * COUNTRY VARCHAR2(50)
     * LATITUDE2 NUMBER
     * LONGITUDE2 NUMBER
     * NAICS_CODE NUMBER
     * NAICS_DESC VARCHAR2(255)
     * SOURCE VARCHAR2(255)
     * WEBSITE VARCHAR2(255)
     * OWNER VARCHAR2(255)
     * TTL_STAFF NUMBER
     * BEDS NUMBER
     * TRAUMA VARCHAR2(50)
     * SHAPE PUBLIC.SDO_GEOMETRY
     */
    @CrossOrigin
    @GetMapping("/getHospitals")
    public String getHospitals() throws Exception {
        String sql = "SELECT name, ADDRESS, longitude, latitude, COUNT(*) as count " +
                "FROM " + schema + "US_HOSPITALS " +
                "GROUP BY name, ADDRESS, longitude, latitude " +
                "ORDER BY count DESC " +
                "FETCH FIRST 300 ROWS ONLY"; // 7563 rows
        List<Feature> features = new ArrayList<>();
        System.out.println("getHospitals ...");
        jdbcTemplate.query(sql, new RowMapper() {
            public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                String name = rs.getString("name");
                String address = rs.getString("ADDRESS");
                // todo seems the database table has long and lat backwards?
                double longitude = rs.getDouble("latitude");
                double latitude = rs.getDouble("longitude");
                int count = rs.getInt("count"); // currently unused
                Feature feature = new Feature(
                        name, "",
                        "https://baycare.org/-/media/project/baycare/consumer-portal/hospitals-page/mease-countryside-hospital.jpg",
                        longitude, latitude);
                features.add(feature);
                return "";
            }
        });
        System.out.println("getHospitals featureslength:" + features.size());
        String json = FeatureCollection.build(features).toJson();
        return json;
    }

    @CrossOrigin
    @GetMapping("/getDocuments")
    public String getDocuments() throws Exception {
        // public ResponseEntity<String> getDocuments() throws Exception {
        String sql = "SELECT jsondata FROM aidocument_results";
        List<JSONObject> results = jdbcTemplate.query(sql, new DocumentAnalysisRowMapper());

        JSONArray jsonArray = new JSONArray();
        for (JSONObject jsonObject : results) {
            jsonArray.put(jsonObject);
        }

        System.out.println("DocumentAnalysisRowMapper.jsonArray.toString() " + jsonArray.toString());
        return jsonArray.toString();
        // return ResponseEntity.ok(jsonArray.toString());
    }

    private static class DocumentAnalysisRowMapper implements RowMapper<JSONObject> {
        @Override
        public JSONObject mapRow(ResultSet rs, int rowNum) throws SQLException {
            Clob clob = rs.getClob("jsondata");
            String jsonString = clob.getSubString(1, (int) clob.length());
            System.out.println("DocumentAnalysisRowMapper.mapRow " + jsonString);
            return new JSONObject(jsonString);
        }
    }
}
