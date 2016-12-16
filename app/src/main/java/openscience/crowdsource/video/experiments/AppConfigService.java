package openscience.crowdsource.video.experiments;

import android.app.Activity;

import org.ctuning.openme.openme;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static openscience.crowdsource.video.experiments.Utils.get_shared_computing_resource;

/**
 * Application configuration Service
 * provides acess for basic app configuration state from activities and async tasks as well
 *
 * @author Daniil Efremov
 */
//todo it's better to implement all services using IoC
public class AppConfigService {

    private final static String APP_CONFIG_DIR = "/sdcard/openscience/"; //todo get log dir from common config service
    private final static String APP_CONFIG_FILE_PATH = APP_CONFIG_DIR + "app_config.json";

    public final static String URL_SDK = "http://github.com/ctuning/ck";
    public final static String URL_ABOUT = "https://github.com/ctuning/ck/wiki/Advanced_usage_crowdsourcing";
    public final static String URL_CROWD_RESULTS = "http://cknowledge.org/repo/web.php?action=index&module_uoa=wfe&native_action=show&native_module_uoa=program.optimization&scenario=experiment.bench.dnn.mobile";
    public final static String URL_USERS = "http://cTuning.org/crowdtuning-timeline";

    public static final String PLEASE_WAIT = "Please wait ...";

    public static final String ACKNOWLEDGE_YOUR_CONTRIBUTIONS = "acknowledge your contributions!";

    public static final String path_opencl = "/system/vendor/lib/libOpenCL.so";

    public static final String url_cserver = "http://cTuning.org/shared-computing-resources-json/ck.json";

    public static final String repo_uoa = "upload";

    public static final String COMMAND_CHMOD_744 = "/system/bin/chmod 744";


    public static final String externalSDCardPath = File.separator + "sdcard";
    public static final String externalSDCardOpensciencePath = externalSDCardPath + File.separator + "openscience" + File.separator;
    public static final String externalSDCardOpenscienceTmpPath = externalSDCardOpensciencePath + File.separator + "tmp" + File.separator;
    public static final String cachedScenariosFilePath = externalSDCardOpensciencePath + "scenariosFile.json";
    public static final String cachedPlatformFeaturesFilePath = externalSDCardOpensciencePath + "platformFeaturesFile.json";
    public static final String SELECTED_RECOGNITION_SCENARIO = "selected_recognition_scenario";
    public static final String CROWDSOURCE_VIDEO_EXPERIMENTS_ON_ANDROID_PREFERENCES = "crowdsource-video-experiments-on-android.preferences";
    public static final String SHARED_PREFERENCES = "sharedPreferences";


    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = CPU_COUNT + 2;
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 2;
    private static final int KEEP_ALIVE = 1;

    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(Runnable r) {
            return new Thread(r, "AsyncTask #" + mCount.getAndIncrement());
        }
    };

    private static final BlockingQueue<Runnable> sPoolWorkQueue =
            new LinkedBlockingQueue<Runnable>(128);

    /**
     * An {@link Executor} that can be used to execute tasks in parallel.
     */
    public static final Executor THREAD_POOL_EXECUTOR
            = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE,
            TimeUnit.SECONDS, sPoolWorkQueue, sThreadFactory);

    private static Updater previewRecognitionTextUpdater;

    synchronized public static void initAppConfig(Activity activity) {
        initLocalAppPath(activity);
    }

    private static void initRemoteServerURL() {
        String remoteServerURL = get_shared_computing_resource(url_cserver);
        AppConfigService.updateRemoteServerURL(remoteServerURL);
    }

    private static void initLocalAppPath(Activity activity) {
        String localAppPath = activity.getFilesDir().toString()+ File.separator + "ck-crowd";
        File fp = new File(localAppPath);
        if (!fp.exists()) {
            if (!fp.mkdirs()) {
                AppLogger.logMessage("\nERROR: can't create directory for local tmp files!\n");
                return;
            }
        }
        AppConfig appConfig = loadAppConfig();
        if (appConfig == null) {
            appConfig = new AppConfig();
        }
        appConfig.setLocalAppPath(localAppPath);
        saveAppConfig(appConfig);
    }

    synchronized public static String getLocalAppPath() {
        AppConfig appConfig = loadAppConfig();
        if (appConfig == null) {
            return null;
        }
        return appConfig.getLocalAppPath();
    }

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


    synchronized public static void updatePreviewRecognitionText(String value) {
        AppConfig appConfig = loadAppConfig();
        if (appConfig == null) {
            appConfig = new AppConfig();
        }
        if (previewRecognitionTextUpdater != null) {
            if (value != null) {
                final String[] predictions = value.split("[\\r\\n]+");
                if (predictions.length >= 2) {
                    String previewRecognitionText = predictions[1];
                    appConfig.setPreviewRecognitionText(previewRecognitionText);
                    previewRecognitionTextUpdater.update(previewRecognitionText);
                }
            } else {
                previewRecognitionTextUpdater.update("");
                appConfig.setPreviewRecognitionText(null);
            }
        }
        saveAppConfig(appConfig);
    }

    synchronized public static String getPreviewRecognitionText() {
        AppConfig appConfig = loadAppConfig();
        if (appConfig == null) {
            return null;
        }
        return appConfig.getPreviewRecognitionText();
    }


    public static void registerPreviewRecognitionText(Updater updaterNew) {
        previewRecognitionTextUpdater = updaterNew;
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
        appConfig.setRemoteServerURL(value);
        saveAppConfig(appConfig);
    }

    /**
     * Returns remote server URL or detect it using default web service
     * Do not use in UI thread becouse of network usage!
     *
     * @return
     */
    synchronized public static String getRemoteServerURL() {
        AppConfig appConfig = loadAppConfig();
        if (appConfig == null || appConfig.getRemoteServerURL() == null) {
            initRemoteServerURL();
            appConfig = loadAppConfig();
        }
        return appConfig.getRemoteServerURL();
    }

    synchronized public static void updateSelectedRecognitionScenario(int value) {
        AppConfig appConfig = loadAppConfig();
        if (appConfig == null) {
            appConfig = new AppConfig();
        }
        appConfig.setSelectedRecognitionScenario(value);
        saveAppConfig(appConfig);
    }

    synchronized public static int getSelectedRecognitionScenarioId() {
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
            return "";
        }
        String email = appConfig.getEmail();
        if (email == null) {
            return "";
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

        public static final String PREVIEW_RECOGNITION_TEXT = "preview_recognition_text";

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
        public static final String LOCAL_APP_PATH = "local_app_path";

        private String email;
        private String actualImagePath;

        private String remoteServerURL;
        private String recognitionResultText;
        private String dataUID;
        private String behaviorUID;
        private String crowdUID;
        private int selectedRecognitionScenario;
        private State state;
        private String localAppPath;
        private String previewRecognitionText;

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

        public String getLocalAppPath() {
            return localAppPath;
        }

        public void setLocalAppPath(String localAppPath) {
            this.localAppPath = localAppPath;
        }

        public String getPreviewRecognitionText() {
            return previewRecognitionText;
        }

        public void setPreviewRecognitionText(String previewRecognitionText) {
            this.previewRecognitionText = previewRecognitionText;
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
                jsonObject.put(LOCAL_APP_PATH, getLocalAppPath());
                jsonObject.put(PREVIEW_RECOGNITION_TEXT, getPreviewRecognitionText());
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

            try {
                appConfig.setLocalAppPath(jsonObject.getString(LOCAL_APP_PATH));
            } catch (JSONException e) {
                // optional param
            }

            try {
                appConfig.setPreviewRecognitionText(jsonObject.getString(PREVIEW_RECOGNITION_TEXT));
            } catch (JSONException e) {
                // optional param
            }
            return appConfig;
        }
    }

    public interface Updater {
        void update(String message);
    }
}
