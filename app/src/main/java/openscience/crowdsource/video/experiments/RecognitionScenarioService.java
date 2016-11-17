package openscience.crowdsource.video.experiments;

import android.app.Activity;

import org.ctuning.openme.openme;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static openscience.crowdsource.video.experiments.AppConfigService.cachedScenariosFilePath;

/**
 * @author Daniil Efremov
 */

public class RecognitionScenarioService {

    private static List<RecognitionScenario> recognitionScenarios = new ArrayList<>();

    public static List<RecognitionScenario> getRecognitionScenarios() {
        if (recognitionScenarios.isEmpty()) {
            reloadRecognitionScenariosFromFile();
            if (recognitionScenarios.isEmpty()) {
                new ReloadScenarioAsyncTask().execute();
            }
        }
        return recognitionScenarios;
    }

    public static void add(RecognitionScenario recognitionScenario) {
        getRecognitionScenarios().add(recognitionScenario);
    }


    public static JSONObject loadScenariosJSONObjectFromFile() {
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
                recognitionScenarios.add(recognitionScenario);
            }
        } catch (JSONException e) {
            AppLogger.logMessage("Error loading scenarios from file " + e.getLocalizedMessage());
        }
    }




    public static void startDownloading(RecognitionScenario recognitionScenario) {
        if (recognitionScenario.getState() == RecognitionScenario.State.NEW) {
            AppLogger.logMessage("Downloading started for " + recognitionScenario.getTitle() + " ...");
            new LoadScenarioAsyncTask().execute(recognitionScenario);
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


}
