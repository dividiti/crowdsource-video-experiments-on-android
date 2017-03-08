package openscience.crowdsource.video.experiments;

import android.os.AsyncTask;

import org.ctuning.openme.openme;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author Daniil Efremov
 */
class GithubCreateIssueCallTask extends AsyncTask<JSONObject, String, JSONObject> {

    private Exception exception;


    protected JSONObject doInBackground(JSONObject... requests) {
        try {
            JSONObject r = new JSONObject();
            URL u;
            HttpURLConnection c=null;

            String post = "{\n" +
                    "  \"title\": \"Found a bug\",\n" +
                    "  \"body\": \"I'm having a problem with this.\",\n" +
                    "}";
            try
            {
                u=new URL("https://api.github.com/dividiti/crowdsource-video-experiments-on-android/issues");
                c=(HttpURLConnection) u.openConnection();

                c.setRequestMethod("POST");
                c.setRequestProperty("Content-Type", "");
                c.setRequestProperty("Content-Length", Integer.toString(post.getBytes().length));
                c.setUseCaches(false);
                c.setDoInput(true);
                c.setDoOutput(true);

                //Send request
                DataOutputStream dos=new DataOutputStream(c.getOutputStream());
                dos.writeBytes(post);
                dos.flush();
                dos.close();
            }
            catch (IOException e)
            {
                if (c!=null) c.disconnect();
                r.put("return", new Integer(1));
                r.put("error", "Failed sending request to remote server ("+e.getMessage()+") ...");
                return r;
            }


            return r;
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
