package oracleai.geojson;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

public class Feature {
    @Getter @Setter private String type;
    @Getter @Setter private Geometry geometry;
    @Getter @Setter private Properties properties;
    public Feature() {}

    public Feature(String creatorname, String visitorname, String imageurl, double longitude, double latitude) {
        type = "Feature";
        geometry = new Geometry();
        List<Double> list = new ArrayList<>();
        list.add(longitude);
        list.add(latitude);
        geometry.setCoordinates(list);
        properties = new Properties();
        properties.setName(creatorname);
        properties.setImage(imageurl);
        properties.setVisitorname(visitorname);
    }
}