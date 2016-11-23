package openscience.crowdsource.video.experiments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;

import static openscience.crowdsource.video.experiments.MainActivity.setTaskBarColored;

public class ScenariosActivity extends AppCompatActivity {

    private Spinner scenarioSpinner;
    private ArrayAdapter<RecognitionScenario> spinnerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scenarios);
        setTaskBarColored(this);

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
                for (int i=0; i < sortedRecognitionScenarios.size(); i++) {
                    RecognitionScenario recognitionScenario = sortedRecognitionScenarios.get(i);
                    LinearLayout ll = new LinearLayout(this);
                    ll.setOrientation(LinearLayout.HORIZONTAL);
                    final TextView resultItemView = new TextView(this);
                    resultItemView.setPadding(0, 20, 0, 20);
                    Spanned spanned = Html.fromHtml("<font color='#ffffff'><b>" + recognitionScenario.getTitle() + "</b></font>" + "<br>" +
                            "<font color='#64ffda'><b>" + recognitionScenario.getTotalFileSize() + "</b></font>");
                    resultItemView.setText(spanned);
                    final int selected = i;
                    resultItemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            AppConfigService.updateSelectedRecognitionScenario(selected);
                            Intent infoIntent = new Intent(ScenariosActivity.this, MainActivity.class);
                            startActivity(infoIntent);
                        }
                    });
                    ll.addView(resultItemView);
                    resultRadioGroup.addView(ll);
                }
            }
        }
    }
}
