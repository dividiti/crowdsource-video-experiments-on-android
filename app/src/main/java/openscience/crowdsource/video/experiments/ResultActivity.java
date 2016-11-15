package openscience.crowdsource.video.experiments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.Base64;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import org.ctuning.openme.openme;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;

import static openscience.crowdsource.video.experiments.MainActivity.calculateInSampleSize;

public class ResultActivity extends AppCompatActivity {

    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        MainActivity.setTaskBarColored(this);
//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        imageView = (ImageView) findViewById(R.id.imageView1);

        Button suggestButton = (Button) findViewById(R.id.suggest);
        suggestButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                final EditText edittext = new EditText(ResultActivity.this);
                AlertDialog.Builder clarifyDialogBuilder = new AlertDialog.Builder(ResultActivity.this);
                clarifyDialogBuilder.setTitle("Please, enter correct answer:")
                        //todo provide some standart icon for synch answer for example using clarifyDialogBuilder.setIcon(R.drawable.)
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
                                    }
                                })
                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });
                final AlertDialog clarifyDialog = clarifyDialogBuilder.create();


                clarifyDialog.setMessage("");
                clarifyDialog.setTitle("Please, enter correct answer:");
                clarifyDialog.setView(edittext);
                clarifyDialog.show();

            }
        });



        String actualImagePath = AppConfigService.getActualImagePath();
        if (actualImagePath != null) {
            updateImageView(actualImagePath);
        }

        TextView resultTextView = (TextView) findViewById(R.id.resultText);
        String recognitionResultText = AppConfigService.getRecognitionResultText();
        if (recognitionResultText != null) {
            String[] predictions = recognitionResultText.split("[\\r\\n]+");

            if (predictions.length < 2) {
                AppLogger.logMessage("Error incorrect result text format...");
                return;
            }

            final String firstPrediction = predictions[1];
            StringBuilder otherPredictionsBuilder = new StringBuilder();
            for (int p = 2; p < predictions.length; p++) {
                otherPredictionsBuilder.append(predictions[p]).append("<br>");
            }
            final String otherPredictions = otherPredictionsBuilder.toString();
            resultTextView.setText(Html.fromHtml("<font color='red'><b>" + firstPrediction + "</b></font><br>" + otherPredictions));
        }


        Spinner scenarioSpinner = (Spinner) findViewById(R.id.s_scenario);
        ArrayAdapter spinnerAdapter = new SpinAdapter(this, R.layout.custom_spinner);
        scenarioSpinner.setAdapter(spinnerAdapter);
        scenarioSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                AppConfigService.updateSelectedRecognitionScenario(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        scenarioSpinner.setEnabled(false);
        preloadScenarioses(scenarioSpinner, spinnerAdapter);
        AppConfigService.updateState(AppConfigService.AppConfig.State.READY);
    }

    @Override
    protected void onStop() {
        super.onStop();
        AppConfigService.updateState(AppConfigService.AppConfig.State.READY);
    }

    void preloadScenarioses(Spinner scenarioSpinner, ArrayAdapter spinnerAdapter) {
        //todo remove C&P
        String externalSDCardPath = File.separator + "sdcard";
        String externalSDCardOpensciencePath = externalSDCardPath + File.separator + "openscience" + File.separator;
        String externalSDCardOpenscienceTmpPath = externalSDCardOpensciencePath + File.separator + "tmp" + File.separator;
        String cachedScenariosFilePath = externalSDCardOpensciencePath + "scenariosFile.json";

        File scenariosFile = new File(cachedScenariosFilePath);
        if (scenariosFile.exists()) {
            try {
                JSONObject dict = openme.openme_load_json_file(cachedScenariosFilePath);
                // contract of serialisation and deserialization is not the same so i need to unwrap here original JSON
                JSONObject scenariosJSON = dict.getJSONObject("dict");
                updateScenarioDropdown(scenariosJSON, spinnerAdapter);

                spinnerAdapter.sort(SpinAdapter.COMPARATOR);
                scenarioSpinner.setSelection(AppConfigService.getSelectedRecognitionScenario());
                spinnerAdapter.notifyDataSetChanged();
            } catch (JSONException e) {
                AppLogger.logMessage("ERROR could not read preloaded file " + cachedScenariosFilePath);
                return;
            }
            scenarioSpinner.setSelection(AppConfigService.getSelectedRecognitionScenario());
        }
    }

    private void updateScenarioDropdown(JSONObject scenariosJSON, final ArrayAdapter spinnerAdapter) {
        try {

            JSONArray scenarios = scenariosJSON.getJSONArray("scenarios");
            if (scenarios.length() == 0) {
                return;
            }

            for (int i = 0; i < scenarios.length(); i++) {
                JSONObject scenario = scenarios.getJSONObject(i);


                final String module_uoa = scenario.getString("module_uoa");
                final String dataUID = scenario.getString("data_uid");
                final String data_uoa = scenario.getString("data_uoa");

                scenario.getJSONObject("search_dict");
                scenario.getString("ignore_update");
                scenario.getString("search_string");
                JSONObject meta = scenario.getJSONObject("meta");
                String title = meta.getString("title");

                String sizeMB = "";
                Long sizeBytes = Long.valueOf(0);
                try {
                    String sizeB = scenario.getString("total_file_size");
                    sizeBytes = Long.valueOf(sizeB);
                    sizeMB = MainActivity.bytesIntoHumanReadable(sizeBytes);
                } catch (JSONException e) {
                    AppLogger.logMessage("Erro " + e.getLocalizedMessage());
                }

                final RecognitionScenario recognitionScenario = new RecognitionScenario();
                recognitionScenario.setModuleUOA(module_uoa);
                recognitionScenario.setDataUOA(data_uoa);
                recognitionScenario.setRawJSON(scenario);
                recognitionScenario.setTitle(title);
                recognitionScenario.setTotalFileSize(sizeMB);
                recognitionScenario.setTotalFileSizeBytes(sizeBytes);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //stuff that updates ui
                        spinnerAdapter.add(recognitionScenario);
                        spinnerAdapter.notifyDataSetChanged();
                    }
                });
            }
        } catch (JSONException e) {
            AppLogger.logMessage("Error loading scenarios from file " + e.getLocalizedMessage());
        }
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

    // todo renmove C&P
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

    // todo renmove C&P
    public static Bitmap decodeSampledBitmapFromResource(String imagePath,
                                                         int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(imagePath, options);
    }


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
                AppLogger.logMessage("Error on progress update: to many parameters  " + values[1] + " " + values[2]);
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

}
