package oracleai.geojson;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class FeatureCollection {
    @Getter
    @Setter
    private String type = "FeatureCollection";
    @Getter @Setter private List<Feature> features;

    public FeatureCollection() {

    }
    public FeatureCollection(List<Feature> features) {
        this.features = features;
    }

    public static FeatureCollection build(List<Feature> features) {
        FeatureCollection featureCollection = new FeatureCollection(features);
        featureCollection.setFeatures(features);
        return featureCollection;
    }

    public String toJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }

}






