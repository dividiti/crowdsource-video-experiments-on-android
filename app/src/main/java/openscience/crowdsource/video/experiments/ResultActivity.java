package openscience.crowdsource.video.experiments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.Spanned;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.ctuning.openme.openme;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;

import static openscience.crowdsource.video.experiments.AppConfigService.URL_CROWD_RESULTS;
import static openscience.crowdsource.video.experiments.Utils.decodeSampledBitmapFromResource;
import static openscience.crowdsource.video.experiments.Utils.validateReturnCode;

/**
 * Screen displays recognition result
 * At this screen user also able to send correct recognition result
 *
 * @author Daniil Efremov
 */
public class ResultActivity extends AppCompatActivity {

    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        MainActivity.setTaskBarColored(this);

        imageView = (ImageView) findViewById(R.id.imageView1);

        final ImageView scenarioInfoButton = (ImageView) findViewById(R.id.ico_scenarioInfo);
        scenarioInfoButton.setVisibility(View.VISIBLE);
        scenarioInfoButton.setEnabled(true);
        scenarioInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mainIntent = new Intent(ResultActivity.this, MainActivity.class);
                startActivity(mainIntent);
            }
        });

        Button backButton = (Button) findViewById(R.id.b_back);
        backButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                final Intent mainIntent = new Intent(ResultActivity.this, MainActivity.class);
                startActivity(mainIntent);
            }
        });

        String actualImagePath = AppConfigService.getActualImagePath();
        if (actualImagePath != null) {
            updateImageView(actualImagePath);
        }

        final TextView crowdResultURL = (TextView) findViewById(R.id.crowdResultURL);
        crowdResultURL.setText(Html.fromHtml("<a href=\"" +  AppConfigService.getResultURL() + "\">View your results in the public repository</a>"));
        crowdResultURL.setOnClickListener(new View.OnClickListener() {
            @SuppressWarnings({"unused", "unchecked"})
            @Override
            public void onClick(View arg0) {
                String resultURL = AppConfigService.getResultURL();
                AppLogger.logMessage("Opening " + resultURL + " ...");

                Intent browserIntent =
                        new Intent(Intent.ACTION_VIEW, Uri.parse(resultURL));

                startActivity(browserIntent);
            }
        });

        final ViewGroup resultRadioGroup = (ViewGroup) findViewById(R.id.rgSelectResultList);

        String recognitionResultText = AppConfigService.getRecognitionResultText();
        if (recognitionResultText != null) {
            final String[] predictions = recognitionResultText.split("[\\r\\n]+");

            if (predictions.length < 1) {
                AppLogger.logMessage("Error incorrect result text format...");
                return;
            }

            for (int p = 0; p <= predictions.length; p++) {
                LinearLayout ll = new LinearLayout(this);
                ll.setOrientation(LinearLayout.HORIZONTAL);
                final TextView resultItemView = new TextView(this);
                resultItemView.setPadding(0, 20, 0 , 20);
                Spanned spanned;
                final EditText edittext = new EditText(ResultActivity.this);
                edittext.setEnabled(false);
                int skipIndex = 0;
                if (p == skipIndex) {
                    spanned = Html.fromHtml("<font color='red'><b>" + predictions[p] + "</b></font>");
                    TextView correctResultvalue = (TextView) findViewById(R.id.predictedResultValue);
                    correctResultvalue.setText(spanned);
                    continue;
                } else if (p == predictions.length){
                    spanned = Html.fromHtml("<font color='#ffffff'><b>Other: _____________________________</b></font>");
                    edittext.setText("");
                    edittext.setEnabled(true);
                } else {
                    spanned = Html.fromHtml("<font color='#ffffff'><b><u>" + predictions[p] + "</u></b></font>");
                    edittext.setText(predictions[p]);
                }
                resultItemView.setText(spanned);
                if (p > skipIndex) {
                    // first result should not be clickable for suggestion
                    resultItemView.setOnClickListener(new View.OnClickListener() {

                        AlertDialog clarifyDialog;

                        @Override
                        public void onClick(View v) {
                            if (clarifyDialog != null) {
                                clarifyDialog.show();
                                return;
                            }
                            AlertDialog.Builder clarifyDialogBuilder = new AlertDialog.Builder(ResultActivity.this);
                            clarifyDialogBuilder
                                    .setTitle("Send correct answer:")
                                    .setView(edittext)
                                    .setCancelable(false)

                                    .setPositiveButton("Send",
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {
                                                    dialog.cancel();
                                                    String correctAnswer = edittext.getText().toString();
                                                    String recognitionResultText = AppConfigService.getRecognitionResultText();
                                                    String dataUID = AppConfigService.getDataUID();
                                                    String behaviorUID = AppConfigService.getBehaviorUID();
                                                    String crowdUID = AppConfigService.getCrowdUID();

                                                    sendCorrectAnswer(recognitionResultText, correctAnswer, AppConfigService.getActualImagePath(), dataUID, behaviorUID, crowdUID);

                                                    AlertDialog.Builder builder = new AlertDialog.Builder(ResultActivity.this);
                                                    builder.setMessage("Your correct answer was successfully sent.")
                                                            .setCancelable(false)
                                                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                                                public void onClick(DialogInterface dialog, int id) {
                                                                    final Intent mainIntent = new Intent(ResultActivity.this, MainActivity.class);
                                                                    startActivity(mainIntent);
                                                                }
                                                            });
                                                    AlertDialog alert = builder.create();
                                                    alert.show();
                                                }
                                            })
                                    .setNegativeButton("Cancel",
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {
                                                    dialog.cancel();
                                                }
                                            });
                            clarifyDialog = clarifyDialogBuilder.create();
                            clarifyDialog.show();
                        }
                    });
                }
                ll.addView(resultItemView);
                resultRadioGroup.addView(ll);
            }
        }

        View selectScenario = (View)findViewById(R.id.topSelectedScenario);
        selectScenario.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent homeIntent = new Intent(ResultActivity.this, ScenariosActivity.class);
                startActivity(homeIntent);
            }
        });
        TextView selectScenarioText = (TextView)findViewById(R.id.topSelectedScenarioText);
        selectScenarioText.setText(RecognitionScenarioService.getSelectedRecognitionScenario().getTitle());

        AppConfigService.updateState(AppConfigService.AppConfig.State.READY);
    }

    @Override
    protected void onStop() {
        super.onStop();
        AppConfigService.updateState(AppConfigService.AppConfig.State.READY);
    }

    public void sendCorrectAnswer(String recognitionResultText,
                                  String correctAnswer,
                                  String imageFilePath,
                                  String data_uid,
                                  String behavior_uid,
                                  String crowd_uid) {

        AppLogger.logMessage("Adding correct answer to Collective Knowledge ...");

        JSONObject request = new JSONObject();
        try {
            request.put("raw_results", recognitionResultText);
            request.put("correct_answer", correctAnswer);
            String base64content = "";
            if (imageFilePath != null) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                Bitmap bitmap = BitmapFactory.decodeFile(imageFilePath);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream); // todo we can reduce image size sending image in low quality
                byte[] byteFormat = stream.toByteArray();
                base64content = Base64.encodeToString(byteFormat, Base64.URL_SAFE); //todo fix hanging up on Galaxy Note4
                request.put("file_base64", base64content);
            }
            request.put("data_uid", data_uid);
            request.put("behavior_uid", behavior_uid);
            request.put("remote_server_url", AppConfigService.getRemoteServerURL());
            request.put("action", "process_unexpected_behavior");
            request.put("module_uoa", "experiment.bench.dnn.mobile");
            request.put("crowd_uid", crowd_uid);

            new ResultActivity.RemoteCallTask().execute(request);
        } catch (JSONException e) {
            AppLogger.logMessage("\nError send correct answer to server (" + e.getMessage() + ") ...\n\n");
            return;
        }
    }

    private void updateImageView(String imagePath) {
        if (imagePath != null) {
            try {
                Bitmap bmp = decodeSampledBitmapFromResource(imagePath, imageView.getMaxWidth(), imageView.getMaxHeight());
                imageView.setVisibility(View.VISIBLE);
                imageView.setEnabled(true);

                imageView.setImageBitmap(bmp);
                bmp = null;
            } catch (Exception e) {
                AppLogger.logMessage("Error on drawing image " + e.getLocalizedMessage());
            }
        }
    }

    class RemoteCallTask extends AsyncTask<JSONObject, String, JSONObject> {

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
                publishProgress("\nError processing remote call task(" + e.getMessage() + ") ...\n\n");
                return null;
            }
        }

        protected void onProgressUpdate(String... values) {
            if (values[0] != "") {
                AppLogger.logMessage(values[0]);
            } else if (values[1] != "") {
                AppLogger.logMessage("Error on progress update: to many parameters  " + values[1] + " " + values[2]);
            }
        }

        protected void onPostExecute(JSONObject response) {
            // TODO: check this.exception
            // TODO: do something with the response
        }
    }

}
