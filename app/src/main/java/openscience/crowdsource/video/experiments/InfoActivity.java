package openscience.crowdsource.video.experiments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class InfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        MainActivity.setTaskBarColored(this);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        addToolbarListeners();
        addListenersOnButtons();
    }

    private void addToolbarListeners() {
        Button consoleButton = (Button) findViewById(R.id.btn_console);
        consoleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent logIntent = new Intent(InfoActivity.this, ConsoleActivity.class);
                startActivity(logIntent);
            }
        });

        Button homeRecognize = (Button) findViewById(R.id.btn_home_recognize);
        homeRecognize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent aboutIntent = new Intent(InfoActivity.this, MainActivity.class);
                startActivity(aboutIntent);
            }
        });
    }

    /*************************************************************************/
    private void addListenersOnButtons() {
        //todo move out to AppConfigService
        final String url_sdk = "http://github.com/ctuning/ck";
        final String url_about = "https://github.com/ctuning/ck/wiki/Advanced_usage_crowdsourcing";
        final String url_stats = "http://cknowledge.org/repo/web.php?action=index&module_uoa=wfe&native_action=show&native_module_uoa=program.optimization&scenario=experiment.bench.dnn.mobile";
        final String url_users = "http://cTuning.org/crowdtuning-timeline";

        Button b_sdk = (Button) findViewById(R.id.b_sdk);
        Button b_about = (Button) findViewById(R.id.b_about_app);
        Button b_stats = (Button) findViewById(R.id.b_results);
        Button b_users = (Button) findViewById(R.id.b_users);

        b_sdk.setOnClickListener(new View.OnClickListener() {
            @SuppressWarnings({"unused", "unchecked"})
            @Override
            public void onClick(View arg0) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url_sdk));
                startActivity(browserIntent);
            }
        });

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

//        Button buttonUpdateExit.setOnClickListener(new View.OnClickListener() {
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

            Button t_email = (Button) findViewById(R.id.b_email);
            t_email.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText edittext = new EditText(InfoActivity.this);
                String email = AppConfigService.getEmail();
                edittext.setText(email);
                AlertDialog.Builder clarifyDialogBuilder = new AlertDialog.Builder(InfoActivity.this);
                clarifyDialogBuilder.setTitle("Please, enter email:")
                        .setCancelable(false)
                        .setPositiveButton("Update",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                        String newEmail = edittext.getText().toString();
                                        AppConfigService.updateEmail(newEmail);
                                    }
                                })
                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });
                final AlertDialog clarifyDialog = clarifyDialogBuilder.create();

                clarifyDialog.setTitle("");
                clarifyDialog.setMessage(Html.fromHtml("(OPTIONAL) Please enter your email if you would like to acknowledge your contributions (will be publicly visible):"));

                SpannableString spanString = new SpannableString(email.trim());
                spanString.setSpan(new UnderlineSpan(), 0, spanString.length(), 0);

                clarifyDialog.setView(edittext);
                clarifyDialog.show();
            }
        });

        Button b_cleanup = (Button) findViewById(R.id.b_cleanup);
        b_cleanup.setOnClickListener(new View.OnClickListener() {
            @SuppressWarnings({"unused", "unchecked"})
            @Override
            public void onClick(View arg0) {
                AlertDialog.Builder clarifyDialogBuilder = new AlertDialog.Builder(InfoActivity.this);
                clarifyDialogBuilder.setTitle("Are you sure to clean up logs and tmp image files:")
                        .setCancelable(false)
                        .setPositiveButton("yes",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                        AppConfigService.deleteTMPFiles();
                                        AppLogger.cleanup();
                                    }
                                })
                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });
                final AlertDialog clarifyDialog = clarifyDialogBuilder.create();
                clarifyDialog.show();
            }
        });
    }
}
