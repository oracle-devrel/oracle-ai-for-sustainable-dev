package oracleai.financial;

import org.locationtech.jts.io.WKTReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@RestController
@RequestMapping("/api/anomaly")
public class AnomalyDetectionController {

    @Autowired
    private DataSource dataSource;

    private WKTReader wktReader = new WKTReader();

    // 1. Create connection (handled by Spring Boot/JdbcTemplate)

    // 2. Get cluster centroids
    @PostMapping("/cluster-centroids/{custId}")
    public List<Map<String, Object>> getClusterCentroids(@PathVariable int custId) throws Exception {
        List<Map<String, Object>> centroids = new ArrayList<>();

        try (Connection conn = dataSource.getConnection()) {
            // Truncate labels table
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("TRUNCATE TABLE transaction_labels");
            }

            // Fetch transactions for customer
            List<Map<String, Object>> transactions = new ArrayList<>();
            String txSql = "SELECT a.cust_id, a.trans_id, a.trans_epoch_date, (lonlat_to_proj_geom(b.lon,b.lat)).get_wkt() " +
                    "FROM transactions a, locations b WHERE a.location_id=b.location_id AND cust_id=?";
            try (PreparedStatement ps = conn.prepareStatement(txSql)) {
                ps.setInt(1, custId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> map = new HashMap<>();
                        map.put("cust_id", rs.getInt(1));
                        map.put("trans_id", rs.getInt(2));
                        map.put("epoch_date", rs.getLong(3));
                        map.put("geometry", rs.getString(4));
                        transactions.add(map);
                    }
                }
            }

            // TODO: Implement ST-DBSCAN clustering and insert labels into transaction_labels
            // For now, stub: assign label -1 to all
            String insertSql = "INSERT INTO transaction_labels (trans_id, label) VALUES (?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                for (Map<String, Object> tx : transactions) {
                    ps.setObject(1, tx.get("trans_id"));
                    ps.setInt(2, -1);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            // Fetch cluster centroids
            String centroidSql = "SELECT label, min(trans_epoch_date) as min_time, max(trans_epoch_date) as max_time, " +
                    "SDO_AGGR_CENTROID(SDOAGGRTYPE(lonlat_to_proj_geom(b.lon,b.lat), 0.005)).get_wkt() as geometry, " +
                    "count(*) as trans_count " +
                    "FROM transactions a, locations b, transaction_labels c " +
                    "WHERE a.location_id=b.location_id AND a.trans_id=c.trans_id AND c.label != -1 GROUP BY label";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(centroidSql)) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("label", rs.getInt("label"));
                    map.put("min_time", rs.getLong("min_time"));
                    map.put("max_time", rs.getLong("max_time"));
                    map.put("geometry", rs.getString("geometry"));
                    map.put("trans_count", rs.getInt("trans_count"));
                    centroids.add(map);
                }
            }
        }
        return centroids;
    }

    // 3. Get anomalies
    @GetMapping("/anomalies/{custId}")
    public List<Map<String, Object>> getAnomalies(@PathVariable int custId) throws Exception {
        List<Map<String, Object>> anomalies = new ArrayList<>();
        String sql =
                "WITH " +
                "x as (SELECT a.cust_id, a.location_id, a.trans_id, a.trans_epoch_date, " +
                "lonlat_to_proj_geom(b.lon,b.lat) as proj_geom, c.label " +
                "FROM transactions a, locations b, transaction_labels c " +
                "WHERE a.location_id=b.location_id AND a.trans_id=c.trans_id ), " +
                "y as (SELECT label, min(trans_epoch_date) as min_time, max(trans_epoch_date) as max_time, " +
                "SDO_AGGR_CENTROID(SDOAGGRTYPE(lonlat_to_proj_geom(b.lon,b.lat), 0.005)) as proj_geom, " +
                "count(*) as trans_count FROM transactions a, locations b, transaction_labels c " +
                "WHERE a.location_id=b.location_id AND a.trans_id=c.trans_id AND c.label != -1 GROUP BY label) " +
                "SELECT x.cust_id, x.trans_epoch_date, (x.proj_geom).get_wkt(), x.trans_id, x.label, y.label, " +
                "round(sdo_geom.sdo_distance(x.proj_geom, y.proj_geom, 0.05, 'unit=KM')) " +
                "FROM x, y WHERE x.trans_epoch_date between y.min_time and y.max_time " +
                "AND x.label!=y.label AND x.label=-1 " +
                "AND sdo_within_distance(x.proj_geom, y.proj_geom, 'distance=500 unit=KM') = 'FALSE'";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("cust_id", rs.getInt(1));
                map.put("trans_epoch_date", rs.getLong(2));
                map.put("geometry", rs.getString(3));
                map.put("trans_id", rs.getInt(4));
                map.put("label", rs.getInt(5));
                map.put("outlier_to_label", rs.getInt(6));
                map.put("distance", rs.getDouble(7));
                anomalies.add(map);
            }
        }
        return anomalies;
    }

    // 4. Get map (returns GeoJSON for plotting)
    @GetMapping("/map/{custId}")
    public Map<String, Object> getMap(@PathVariable int custId) throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("centroids", getClusterCentroids(custId));
        map.put("anomalies", getAnomalies(custId));
        return map;
    }
}