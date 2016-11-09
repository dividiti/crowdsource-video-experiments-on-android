package openscience.crowdsource.video.experiments;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class AboutActivity extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        addListenersOnButtons();
        MainActivity.setTaskBarColored(this);
    }
    /*************************************************************************/
    private void addListenersOnButtons() {
        final String url_sdk = "http://github.com/ctuning/ck";
        final String url_about = "https://github.com/ctuning/ck/wiki/Advanced_usage_crowdsourcing";
        final String url_stats = "http://cknowledge.org/repo/web.php?action=index&module_uoa=wfe&native_action=show&native_module_uoa=program.optimization&scenario=experiment.bench.dnn.mobile";
        final String url_users = "http://cTuning.org/crowdtuning-timeline";

        Button b_sdk = (Button) findViewById(R.id.b_sdk);
        Button b_about = (Button) findViewById(R.id.b_about_app);
//        b_clean = (Button) findViewById(R.id.b_clean);
        Button b_stats = (Button) findViewById(R.id.b_results);
        Button b_users = (Button) findViewById(R.id.b_users);

        /*************************************************************************/
        b_sdk.setOnClickListener(new View.OnClickListener() {
            @SuppressWarnings({"unused", "unchecked"})
            @Override
            public void onClick(View arg0) {
//                log.append("\nOpening " + url_sdk + " ...\n");

                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url_sdk));
                startActivity(browserIntent);
            }
        });

        /*************************************************************************/
        b_about.setOnClickListener(new View.OnClickListener() {
            @SuppressWarnings({"unused", "unchecked"})
            @Override
            public void onClick(View arg0) {
                AppLogger.logMessage("Opening " + url_about + " ...");

                Intent browserIntent =
                        new Intent(Intent.ACTION_VIEW, Uri.parse(url_about));

                startActivity(browserIntent);
            }
        });

//        /*************************************************************************/
//        b_clean.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View arg0) {
//                log.setText("");
//                log.setText("Cleaning local tmp files ...\n");
//                if (!clean_log_tmp())
//                    log.setText("  ERROR: Can't create directory " + path + " ...\n");
//            }
//        });

//        /*************************************************************************/
        b_stats.setOnClickListener(new View.OnClickListener() {
            @SuppressWarnings({"unused", "unchecked"})
            @Override
            public void onClick(View arg0) {
                AppLogger.logMessage("Opening " + url_stats + " ...");

                Intent browserIntent =
                        new Intent(Intent.ACTION_VIEW, Uri.parse(url_stats));

                startActivity(browserIntent);
            }
        });

        /*************************************************************************/
        b_users.setOnClickListener(new View.OnClickListener() {
            @SuppressWarnings({"unused", "unchecked"})
            @Override
            public void onClick(View arg0) {
                AppLogger.logMessage("Opening " + url_users + " ...");

                Intent browserIntent =
                        new Intent(Intent.ACTION_VIEW, Uri.parse(url_users));

                startActivity(browserIntent);
            }
        });

        /*************************************************************************/
//        buttonUpdateExit.setOnClickListener(new View.OnClickListener() {
//            @SuppressWarnings({"unused", "unchecked"})
//            @Override
//            public void onClick(View arg0) {
//                if (running) {
//                    running = false;
//
//                    buttonUpdateExit.setEnabled(false);
//
//                    log.append("\n");
//                    log.append(s_thanks);
//                    log.append("Interrupting crowd-tuning and quitting program ...");
//
//                    Handler handler = new Handler();
//                    handler.postDelayed(new Runnable() {
//                        public void run() {
//                            finish();
//                            System.exit(0);
//                        }
//                    }, 1500);
//
//                } else {
//                    platformFeatures = null; // force reload features
//                    isUpdateMode = true;
//                    preloadScenarioses(true);
//                }
//            }
//        });
    }

}
