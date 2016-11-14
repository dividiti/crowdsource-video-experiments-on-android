package openscience.crowdsource.video.experiments;

import org.json.JSONObject;

/**
 * Created by daniil on 11/14/16.
 */
public class RecognitionScenario {
    private String defaultImagePath;
    private String dataUOA;
    private String moduleUOA;
    private String title;
    private String totalFileSize;
    private Long totalFileSizeBytes;


    private JSONObject rawJSON; //todo move out to file


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDefaultImagePath() {
        return defaultImagePath;
    }

    public void setDefaultImagePath(String defaultImagePath) {
        this.defaultImagePath = defaultImagePath;
    }

    public String getDataUOA() {
        return dataUOA;
    }

    public void setDataUOA(String dataUOA) {
        this.dataUOA = dataUOA;
    }

    public String getModuleUOA() {
        return moduleUOA;
    }

    public void setModuleUOA(String moduleUOA) {
        this.moduleUOA = moduleUOA;
    }

    public JSONObject getRawJSON() {
        return rawJSON;
    }

    public void setRawJSON(JSONObject rawJSON) {
        this.rawJSON = rawJSON;
    }

    public String getTotalFileSize() {
        return totalFileSize;
    }

    public void setTotalFileSize(String totalFileSize) {
        this.totalFileSize = totalFileSize;
    }

    public Long getTotalFileSizeBytes() {
        return totalFileSizeBytes;
    }

    public void setTotalFileSizeBytes(Long totalFileSizeBytes) {
        this.totalFileSizeBytes = totalFileSizeBytes;
    }

    @Override
    public String toString() {
        return title;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RecognitionScenario that = (RecognitionScenario) o;

        if (defaultImagePath != null ? !defaultImagePath.equals(that.defaultImagePath) : that.defaultImagePath != null)
            return false;
        if (dataUOA != null ? !dataUOA.equals(that.dataUOA) : that.dataUOA != null)
            return false;
        if (moduleUOA != null ? !moduleUOA.equals(that.moduleUOA) : that.moduleUOA != null)
            return false;
        if (title != null ? !title.equals(that.title) : that.title != null) return false;
        if (totalFileSize != null ? !totalFileSize.equals(that.totalFileSize) : that.totalFileSize != null)
            return false;
        return rawJSON != null ? rawJSON.equals(that.rawJSON) : that.rawJSON == null;

    }

    @Override
    public int hashCode() {
        int result = defaultImagePath != null ? defaultImagePath.hashCode() : 0;
        result = 31 * result + (dataUOA != null ? dataUOA.hashCode() : 0);
        result = 31 * result + (moduleUOA != null ? moduleUOA.hashCode() : 0);
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (totalFileSize != null ? totalFileSize.hashCode() : 0);
        result = 31 * result + (rawJSON != null ? rawJSON.hashCode() : 0);
        return result;
    }
}
