package openscience.crowdsource.video.experiments;

import android.os.AsyncTask;

import org.ctuning.openme.openme;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by daniil on 11/16/16.
 */
class RemoteCallTask extends AsyncTask<JSONObject, String, JSONObject> {

    private Exception exception;


    protected JSONObject doInBackground(JSONObject... requests) {
        try {
            JSONObject response = openme.remote_access(requests[0]);
            if (validateReturnCode(response)) {
                publishProgress("\nError sending correct answer to server ...\n\n");
            }

            int responseCode = 0;
            if (!response.has("return")) {
                publishProgress("\nError obtaining key 'return' from OpenME output ...\n\n");
                return response;
            }

            try {
                Object rx = response.get("return");
                if (rx instanceof String) responseCode = Integer.parseInt((String) rx);
                else responseCode = (Integer) rx;
            } catch (JSONException e) {
                publishProgress("\nError obtaining key 'return' from OpenME output (" + e.getMessage() + ") ...\n\n");
                return response;
            }

            if (responseCode > 0) {
                String err = "";
                try {
                    err = (String) response.get("error");
                } catch (JSONException e) {
                    publishProgress("\nError obtaining key 'error' from OpenME output (" + e.getMessage() + ") ...\n\n");
                    return response;
                }

                publishProgress("\nProblem accessing CK server: " + err + "\n");
                return response;
            }

            publishProgress("\nSuccessfully added correct answer to Collective Knowledge!\n\n");

            return response;
        } catch (JSONException e) {
            publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
            return null;
        } catch (Exception e) {
            this.exception = e;

            return null;
        }
    }

    protected void onProgressUpdate(String... values) {
        if (values[0] != "") {
            AppLogger.logMessage(values[0]);
        } else if (values[1] != "") {
            AppLogger.logMessage("Error");
        }
    }

    protected void onPostExecute(JSONObject response) {
        // TODO: check this.exception
        // TODO: do something with the response
    }

    private boolean validateReturnCode(JSONObject r) {
        int rr = 0;
        if (!r.has("return")) {
            publishProgress("\nError obtaining key 'return' from OpenME output ...\n\n");
            return true;
        }

        try {
            Object rx = r.get("return");
            if (rx instanceof String) rr = Integer.parseInt((String) rx);
            else rr = (Integer) rx;
        } catch (JSONException e) {
            publishProgress("\nError obtaining key 'return' from OpenME output (" + e.getMessage() + ") ...\n\n");
            return true;
        }

        if (rr > 0) {
            String err = "";
            try {
                err = (String) r.get("error");
            } catch (JSONException e) {
                publishProgress("\nError obtaining key 'error' from OpenME output (" + e.getMessage() + ") ...\n\n");
                return true;
            }

            publishProgress("\nProblem accessing CK server: " + err + "\n");
            return true;
        }
        return false;
    }
}
