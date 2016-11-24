package openscience.crowdsource.video.experiments;

import android.os.AsyncTask;

/**
 * @author Daniil Efremov
 */
public class UpdatePlatformFeaturesAsyncTask extends AsyncTask<String, String, String> {

    protected void onPostExecute(String x) {
        AppConfigService.AppConfig.State state = AppConfigService.getState();
        if (state == null || state.equals(AppConfigService.AppConfig.State.IN_PROGRESS)) {
            AppConfigService.updateState(AppConfigService.AppConfig.State.READY);
        }
    }

    /*************************************************************************/
    protected void onProgressUpdate(String... values) {
        if (values[0] != "") {
            AppLogger.logMessage(values[0]);
        } else if (values[1] != "") {
            AppLogger.logMessage("Error onProgressUpdate " + values[1]);
        }
    }

    /*************************************************************************/
    @Override
    protected String doInBackground(String... arg0) {
        PlatformFeaturesService.loadPlatformFeaturesFromServer();
        return null;
    }
}