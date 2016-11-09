package openscience.crowdsource.video.experiments;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.EditText;

import org.ctuning.openme.openme;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

import static java.lang.Thread.sleep;

public class LogActivity extends AppCompatActivity {

    private EditText consoleEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        consoleEditText = (EditText) findViewById(R.id.consoleEditText);
        AppLogger.updateTextView(consoleEditText);
        MainActivity.setTaskBarColored(this);
        new RemoteCallTask().execute("");
    }

    @Nullable
    @Override
    public Intent getSupportParentActivityIntent() {
        return super.getSupportParentActivityIntent();
    }

    class RemoteCallTask extends AsyncTask<String, String, String> {

        protected String doInBackground(String... requests) {
            try {
                for (int i = 0; i<10; i++) {
                    sleep(1000);
                    // todo remove debug
//                    publishProgress(" some other daemon log " + new Date());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            AppLogger.updateTextView(consoleEditText);
                        }
                    });

                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        protected void onProgressUpdate(String... values) {
            if (values[0] != "") {
                AppLogger.logMessage(values[0]);
                AppLogger.updateTextView(consoleEditText);
            } else if (values[1] != "") {
                //todo handle error
            }
        }

        protected void onPostExecute(JSONObject response) {
            // TODO: check this.exception
            // TODO: do something with the response
        }

    }
}
