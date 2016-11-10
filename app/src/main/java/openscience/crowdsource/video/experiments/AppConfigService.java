package openscience.crowdsource.video.experiments;

import org.ctuning.openme.openme;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

/**
 * Created by daniil on 11/10/16.
 */

//todo implement as service using IoC
public class AppConfigService {

    private final static String APP_CONFIG_DIR = "/sdcard/openscience/"; //todo get log dir from common config service
    private final static String APP_CONFIG_FILE_PATH = APP_CONFIG_DIR + "app_config.json";

    public static final String ACKNOWLEDGE_YOUR_CONTRIBUTIONS = "acknowledge your contributions!";

    private static final String externalSDCardPath = File.separator + "sdcard";
    private static final String externalSDCardOpensciencePath = externalSDCardPath + File.separator + "openscience" + File.separator;
    private static final String externalSDCardOpenscienceTmpPath = externalSDCardOpensciencePath + File.separator + "tmp" + File.separator;
    private static final String cachedScenariosFilePath = externalSDCardOpensciencePath + "scenariosFile.json";
    private static final String cachedPlatformFeaturesFilePath = externalSDCardOpensciencePath + "platformFeaturesFile.json";


    synchronized public static void deleteTMPFiles() {
        File file = new File(externalSDCardOpenscienceTmpPath);

        if (file.exists()) {
            String deleteCmd = "rm -r " + externalSDCardOpenscienceTmpPath;
            Runtime runtime = Runtime.getRuntime();
            try {
                runtime.exec(deleteCmd);
            } catch (IOException e) {
            }
        }
    }

        synchronized public static void updateActualImagePath(String actualImagePath) {
        AppConfig appConfig = loadAppConfig();
        if (appConfig == null) {
            appConfig = new AppConfig();
        }
        appConfig.setActualImagePath(actualImagePath);
        saveAppConfig(appConfig);
    }

    synchronized public static String getActualImagePath() {
        AppConfig appConfig = loadAppConfig();
        if (appConfig == null) {
            return null;
        }
        return appConfig.getActualImagePath();
    }

    synchronized public static void updateEmail(String email) {
        AppConfig appConfig = loadAppConfig();
        if (appConfig == null) {
            appConfig = new AppConfig();
        }
        appConfig.setEmail(email);
        saveAppConfig(appConfig);
    }

    synchronized public static String getEmail() {
        AppConfig appConfig = loadAppConfig();
        if (appConfig == null) {
            return ACKNOWLEDGE_YOUR_CONTRIBUTIONS;
        }
        String email = appConfig.getEmail();
        if (email == null) {
            return ACKNOWLEDGE_YOUR_CONTRIBUTIONS;
        }
        return email;
    }


    public static void saveAppConfig(AppConfig appConfig) {
        try {
            JSONObject scenariosJSON = appConfig.toJSONObject();
            openme.openme_store_json_file(scenariosJSON, APP_CONFIG_FILE_PATH);
        } catch (JSONException e) {
            AppLogger.logMessage("ERROR could not write app config file");
        }
    }

    public static AppConfig loadAppConfig() {
        File scenariosFile = new File(APP_CONFIG_FILE_PATH);
        if (scenariosFile.exists()) {
            try {
                JSONObject dict = openme.openme_load_json_file(APP_CONFIG_FILE_PATH);
                // contract of serialisation and deserialization is not the same so i need to unwrap here original JSON
                JSONObject scenariosJSON = dict.getJSONObject("dict");
                return AppConfig.fromJSONObject(scenariosJSON);
            } catch (JSONException e) {
                AppLogger.logMessage("ERROR could not read app config file ");
            }
        }
        return null;
    }

    public static class AppConfig {
        public static final String EMAIL = "email";
        public static final String ACTUAL_IMAGE_PATH = "actual_image_path";
        private String email;
        private String actualImagePath;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getActualImagePath() {
            return actualImagePath;
        }

        public void setActualImagePath(String actualImagePath) {
            this.actualImagePath = actualImagePath;
        }

        public JSONObject toJSONObject() {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put(EMAIL, getEmail());
                jsonObject.put(ACTUAL_IMAGE_PATH, getActualImagePath());
            } catch (JSONException e) {
                AppLogger.logMessage("ERROR could not serialize app config to json format");
            }
            return jsonObject;
        }

        public static AppConfig fromJSONObject(JSONObject jsonObject) {
            AppConfig appConfig = new AppConfig();
            try {
                appConfig.setEmail(jsonObject.getString(EMAIL));
            } catch (JSONException e) {
                AppLogger.logMessage("ERROR could not deserialize app config from " + EMAIL);
            }

            try {
                appConfig.setActualImagePath(jsonObject.getString(ACTUAL_IMAGE_PATH));
            } catch (JSONException e) {
                AppLogger.logMessage("ERROR could not deserialize app config from " + ACTUAL_IMAGE_PATH);
            }
            return appConfig;
        }
    }
}
