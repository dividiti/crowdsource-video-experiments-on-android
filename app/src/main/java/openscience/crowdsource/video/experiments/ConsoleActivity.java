package openscience.crowdsource.video.experiments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.net.PasswordAuthentication;
import java.util.Properties;

import static android.R.attr.password;
import static openscience.crowdsource.video.experiments.AppConfigService.SUPPORT_EMAIL;
import static openscience.crowdsource.video.experiments.BuildConfig.APPLICATION_ID;

/**
 * Screen with app console log displays recognition and other processes details
 *
 * @author Daniil Efremov
 */
public class ConsoleActivity extends AppCompatActivity {

    private EditText consoleEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_console);

        MainActivity.setTaskBarColored(this);

        addToolbarListeners();

        consoleEditText = (EditText) findViewById(R.id.consoleEditText);
        AppLogger.updateTextView(consoleEditText);
        MainActivity.setTaskBarColored(this);
        registerLoggerViewerUpdater();

        String error = getIntent().getStringExtra(ExceptionHandler.ERROR);
        if (error != null) {
            AppLogger.logMessage(error);
            Button homeRecognize = (Button) findViewById(R.id.btn_home_recognizeConsole);
            homeRecognize.setEnabled(false);
            homeRecognize.setVisibility(View.GONE);
        }

        View copyToBufferButton = (View) findViewById(R.id.ico_copy_to_buffer);
        copyToBufferButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText(APPLICATION_ID + " logs", AppLogger.getAllLogs());
                clipboard.setPrimaryClip(clip);

                AlertDialog.Builder builder = new AlertDialog.Builder(ConsoleActivity.this);
                builder.setMessage("Logs successfully copied to buffer.")
                        .setCancelable(false)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //do nothing
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();
            }
        });

        View sendLogByEmailButton = (View) findViewById(R.id.ico_send_by_email);
        sendLogByEmailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                emailIntent.setData(Uri.parse("mailto:" + SUPPORT_EMAIL));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, APPLICATION_ID + " logs");
                emailIntent.putExtra(Intent.EXTRA_TEXT, AppLogger.getAllLogs());

                try {
                    startActivity(Intent.createChooser(emailIntent, "Send email using..."));
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(ConsoleActivity.this, "No email clients installed.", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private void registerLoggerViewerUpdater() {
        AppLogger.registerTextView(new AppLogger.Updater() {
            @Override
            public void update(final String message) {
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
        registerLoggerViewerUpdater();
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
        Button infoButton = (Button) findViewById(R.id.btn_infoConsole);
        infoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent aboutIntent = new Intent(ConsoleActivity.this, InfoActivity.class);
                startActivity(aboutIntent);
            }
        });

        Button homeRecognize = (Button) findViewById(R.id.btn_home_recognizeConsole);
        homeRecognize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent aboutIntent = new Intent(ConsoleActivity.this, MainActivity.class);
                startActivity(aboutIntent);
            }
        });
    }

    @Nullable
    @Override
    public Intent getSupportParentActivityIntent() {
        return super.getSupportParentActivityIntent();
    }
}
