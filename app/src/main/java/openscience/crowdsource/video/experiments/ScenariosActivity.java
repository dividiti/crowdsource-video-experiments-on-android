package openscience.crowdsource.video.experiments;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;

import static openscience.crowdsource.video.experiments.MainActivity.setTaskBarColored;

public class ScenariosActivity extends AppCompatActivity {
    private LayoutInflater inflator;
    private Spinner scenarioSpinner;
    private ArrayAdapter<RecognitionScenario> spinnerAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scenarios);
        setTaskBarColored(this);
        inflator = LayoutInflater.from(this);

        View selectScenario = (View)findViewById(R.id.selectScenario);
        selectScenario.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent homeIntent = new Intent(ScenariosActivity.this, MainActivity.class);
                startActivity(homeIntent);
            }
        });


        Button homeButton = (Button) findViewById(R.id.btn_home_recognizeMain);
        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent homeIntent = new Intent(ScenariosActivity.this, MainActivity.class);
                startActivity(homeIntent);
            }
        });


        Button consoleButton = (Button) findViewById(R.id.btn_consoleMain);
        consoleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent logIntent = new Intent(ScenariosActivity.this, ConsoleActivity.class);
                startActivity(logIntent);
            }
        });


        Button infoButton = (Button) findViewById(R.id.btn_infoMain);
        infoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent infoIntent = new Intent(ScenariosActivity.this, InfoActivity.class);
                startActivity(infoIntent);
            }
        });


//        scenarioSpinner = (Spinner) findViewById(R.id.s_scenario);
//        spinnerAdapter = new SpinAdapter(this, R.layout.custom_spinner);
//        scenarioSpinner.setAdapter(spinnerAdapter);
//        scenarioSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                AppConfigService.updateSelectedRecognitionScenario(position);
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> parent) {
//            }
//        });


        final ViewGroup resultRadioGroup = (ViewGroup) findViewById(R.id.rgScenariosList);

        String recognitionResultText = AppConfigService.getRecognitionResultText();
        if (recognitionResultText != null) {
            ArrayList<RecognitionScenario> sortedRecognitionScenarios = RecognitionScenarioService.getSortedRecognitionScenarios();

            if (sortedRecognitionScenarios.size() == 0) {
                AppLogger.logMessage("There is not scenarios found for your device...");
                LinearLayout ll = new LinearLayout(this);
                ll.setOrientation(LinearLayout.HORIZONTAL);
                final TextView resultItemView = new TextView(this);
                resultItemView.setPadding(0, 20, 0 , 20);
                Spanned spanned;
                spanned = Html.fromHtml("<font color='#ffffff'><b>There is not scenarios found for your device...</b></font>");
                resultItemView.setText(spanned);
                ll.addView(resultItemView);
                resultRadioGroup.addView(ll);
            } else {
                final View scenarioItemConvertView = inflator.inflate(R.layout.scenario_item, resultRadioGroup);

                for (int i=0; i < sortedRecognitionScenarios.size(); i++) {
                    final RecognitionScenario recognitionScenario = sortedRecognitionScenarios.get(i);
//                    View ll = scenarioItemConvertView.findViewById(R.id.scenario_row);
//                    TextView scenarioTextView = (TextView) scenarioItemConvertView.findViewById(R.id.scenario);
//                    scenarioTextView.setText(recognitionScenario.getTitle());
//
//                    final TextView volumeTextView = (TextView) scenarioItemConvertView.findViewById(R.id.volume_mb);
//                    volumeTextView.setText(recognitionScenario.getTotalFileSize());
//                    recognitionScenario.setProgressUpdater(new RecognitionScenarioService.Updater() {
//                        @Override
//                        public void update(final RecognitionScenario recognitionScenario) {
//
//                            ScenariosActivity.this.runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    if (recognitionScenario.getState().equals(RecognitionScenario.State.DOWNLOADING_IN_PROGRESS)) {
//                                        volumeTextView.setText(
//                                                Utils.bytesIntoHumanReadable(recognitionScenario.getDownloadedTotalFileSizeBytes()) +
//                                                        " of " +
//                                                        Utils.bytesIntoHumanReadable(recognitionScenario.getTotalFileSizeBytes()));
//                                        scenarioItemConvertView.refreshDrawableState();
//                                    } else {
//                                        volumeTextView.setText(
//                                                Utils.bytesIntoHumanReadable(recognitionScenario.getTotalFileSizeBytes()));
//                                    }
//                                }
//                            });
//
//                            // todo change view volumeTextView.
//                        }
//                    });
//                    recognitionScenario.getProgressUpdater().update(recognitionScenario);
//
//                    final ImageView downloadButton = (ImageView) scenarioItemConvertView.findViewById(R.id.ico_download);
//                    downloadButton.setVisibility(View.VISIBLE);
//                    downloadButton.setEnabled(true);
//                    downloadButton.setOnClickListener(new View.OnClickListener() {
//                        @Override
//                        public void onClick(View v) {
//                            RecognitionScenarioService.startDownloading(recognitionScenario);
//                        }
//                    });
//
//                    recognitionScenario.setButtonUpdater(new RecognitionScenarioService.Updater() {
//                        @Override
//                        public void update(final RecognitionScenario recognitionScenario) {
//
//                            ScenariosActivity.this.runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//
//                                    if (recognitionScenario.getState().equals(RecognitionScenario.State.DOWNLOADING_IN_PROGRESS)) {
//                                        downloadButton.setImageResource(R.drawable.ico_preloading);
//                                        downloadButton.setEnabled(false);
//                                        downloadButton.setVisibility(View.VISIBLE);
//                                        scenarioItemConvertView.refreshDrawableState();
//                                    } if (recognitionScenario.getState().equals(RecognitionScenario.State.DOWNLOADED)) {
//                                        downloadButton.setEnabled(false);
//                                        downloadButton.setVisibility(View.GONE);
//                                    }
//                                }
//                            });
//
//                        }
//                    });
//                    recognitionScenario.getButtonUpdater().update(recognitionScenario);
                    final int selected = i;

                    LinearLayout ll = new LinearLayout(this);
                    ll.setOrientation(LinearLayout.HORIZONTAL);
                    ll.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            AppConfigService.updateSelectedRecognitionScenario(selected);
                            Intent infoIntent = new Intent(ScenariosActivity.this, MainActivity.class);
                            startActivity(infoIntent);
                        }
                    });

                    final TextView resultItemView = new TextView(this);
                    resultItemView.setPadding(0, 20, 0, 20);
                    Spanned spanned = Html.fromHtml("<font color='#ffffff'><b>" + recognitionScenario.getTitle() + "</b></font>" );
                    resultItemView.setText(spanned);
                    ll.addView(resultItemView);

                    final TextView volumeTextView = new TextView(this);
                    volumeTextView.setPadding(0, 20, 0, 20);
                    Spanned volumeTextViewSpanned = Html.fromHtml("<font color='#64ffda'><b>" + recognitionScenario.getTotalFileSize() + "</b></font>");
                    volumeTextView.setText(volumeTextViewSpanned);
                    ll.addView(volumeTextView);


                    recognitionScenario.setProgressUpdater(new RecognitionScenarioService.Updater() {
                        @Override
                        public void update(final RecognitionScenario recognitionScenario) {

                            ScenariosActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (recognitionScenario.getState().equals(RecognitionScenario.State.DOWNLOADING_IN_PROGRESS)) {
                                        volumeTextView.setText(
                                                Utils.bytesIntoHumanReadable(recognitionScenario.getDownloadedTotalFileSizeBytes()) +
                                                        " of " +
                                                        Utils.bytesIntoHumanReadable(recognitionScenario.getTotalFileSizeBytes()));
                                        scenarioItemConvertView.refreshDrawableState();
                                    } else {
                                        volumeTextView.setText(
                                                Utils.bytesIntoHumanReadable(recognitionScenario.getTotalFileSizeBytes()));
                                    }
                                }
                            });

                            // todo change view volumeTextView.
                        }
                    });
                    recognitionScenario.getProgressUpdater().update(recognitionScenario);

                    final ImageView downloadButton = new ImageView(this);
                    downloadButton.setVisibility(View.VISIBLE);
                    downloadButton.setEnabled(true);
                    downloadButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            RecognitionScenarioService.startDownloading(recognitionScenario);
                        }
                    });
                    ll.addView(downloadButton);
                    recognitionScenario.setButtonUpdater(new RecognitionScenarioService.Updater() {
                        @Override
                        public void update(final RecognitionScenario recognitionScenario) {

                            ScenariosActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                    if (recognitionScenario.getState().equals(RecognitionScenario.State.DOWNLOADING_IN_PROGRESS)) {
                                        downloadButton.setImageResource(R.drawable.ico_preloading);
                                        downloadButton.setEnabled(false);
                                        downloadButton.setVisibility(View.VISIBLE);
                                        scenarioItemConvertView.refreshDrawableState();
                                    } if (recognitionScenario.getState().equals(RecognitionScenario.State.DOWNLOADED)) {
                                        downloadButton.setEnabled(false);
                                        downloadButton.setVisibility(View.GONE);
                                    }
                                }
                            });

                        }
                    });
                    recognitionScenario.getButtonUpdater().update(recognitionScenario);


                    resultRadioGroup.addView(ll);
                }
            }
        }
    }
}
