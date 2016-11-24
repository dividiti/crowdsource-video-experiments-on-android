package openscience.crowdsource.video.experiments;

import android.os.AsyncTask;

import org.ctuning.openme.openme;
import org.json.JSONException;
import org.json.JSONObject;

import static openscience.crowdsource.video.experiments.Utils.validateReturnCode;

/**
 * @author Daniil Efremov
 */
public class ReloadScenariosAsyncTask extends AsyncTask<RecognitionScenarioService.ScenariosUpdater, String, String> {

    protected void onPostExecute(String x) {
        AppConfigService.AppConfig.State state = AppConfigService.getState();
        if (state == null || state.equals(AppConfigService.AppConfig.State.IN_PROGRESS)) {
            AppConfigService.updateState(AppConfigService.AppConfig.State.READY);
        }
    }

    protected    void onProgressUpdate(String... values) {
        if (values[0] != "") {
            AppLogger.logMessage(values[0]);
        } else if (values[1] != "") {
            AppLogger.logMessage("Error onProgressUpdate " + values[1]);
        }
    }

    @Override
    protected String doInBackground(RecognitionScenarioService.ScenariosUpdater... arg0) {
//        JSONObject platformFeatures = PlatformFeaturesService.loadPlatformFeaturesFromFile();
//        publishProgress("\nSending request to CK server to obtain available collaborative experiment scenarios for your mobile device ...\n\n");
//
//        JSONObject availableScenariosRequest = new JSONObject();
//        try {
//            String remoteServerURL = AppConfigService.getRemoteServerURL();
//            if (remoteServerURL == null) {
//                publishProgress("\n Error we could not load scenarios from Collective Knowledge server: it's not reachible ...\n\n");
//                return null;
//            }
//            availableScenariosRequest.put("remote_server_url", remoteServerURL);
//            availableScenariosRequest.put("action", "get");
//            availableScenariosRequest.put("module_uoa", "experiment.scenario.mobile");
//            availableScenariosRequest.put("email", AppConfigService.getEmail());
//            availableScenariosRequest.put("platform_features", platformFeatures);
//            availableScenariosRequest.put("out", "json");
//        } catch (JSONException e) {
//            publishProgress("\nError with JSONObject ...\n\n");
//            return null;
//        }
//
//        JSONObject responseJSONObject;
//        try {
//            responseJSONObject = openme.remote_access(availableScenariosRequest);
//        } catch (JSONException e) {
//            publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
//            return null;
//        }
//
//        if (validateReturnCode(responseJSONObject)) return null;
//        RecognitionScenarioService.saveScenariosJSONObjectToFile(responseJSONObject);
//        RecognitionScenarioService.reloadRecognitionScenariosFromFile();
        RecognitionScenarioService.loadRecognitionScenariosFromServer();
        AppConfigService.updateState(AppConfigService.AppConfig.State.READY);
        if (arg0[0] != null) {
            arg0[0].update();
        }

        publishProgress("Finished pre-loading shared scenarios for crowdsourcing!\n\n");
        publishProgress("Crowd engine is READY!\n");
        return null;
    }
}