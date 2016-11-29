package openscience.crowdsource.video.experiments;

import android.annotation.SuppressLint;

import org.ctuning.openme.openme;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import static openscience.crowdsource.video.experiments.AppConfigService.cachedScenariosFilePath;
import static openscience.crowdsource.video.experiments.AppConfigService.externalSDCardOpensciencePath;
import static openscience.crowdsource.video.experiments.AppConfigService.getSelectedRecognitionScenarioId;
import static openscience.crowdsource.video.experiments.Utils.downloadFileAndCheckMd5;
import static openscience.crowdsource.video.experiments.Utils.getCachedMD5;
import static openscience.crowdsource.video.experiments.Utils.validateReturnCode;

/**
 * @author Daniil Efremov
 */

public class RecognitionScenarioService {

    public static final String PRELOADING_TEXT = "Preloading...";
    private static ArrayList<RecognitionScenario> recognitionScenarios = new ArrayList<>();
    public static final Comparator<? super RecognitionScenario> COMPARATOR = new Comparator<RecognitionScenario>() {
        @SuppressLint("NewApi")
        @Override
        public int compare(RecognitionScenario lhs, RecognitionScenario rhs) {
            return Long.compare(lhs.getTotalFileSizeBytes().longValue(), rhs.getTotalFileSizeBytes().longValue());
        }
    };


    synchronized public static void cleanupCachedScenarios() {
        File file = new File(cachedScenariosFilePath);
        if (file.exists()) {
            file.delete();
        }
    }

    synchronized public static void deleteScenarioFiles(RecognitionScenario recognitionScenario) {
        try {
            JSONObject scenario = recognitionScenario.getRawJSON();
            JSONObject meta = scenario.getJSONObject("meta");
            JSONArray files = meta.getJSONArray("files");
            for (int j = 0; j < files.length(); j++) {
                JSONObject file = files.getJSONObject(j);
                String fileName = file.getString("filename");
                String fileDir = externalSDCardOpensciencePath + file.getString("path");
                final String targetFilePath = fileDir + File.separator + fileName;
                File fp = new File(targetFilePath);
                if (fp.exists()) {
                    fp.delete();
                    AppLogger.logMessage("Deleted local scenario file " + targetFilePath);
                }

                String md5FilePath = targetFilePath + ".md5";
                fp = new File(md5FilePath);
                if (fp.exists()) {
                    fp.delete();
                    AppLogger.logMessage("Deleted local file " + md5FilePath);
                }
                fp = new File(fileDir);
                if (fp.exists()) {
                    fp.delete();
                    AppLogger.logMessage("Deleted local scenario dir " + fileDir);
                }

            }
            recognitionScenario.setState(RecognitionScenario.State.NEW);
            recognitionScenario.setLoadScenarioFilesAsyncTask(null);
            AppLogger.logMessage("All downloaded files ware deleted for scenario " + recognitionScenario.getTitle());
        } catch (JSONException e) {
            AppLogger.logMessage("Error deleting local scenario's files " + e.getLocalizedMessage());
        }
    }

    public static void initRecognitionScenariosAsync(ScenariosUpdater updater) {
        if (recognitionScenarios.isEmpty()) {
            long startReloading = new Date().getTime();
            AppLogger.logMessage("Start scenarios reloading ...");
            reloadRecognitionScenariosFromFile();
            if (recognitionScenarios.isEmpty()) {
                AppConfigService.updateState(AppConfigService.AppConfig.State.PRELOAD);
                recognitionScenarios.add(getPreloadingRecognitionScenario());
                new ReloadScenariosAsyncTask().execute(updater);
                return;
            }
            AppLogger.logMessage("Scenarios reloaded for " + (new Date().getTime() - startReloading) + " ms");
        }
        Collections.sort(recognitionScenarios, COMPARATOR);
        updater.update();
    }


    public static void loadRecognitionScenariosFromServer() {
        JSONObject platformFeatures = PlatformFeaturesService.loadPlatformFeatures();
        if (platformFeatures == null) {
            AppLogger.logMessage("Error with loading platform feature. Scenarios could not be loaded...");
            return;
        }
        AppLogger.logMessage("\nSending request to CK server to obtain available collaborative experiment scenarios for your mobile device ...\n\n");
        JSONObject availableScenariosRequest = new JSONObject();
        try {
            String remoteServerURL = AppConfigService.getRemoteServerURL();
            if (remoteServerURL == null) {
                AppLogger.logMessage("\n Error we could not load scenarios from Collective Knowledge server: it's not reachible ...\n\n");
                return;
            }
            availableScenariosRequest.put("remote_server_url", remoteServerURL);
            availableScenariosRequest.put("action", "get");
            availableScenariosRequest.put("module_uoa", "experiment.scenario.mobile");
            availableScenariosRequest.put("email", AppConfigService.getEmail());
            availableScenariosRequest.put("platform_features", platformFeatures);
            availableScenariosRequest.put("out", "json");
        } catch (JSONException e) {
            AppLogger.logMessage("\nError with JSONObject ...\n\n");
            return;
        }

        JSONObject responseJSONObject;
        try {
            responseJSONObject = openme.remote_access(availableScenariosRequest);
        } catch (JSONException e) {
            AppLogger.logMessage("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
            return;
        }

        if (validateReturnCode(responseJSONObject)) {
            return;
        }
        RecognitionScenarioService.saveScenariosJSONObjectToFile(responseJSONObject);
        RecognitionScenarioService.reloadRecognitionScenariosFromFile();
        AppConfigService.updateState(AppConfigService.AppConfig.State.READY);

        AppLogger.logMessage("Finished pre-loading shared scenarios for crowdsourcing!\n\n");
        AppLogger.logMessage("Crowd engine is READY!\n");
    }



    public static ArrayList<RecognitionScenario> getSortedRecognitionScenarios() {
        if (recognitionScenarios.isEmpty()) {
            long startReloading = new Date().getTime();
            AppLogger.logMessage("Start scenarios reloading ...");
            reloadRecognitionScenariosFromFile();
            if (recognitionScenarios.isEmpty()) {
                AppConfigService.updateState(AppConfigService.AppConfig.State.PRELOAD);
                recognitionScenarios.add(getPreloadingRecognitionScenario());
                new ReloadScenariosAsyncTask().execute(new ScenariosUpdater() {
                    @Override
                    public void update() {
                        //do nothing in this case
                    }
                });
            }
            AppLogger.logMessage("Scenarios reloaded for " + (new Date().getTime() - startReloading) + " ms");
            AppConfigService.updateState(AppConfigService.AppConfig.State.READY);
        }
        Collections.sort(recognitionScenarios, COMPARATOR); // todo it's better to do only once at init
        return recognitionScenarios;
    }

    synchronized public static RecognitionScenario getSelectedRecognitionScenario() {
        int selectedRecognitionScenarioId = getSelectedRecognitionScenarioId();
        ArrayList<RecognitionScenario> sortedRecognitionScenarios = RecognitionScenarioService.getSortedRecognitionScenarios();
        if (selectedRecognitionScenarioId < sortedRecognitionScenarios.size()) {
            return sortedRecognitionScenarios.get(selectedRecognitionScenarioId);
        }
        return getPreloadingRecognitionScenario();
    }

    synchronized public static RecognitionScenario getPreloadingRecognitionScenario() {
        RecognitionScenario emptyRecognitionScenario = new RecognitionScenario();
        emptyRecognitionScenario.setTitle(PRELOADING_TEXT);
        emptyRecognitionScenario.setTotalFileSize("");
        emptyRecognitionScenario.setTotalFileSizeBytes(Long.valueOf(0));
        return emptyRecognitionScenario;
    }

    static JSONObject loadScenariosJSONObjectFromFile() {
        File scenariosFile = new File(cachedScenariosFilePath);
        if (scenariosFile.exists()) {
            try {
                JSONObject dict = openme.openme_load_json_file(cachedScenariosFilePath);
                // contract of serialisation and deserialization is not the same so i need to unwrap here original JSON
                return dict.getJSONObject("dict");
            } catch (JSONException e) {
                AppLogger.logMessage("ERROR could not read preloaded file " + cachedScenariosFilePath);
                return null;
            }
        }
        return null;
    }

    public static void saveScenariosJSONObjectToFile(JSONObject scenariousJSON) {
        try {
            openme.openme_store_json_file(scenariousJSON, cachedScenariosFilePath);
        } catch (JSONException e) {
            AppLogger.logMessage("ERROR could save scenarios to file " + cachedScenariosFilePath + " because of the error " + e.getLocalizedMessage());
            return;
        }
    }

    public static void reloadRecognitionScenariosFromFile() {
        try {

            JSONObject scenariosJSON = loadScenariosJSONObjectFromFile();
            if (scenariosJSON == null) {
                return;
            }
            JSONArray scenarios = scenariosJSON.getJSONArray("scenarios");
            if (scenarios.length() == 0) {
                return;
            }
            recognitionScenarios.clear();
            for (int i = 0; i < scenarios.length(); i++) {
                JSONObject scenario = scenarios.getJSONObject(i);


                final String module_uoa = scenario.getString("module_uoa");
                final String dataUID = scenario.getString("data_uid");
                final String data_uoa = scenario.getString("data_uoa");

                scenario.getJSONObject("search_dict");
                scenario.getString("ignore_update");
                scenario.getString("search_string");
                JSONObject meta = scenario.getJSONObject("meta");
                String title = meta.getString("title");
                String sizeMB = "";
                Long sizeBytes = Long.valueOf(0);
                try {
                    String sizeB = scenario.getString("total_file_size");
                    sizeBytes = Long.valueOf(sizeB);
                    sizeMB = Utils.bytesIntoHumanReadable(sizeBytes);
                } catch (JSONException e) {
                    AppLogger.logMessage("Warn loading scenarios from file " + e.getLocalizedMessage());
                }

                final RecognitionScenario recognitionScenario = new RecognitionScenario();
                recognitionScenario.setModuleUOA(module_uoa);
                recognitionScenario.setDataUOA(data_uoa);
                recognitionScenario.setRawJSON(scenario);
                recognitionScenario.setTitle(title);
                recognitionScenario.setTotalFileSize(sizeMB);
                recognitionScenario.setTotalFileSizeBytes(sizeBytes);
                JSONArray files = meta.getJSONArray("files");
                if (isFilesLoaded(files)) {
                    AppLogger.logMessage("All files already loaded for scenario " + recognitionScenario.getTitle());
                    recognitionScenario.setState(RecognitionScenario.State.DOWNLOADED);
                    recognitionScenario.setLoadScenarioFilesAsyncTask(null);
                    recognitionScenario.setDefaultImagePath(getLocalDefaultImageFilePath(files));
                } else {
                    AppLogger.logMessage("Files not loaded yet for scenario " + recognitionScenario.getTitle());
                    recognitionScenario.setState(RecognitionScenario.State.NEW);
                    recognitionScenario.setLoadScenarioFilesAsyncTask(null);
                    if (i == AppConfigService.getSelectedRecognitionScenarioId() && AppConfigService.getActualImagePath() == null) {
                        String imageFile = downloadImageAndGetLocalFilePath(files);
                        if (imageFile != null) {
                            recognitionScenario.setDefaultImagePath(imageFile);
                            AppConfigService.updateActualImagePath(imageFile);
                        }
                    }
                }
                recognitionScenarios.add(recognitionScenario);
            }
        } catch (JSONException e) {
            AppLogger.logMessage("Error loading scenarios from file " + e.getLocalizedMessage());
        }
    }




    public static void startDownloading(RecognitionScenario recognitionScenario) {
        if (recognitionScenario.getState() == RecognitionScenario.State.NEW) {
            AppLogger.logMessage("Downloading started for " + recognitionScenario.getTitle() + " ...");
            recognitionScenario.setDownloadedTotalFileSizeBytes(new Long(0));

            LoadScenarioFilesAsyncTask loadScenarioFilesAsyncTask = new LoadScenarioFilesAsyncTask();
            loadScenarioFilesAsyncTask.execute(recognitionScenario);
            recognitionScenario.setLoadScenarioFilesAsyncTask(loadScenarioFilesAsyncTask);
        } else if (recognitionScenario.getState() == RecognitionScenario.State.DOWNLOADING_IN_PROGRESS) {
            // mostly debug log message
            AppLogger.logMessage("Warning: Download for " + recognitionScenario.getTitle() + " already started...");
        } else if (recognitionScenario.getState() == RecognitionScenario.State.DOWNLOADED) {
            // mostly debug log message
            AppLogger.logMessage("Warning: Download for " + recognitionScenario.getTitle() + " already complete...");
        }
    }


    public static void updateProgress(final RecognitionScenario recognitionScenario) {
        RecognitionScenarioService.Updater buttonUpdater = recognitionScenario.getButtonUpdater();
        if (buttonUpdater != null) {
            buttonUpdater.update(recognitionScenario);
        }
        RecognitionScenarioService.Updater progressUpdater = recognitionScenario.getProgressUpdater();
        if (progressUpdater!=null) {
            progressUpdater.update(recognitionScenario);
        }
    }


    public interface Updater {
        void update(RecognitionScenario recognitionScenario);
    }

    public interface ScenariosUpdater {
        void update();
    }


    private static boolean isFilesLoaded(JSONArray files) {
        try {
            for (int j = 0; j < files.length(); j++) {
                JSONObject file = files.getJSONObject(j);
                String fileName = file.getString("filename");
                String fileDir = externalSDCardOpensciencePath + file.getString("path");
                File fp = new File(fileDir);
                if (!fp.exists()) {
                    if (!fp.mkdirs()) {
                        AppLogger.logMessage("\nError creating dir (" + fileDir + ") ...\n\n");
                        return false;
                    }
                }

                final String targetFilePath = fileDir + File.separator + fileName;
                String md5 = file.getString("md5");
                String existedlocalPathMD5 = getCachedMD5(targetFilePath);
                if (existedlocalPathMD5 == null || !existedlocalPathMD5.equalsIgnoreCase(md5)) {
                    return false;
                }

            }
        } catch (JSONException e) {
            AppLogger.logMessage("Error checking files for scenario");
            return false;

        }
        return true;
    }


    private static String downloadImageAndGetLocalFilePath(JSONArray files) {
        try {
            AppLogger.logMessage("Start downloading default scenario image ...");
            for (int j = 0; j < files.length(); j++) {
                JSONObject file = files.getJSONObject(j);
                String fileName = file.getString("filename");
                String isDefaulImage = null;
                try {
                    isDefaulImage = file.getString("default_image");
                } catch (JSONException e) {
                    // optional
                }
                if (isDefaulImage != null && isDefaulImage.equalsIgnoreCase("yes")) {
                    String fileDir = externalSDCardOpensciencePath + file.getString("path");
                    File fp = new File(fileDir);
                    if (!fp.exists()) {
                        if (!fp.mkdirs()) {
                            AppLogger.logMessage("\nError creating dir (" + fileDir + ") ...\n\n");
                            return null;
                        }
                    }

                    final String targetFilePath = fileDir + File.separator + fileName;
                    String md5 = file.getString("md5");
                    String existedlocalPathMD5 = getCachedMD5(targetFilePath);
                    if (existedlocalPathMD5 == null || !existedlocalPathMD5.equalsIgnoreCase(md5)) {
                        String finalTargetFilePath = fileDir + File.separator + fileName;
                        String url = file.getString("url");
                        downloadFileAndCheckMd5(url, finalTargetFilePath, md5, new MainActivity.ProgressPublisher() {
                            @Override
                            public void setPercent(int percent) {
                            }

                            @Override
                            public void addBytes(long bytes) {

                            }

                            @Override
                            public void println(String text) {
                                AppLogger.logMessage(text);
                            }
                        });
                        AppLogger.logMessage("Default scenario image is downloaded");
                        return finalTargetFilePath;
                    }
                }
            }
        } catch (JSONException e) {
            AppLogger.logMessage("Error downloading default scenario image " + e.getLocalizedMessage());
            return null;

        }
        return null;
    }


    private static String getLocalDefaultImageFilePath(JSONArray files) {
        try {
            for (int j = 0; j < files.length(); j++) {
                JSONObject file = files.getJSONObject(j);
                String fileName = file.getString("filename");
                String isDefaulImage = null;
                try {
                    file.getString("default_image");
                } catch (JSONException e) {
                    // optional
                }
                if (isDefaulImage != null && isDefaulImage.equalsIgnoreCase("yes")) {
                    String fileDir = externalSDCardOpensciencePath + file.getString("path");
                    File fp = new File(fileDir);
                    if (!fp.exists()) {
                        if (!fp.mkdirs()) {
                            AppLogger.logMessage("\nError creating dir (" + fileDir + ") ...\n\n");
                            return null;
                        }
                    }
                    return fileDir + File.separator + fileName;
                }
            }
        } catch (JSONException e) {
            AppLogger.logMessage("Error checking files for scenario");
            return null;

        }
        return null;
    }

    private static String wrapKey(String key) {
        return "<font color='#ffffff'><b>" + key + "</b></font>";
    }
    private static String wrapValue(String value) {
        return "<font color='#64ffda'><b>" + value + "</b></font>";
    }


    public static String getScenarioDescriptionHTML(RecognitionScenario recognitionScenario) throws JSONException {
        StringBuilder descriptionBuilder = new StringBuilder();
//        return recognitionScenario.getRawJSON().toString(4);

        JSONObject scenario = recognitionScenario.getRawJSON();
        String indend = "";
        String delim = ":&nbsp;&nbsp;";
        String lineEnd = "<br>";
        String indentSingle = "&nbsp;&nbsp;";
        descriptionBuilder.append(indend).append(wrapKey("Module UOA")).append(delim).append(scenario.getString("module_uoa")).append(lineEnd);
        descriptionBuilder.append(indend).append(wrapKey("Data UID")).append(delim).append(scenario.getString("data_uid")).append(lineEnd);
        descriptionBuilder.append(indend).append(wrapKey("Data UOA")).append(delim).append(scenario.getString("data_uoa")).append(lineEnd);

        descriptionBuilder.append(indend).append(wrapKey("Meta")).append(delim).append(lineEnd);
        indend = indentSingle;

        JSONObject meta = scenario.getJSONObject("meta");
        descriptionBuilder.append(indend).append(wrapKey("Title")).append(delim).append(meta.getString("title")).append(lineEnd);
        descriptionBuilder.append(indend).append(wrapKey("Engine")).append(delim).append(meta.getString("engine")).append(lineEnd);

        String sizeMB = "";
        Long sizeBytes = Long.valueOf(0);
        try {
            String sizeB = scenario.getString("total_file_size");
            sizeBytes = Long.valueOf(sizeB);
            sizeMB = Utils.bytesIntoHumanReadable(sizeBytes);
        } catch (JSONException e) {
            AppLogger.logMessage("Warn loading scenarios from file " + e.getLocalizedMessage());
        }
        descriptionBuilder.append(indend).append(wrapKey("Total files size")).append(delim).append(sizeMB).append(lineEnd);

        indend = indend + indentSingle;
        JSONArray files = meta.getJSONArray("files");
        for (int j = 0; j < files.length(); j++) {
            JSONObject file = files.getJSONObject(j);
            String fileName = file.getString("filename");
            descriptionBuilder.append(indend).append(wrapKey("File name")).append(delim).append(fileName).append(lineEnd);

            String indendFile = indend + indentSingle;
            sizeBytes = Long.valueOf(0);
            try {
                String sizeB = file.getString("file_size");
                sizeBytes = Long.valueOf(sizeB);
                sizeMB = Utils.bytesIntoHumanReadable(sizeBytes);
            } catch (JSONException e) {
                AppLogger.logMessage("Warn loading scenarios from file " + e.getLocalizedMessage());
            }
            descriptionBuilder.append(indendFile).append(wrapKey("File size")).append(delim).append(wrapValue(sizeMB)).append(lineEnd);
            descriptionBuilder.append(indendFile).append(wrapKey("Path")).append(delim).append(wrapValue(file.getString("path"))).append(lineEnd);
            descriptionBuilder.append(indendFile).append(wrapKey("URL")).append(delim).append(wrapValue(file.getString("url"))).append(lineEnd);
            descriptionBuilder.append(indendFile).append(wrapKey("MD5")).append(delim).append(wrapValue(file.getString("md5"))).append(lineEnd);
            try {
                String supported_abi = file.getString("supported_abi");
                descriptionBuilder.append(indendFile).append(wrapKey("Supported ABI")).append(delim).append(wrapValue(supported_abi)).append(lineEnd);
            } catch (JSONException e) {
                // optional
            }
            try {
                String from_data_uoa = file.getString("from_data_uoa");
                descriptionBuilder.append(indendFile).append(wrapKey("From Data UOA")).append(delim).append(wrapValue(from_data_uoa)).append(lineEnd);
            } catch (JSONException e) {
                // optional
            }
            try {
                String library = file.getString("library");
                descriptionBuilder.append(indendFile).append(wrapKey("Is library")).append(delim).append(wrapValue(library)).append(lineEnd);
            } catch (JSONException e) {
                // optional
            }
            try {
                String executable = file.getString("executable");
                descriptionBuilder.append(indendFile).append(wrapKey("Is executable")).append(delim).append(wrapValue(executable)).append(lineEnd);
            } catch (JSONException e) {
                // optional
            }
            descriptionBuilder.append(lineEnd);
        }

        return descriptionBuilder.toString();
    }


}
