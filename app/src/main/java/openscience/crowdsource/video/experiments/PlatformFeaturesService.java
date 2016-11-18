package openscience.crowdsource.video.experiments;

import org.ctuning.openme.openme;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import static openscience.crowdsource.video.experiments.AppConfigService.cachedPlatformFeaturesFilePath;

/**
 * @author Daniil Efremov
 */

public class PlatformFeaturesService {


    synchronized public static void cleanuCachedPlatformFeaturesF() {
        File file = new File(cachedPlatformFeaturesFilePath);
        if (file.exists()) {
            file.delete();
        }
    }

    public static JSONObject loadPlatformFeaturesFromFile() {
            File file = new File(cachedPlatformFeaturesFilePath);
            if (file.exists()) {
                try {
                    JSONObject dict = openme.openme_load_json_file(cachedPlatformFeaturesFilePath);
                    // contract of serialisation and deserialization is not the same so i need to unwrap here original JSON
                    return dict.getJSONObject("dict");
                } catch (JSONException e) {
                    AppLogger.logMessage("ERROR could not read preloaded file " + cachedPlatformFeaturesFilePath);
                    return null;
                }
            }
        return null;
    }

    public static void savePlatformFeaturesToFile(JSONObject platformFetaturesJSONObject) {
        File scenariosFile = new File(cachedPlatformFeaturesFilePath);
        if (scenariosFile.exists()) {
            try {
                openme.openme_store_json_file(platformFetaturesJSONObject, cachedPlatformFeaturesFilePath);
            } catch (JSONException e) {
                AppLogger.logMessage("ERROR could save platform features to file " + cachedPlatformFeaturesFilePath + " because of the error " + e.getLocalizedMessage());
                return;
            }
        }
    }
}
