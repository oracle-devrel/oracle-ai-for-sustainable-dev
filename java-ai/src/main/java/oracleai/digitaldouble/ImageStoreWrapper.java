package oracleai.digitaldouble;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ImageStoreWrapper {
    private List<ImageStore> items;

    public List<ImageStore> getItems() {
        return items;
    }

    public void setItems(List<ImageStore> items) {
        this.items = items;
    }
}