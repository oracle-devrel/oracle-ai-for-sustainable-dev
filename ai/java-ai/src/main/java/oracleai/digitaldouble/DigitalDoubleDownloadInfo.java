package oracleai.digitaldouble;

public class DigitalDoubleDownloadInfo {

    String email;
    String modelUrl;
    String modelGlbUrl;
    String modelFbxUrl ;
    String modelUsdzUrl ;
    String thumbnailUrl;
    String animatedVideoLocation;
    String similarImageUrl;



    public DigitalDoubleDownloadInfo() {

    }
    public DigitalDoubleDownloadInfo(String modelUrl, String modelGlbUrl, String modelFbxUrl,
                                     String modelUsdzUrl, String thumbnailUrl) {
        this.modelUrl = modelUrl;
        this.modelGlbUrl = modelGlbUrl;
        this.modelFbxUrl = modelFbxUrl;
        this.modelUsdzUrl = modelUsdzUrl;
        this.thumbnailUrl = thumbnailUrl;
    }

    public DigitalDoubleDownloadInfo(String modelGlbUrl, String modelFbxUrl, String modelUsdzUrl,
                                     String thumbnailUrl, String animatedVideoLocation,
                                     String email, String similarImageUrl) {
        this.modelGlbUrl = modelGlbUrl;
        this.modelFbxUrl = modelFbxUrl;
        this.modelUsdzUrl = modelUsdzUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.animatedVideoLocation = animatedVideoLocation;
        this.email = email;
        this.similarImageUrl = similarImageUrl;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setModelUrl(String modelUrl) {
        this.modelUrl = modelUrl;
    }

    public void setModelGlbUrl(String modelGlbUrl) {
        this.modelGlbUrl = modelGlbUrl;
    }

    public void setModelFbxUrl(String modelFbxUrl) {
        this.modelFbxUrl = modelFbxUrl;
    }

    public void setModelUsdzUrl(String modelUsdzUrl) {
        this.modelUsdzUrl = modelUsdzUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public void setAnimatedVideoLocation(String animatedVideoLocation) {
        this.animatedVideoLocation = animatedVideoLocation;
    }

    public void setSimilarImageUrl(String similarImageUrl) {
        this.similarImageUrl = similarImageUrl;
    }

    public String getEmail() {
        return email;
    }

    public String getModelUrl() {
        return modelUrl;
    }

    public String getModelGlbUrl() {
        return modelGlbUrl;
    }

    public String getModelFbxUrl() {
        return modelFbxUrl;
    }

    public String getModelUsdzUrl() {
        return modelUsdzUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public String getAnimatedVideoLocation() {
        return animatedVideoLocation;
    }

    public String getSimilarImageUrl() {
        return similarImageUrl;
    }
}
