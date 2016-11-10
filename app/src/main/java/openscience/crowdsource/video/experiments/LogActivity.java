package openscience.crowdsource.video.experiments;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.json.JSONObject;

import static java.lang.Thread.sleep;

public class LogActivity extends AppCompatActivity {

    private EditText consoleEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        addToolbarListeners();

        consoleEditText = (EditText) findViewById(R.id.consoleEditText);
        AppLogger.updateTextView(consoleEditText);
        MainActivity.setTaskBarColored(this);
//        new RemoteCallTask().execute("");
        registerLogerViewerUpdater();
    }

    private void registerLogerViewerUpdater() {
        AppLogger.registerTextView(new AppLogger.Updater() {
            @Override
            public void update() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AppLogger.updateTextView(consoleEditText);
                    }
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerLogerViewerUpdater();
    }

    @Override
    protected void onStop() {
        super.onStop();
        AppLogger.unregisterTextView();
    }

    @Override
    protected void onPause() {
        super.onPause();
        AppLogger.unregisterTextView();
    }

    private void addToolbarListeners() {
        Button logButton = (Button) findViewById(R.id.btn_about);
        logButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent aboutIntent = new Intent(LogActivity.this, AboutActivity.class);
                startActivity(aboutIntent);
            }
        });

        Button homeRecognize = (Button) findViewById(R.id.btn_home_recognize);
        homeRecognize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent aboutIntent = new Intent(LogActivity.this, MainActivity.class);
                startActivity(aboutIntent);
            }
        });
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
