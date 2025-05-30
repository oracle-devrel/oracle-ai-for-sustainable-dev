package oracleai.financial;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/financial")
//@CrossOrigin(origins = "https://oracledatabase-financial.org")
@CrossOrigin(origins = "*")
//@CrossOrigin(origins = "http://158.180.20.119")
public class FinancialController {

    @Autowired
    private DataSource dataSource;

    @GetMapping("/test")
    public String test() {
        return "test";
    }

    @GetMapping("/testconn")
    public String testconn() throws Exception{
        System.out.println("FinancialController.testconn dataSource (about to get connection):" + dataSource);
        Connection connection = dataSource.getConnection();
        System.out.println("FinancialController.testconn connection:" + connection);
        return "connection = " + connection;
    }


    @GetMapping("/locations/coordinates")
    public List<Map<String, Object>> getLocationCoordinates() {
        String sql = "SELECT LON, LAT FROM locations";
        List<Map<String, Object>> coordinates = new ArrayList<>();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                Map<String, Object> coord = new HashMap<>();
                coord.put("lat", resultSet.getDouble("LAT"));
                coord.put("lng", resultSet.getDouble("LON"));
                coordinates.add(coord);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return coordinates;
    }

    @PostMapping("/locations/check-distance")
    public ResponseEntity<Boolean> checkDistance(@RequestBody Map<String, Object> payload) {
        try {
            Map<String, String> firstLocation = (Map<String, String>) payload.get("firstLocation");
            Map<String, String> secondLocation = (Map<String, String>) payload.get("secondLocation");

            double lat1 = Double.parseDouble(firstLocation.get("latitude"));
            double lon1 = Double.parseDouble(firstLocation.get("longitude"));
            double lat2 = Double.parseDouble(secondLocation.get("latitude"));
            double lon2 = Double.parseDouble(secondLocation.get("longitude"));

            double distanceKm = haversine(lat1, lon1, lat2, lon2);

            return ResponseEntity.ok(distanceKm > 500.0);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(false);
        }
    }

    /**
     * Haversine formula to calculate the great-circle distance between two points.
     */
    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; // distance in km
    }
}
