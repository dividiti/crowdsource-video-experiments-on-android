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
    public static final String SELECTED_RECOGNITION_SCENARIO = "selected_recognition_scenario";


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
        updateActualImagePath(null);
        updateState(AppConfig.State.READY);
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

    synchronized public static void updateRecognitionResultText(String value) {
        AppConfig appConfig = loadAppConfig();
        if (appConfig == null) {
            appConfig = new AppConfig();
        }
        appConfig.setRecognitionResultText(value);
        saveAppConfig(appConfig);
    }

    synchronized public static String getRecognitionResultText() {
        AppConfig appConfig = loadAppConfig();
        if (appConfig == null) {
            return null;
        }
        return appConfig.getRecognitionResultText();
    }

    synchronized public static void updateDataUID(String value) {
        AppConfig appConfig = loadAppConfig();
        if (appConfig == null) {
            appConfig = new AppConfig();
        }
        appConfig.setDataUID(value);
        saveAppConfig(appConfig);
    }

    synchronized public static String getDataUID() {
        AppConfig appConfig = loadAppConfig();
        if (appConfig == null) {
            return null;
        }
        return appConfig.getDataUID();
    }

    synchronized public static void updateBehaviorUID(String value) {
        AppConfig appConfig = loadAppConfig();
        if (appConfig == null) {
            appConfig = new AppConfig();
        }
        appConfig.setBehaviorUID(value);
        saveAppConfig(appConfig);
    }

    synchronized public static String getBehaviorUID() {
        AppConfig appConfig = loadAppConfig();
        if (appConfig == null) {
            return null;
        }
        return appConfig.getBehaviorUID();
    }

    synchronized public static void updateCrowdUID(String value) {
        AppConfig appConfig = loadAppConfig();
        if (appConfig == null) {
            appConfig = new AppConfig();
        }
        appConfig.setCrowdUID(value);
        saveAppConfig(appConfig);
    }

    synchronized public static String getCrowdUID() {
        AppConfig appConfig = loadAppConfig();
        if (appConfig == null) {
            return null;
        }
        return appConfig.getCrowdUID();
    }

    synchronized public static void updateRemoteServerURL(String value) {
        AppConfig appConfig = loadAppConfig();
        if (appConfig == null) {
            appConfig = new AppConfig();
        }
        appConfig.setActualImagePath(value);
        saveAppConfig(appConfig);
    }

    synchronized public static String getRemoteServerURL() {
        AppConfig appConfig = loadAppConfig();
        if (appConfig == null) {
            return null;
        }
        return appConfig.getActualImagePath();
    }

    synchronized public static void updateSelectedRecognitionScenario(int value) {
        AppConfig appConfig = loadAppConfig();
        if (appConfig == null) {
            appConfig = new AppConfig();
        }
        appConfig.setSelectedRecognitionScenario(value);
        saveAppConfig(appConfig);
    }

    synchronized public static int getSelectedRecognitionScenario() {
        AppConfig appConfig = loadAppConfig();
        if (appConfig == null) {
            return 0;
        }
        return appConfig.getSelectedRecognitionScenario();
    }

    synchronized public static void updateEmail(String email) {
        AppConfig appConfig = loadAppConfig();
        if (appConfig == null) {
            appConfig = new AppConfig();
        }
        appConfig.setEmail(email);
        saveAppConfig(appConfig);
    }

    synchronized public static AppConfig.State getState() {
        AppConfig appConfig = loadAppConfig();
        if (appConfig == null) {
            return AppConfig.State.READY;
        }
        AppConfig.State state = appConfig.getState();
        if (state == null) {
            return AppConfig.State.READY;
        }
        return state;
    }

    synchronized public static void updateState(AppConfig.State state) {
        AppConfig appConfig = loadAppConfig();
        if (appConfig == null) {
            appConfig = new AppConfig();
        }
        appConfig.setState(state);
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

        public enum State {
            PRELOAD,
            READY,
            IN_PROGRESS,
            RESULT
        }

        public static final String EMAIL = "email";
        public static final String ACTUAL_IMAGE_PATH = "actual_image_path";
        public static final String REMOTE_SERVER_URL = "remote_server_url";
        public static final String RECOGNITION_RESULT_TEXT = "recognition_result_text";
        public static final String DATA_UID = "data_uid";
        public static final String CROWD_ID = "crowd_id";
        public static final String BEHAVIOR_UID = "behavior_uid";
        public static final String STATE_PARAM = "state";

        private String email;
        private String actualImagePath;

        private String remoteServerURL;
        private String recognitionResultText;
        private String dataUID;
        private String behaviorUID;
        private String crowdUID;
        private int selectedRecognitionScenario;
        private State state;


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

        public String getRemoteServerURL() {
            return remoteServerURL;
        }

        public void setRemoteServerURL(String remoteServerURL) {
            this.remoteServerURL = remoteServerURL;
        }

        public String getRecognitionResultText() {
            return recognitionResultText;
        }

        public void setRecognitionResultText(String recognitionResultText) {
            this.recognitionResultText = recognitionResultText;
        }

        public String getDataUID() {
            return dataUID;
        }

        public void setDataUID(String dataUID) {
            this.dataUID = dataUID;
        }

        public String getBehaviorUID() {
            return behaviorUID;
        }

        public void setBehaviorUID(String behaviorUID) {
            this.behaviorUID = behaviorUID;
        }

        public String getCrowdUID() {
            return crowdUID;
        }

        public void setCrowdUID(String crowdUID) {
            this.crowdUID = crowdUID;
        }

        public int getSelectedRecognitionScenario() {
            return selectedRecognitionScenario;
        }

        public void setSelectedRecognitionScenario(int selectedRecognitionScenario) {
            this.selectedRecognitionScenario = selectedRecognitionScenario;
        }

        public State getState() {
            return state;
        }

        public void setState(State state) {
            this.state = state;
        }

        public JSONObject toJSONObject() {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put(EMAIL, getEmail());
                jsonObject.put(ACTUAL_IMAGE_PATH, getActualImagePath());
                jsonObject.put(REMOTE_SERVER_URL, getRemoteServerURL());
                jsonObject.put(RECOGNITION_RESULT_TEXT, getRecognitionResultText());
                jsonObject.put(DATA_UID, getDataUID());
                jsonObject.put(BEHAVIOR_UID, getBehaviorUID());
                jsonObject.put(CROWD_ID, getCrowdUID());
                jsonObject.put(SELECTED_RECOGNITION_SCENARIO, getSelectedRecognitionScenario());
                jsonObject.put(STATE_PARAM, getState() == null? null : getState().name());
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
                // optional param
            }

            try {
                appConfig.setActualImagePath(jsonObject.getString(ACTUAL_IMAGE_PATH));
            } catch (JSONException e) {
                // optional param
            }

            try {
                appConfig.setRemoteServerURL(jsonObject.getString(REMOTE_SERVER_URL));
            } catch (JSONException e) {
                // optional param
            }

            try {
                appConfig.setRecognitionResultText(jsonObject.getString(RECOGNITION_RESULT_TEXT));
            } catch (JSONException e) {
                // optional param
            }

            try {
                appConfig.setDataUID(jsonObject.getString(DATA_UID));
            } catch (JSONException e) {
                // optional param
            }

            try {
                appConfig.setBehaviorUID(jsonObject.getString(BEHAVIOR_UID));
            } catch (JSONException e) {
                // optional param
            }

            try {
                appConfig.setCrowdUID(jsonObject.getString(CROWD_ID));
            } catch (JSONException e) {
                // optional param
            }

            try {
                appConfig.setSelectedRecognitionScenario(jsonObject.getInt(SELECTED_RECOGNITION_SCENARIO));
            } catch (JSONException e) {
                // optional param
            }

            try {
                String stateName = jsonObject.getString(STATE_PARAM);
                if (stateName !=null) {
                    appConfig.setState(State.valueOf(stateName));
                } else {
                    appConfig.setState(null);
                }
            } catch (JSONException e) {
                // optional param
            }
            return appConfig;
        }
    }
}
