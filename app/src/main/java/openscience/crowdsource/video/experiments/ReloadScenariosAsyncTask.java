package openscience.crowdsource.video.experiments;

import android.os.AsyncTask;

import org.ctuning.openme.openme;
import org.json.JSONException;
import org.json.JSONObject;

import static openscience.crowdsource.video.experiments.Utils.validateReturnCode;

/**
 * Async process reloads scenarios from remote server
 * Downloaded files is not removed for existed scenarios
 *
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
        RecognitionScenarioService.loadRecognitionScenariosFromServer();
        AppConfigService.updateState(AppConfigService.AppConfig.State.READY);
        if (arg0[0] != null) {
            arg0[0].update();
        }
        if (RecognitionScenarioService.isRecognitionScenariosLoaded()) {
            publishProgress("Crowd engine is READY!\n");
        }
        return null;
    }
}