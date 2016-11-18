package openscience.crowdsource.video.experiments;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * @author Daniil Efremov
 */

public class SpinAdapter extends ArrayAdapter<RecognitionScenario> {

    private LayoutInflater inflator;
    private Activity activity;
//    private ArrayList<RecognitionScenario> values = new ArrayList<>();

    public SpinAdapter(Activity activity, int textViewResourceId) {
        super(activity, textViewResourceId);
        this.activity = activity;
        inflator = LayoutInflater.from(activity);
    }

    @Override
    public void add(RecognitionScenario object) {
        super.add(object);
//        values.add(object);
    }

    @Override
    public void clear() {
        super.clear();
//        values = new ArrayList<>();
    }

    public int getCount(){
        return RecognitionScenarioService.getSortedRecognitionScenarios().size();
    }

    public RecognitionScenario getItem(int position){
        return RecognitionScenarioService.getSortedRecognitionScenarios().get(position);
    }

    public long getItemId(int position){
        return position;
    }

    /**
     * top view
     *
     * @param position
     * @param convertView
     * @param parent
     * @return
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = inflator.inflate(R.layout.custom_spinner, null);
        TextView scenarioTextView = (TextView) convertView.findViewById(R.id.scenario);
        ArrayList<RecognitionScenario> recognitionScenarios = RecognitionScenarioService.getSortedRecognitionScenarios();
        scenarioTextView.setText(recognitionScenarios.get(position).getTitle());

        final TextView volumeTextView = (TextView) convertView.findViewById(R.id.volume_mb);
        volumeTextView.setText(recognitionScenarios.get(position).getTotalFileSize());

        ImageView button = (ImageView) convertView.findViewById(R.id.ico_download);
        button.setVisibility(View.GONE);

        ImageView arrowDropdown = (ImageView) convertView.findViewById(R.id.ico_arrowDropdown);
        arrowDropdown.setVisibility(View.VISIBLE);

        return convertView;
    }

    /**
     * Item view
     *
     * @param position
     * @param convertView
     * @param parent
     * @return
     */
    @Override
    public View getDropDownView(final int position, View convertView,
                                ViewGroup parent) {
        convertView = inflator.inflate(R.layout.custom_spinner, null);
        TextView scenarioTextView = (TextView) convertView.findViewById(R.id.scenario);
        final ArrayList<RecognitionScenario> sortedRecognitionScenarios = RecognitionScenarioService.getSortedRecognitionScenarios();
        scenarioTextView.setText(sortedRecognitionScenarios.get(position).getTitle());

        final TextView volumeTextView = (TextView) convertView.findViewById(R.id.volume_mb);
        volumeTextView.setText(sortedRecognitionScenarios.get(position).getTotalFileSize());
        sortedRecognitionScenarios.get(position).setProgressUpdater(new RecognitionScenarioService.Updater() {
            @Override
            public void update(final RecognitionScenario recognitionScenario) {

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (recognitionScenario.getState().equals(RecognitionScenario.State.DOWNLOADING_IN_PROGRESS)) {
                            volumeTextView.setText(
                                    Utils.bytesIntoHumanReadable(recognitionScenario.getDownloadedTotalFileSizeBytes()) +
                                            " of " +
                                            Utils.bytesIntoHumanReadable(recognitionScenario.getTotalFileSizeBytes()));
                        } else {
                            volumeTextView.setText(
                                    Utils.bytesIntoHumanReadable(recognitionScenario.getTotalFileSizeBytes()));
                        }
//                        notifyDataSetChanged();
                    }
                });

                // todo change view volumeTextView.
            }
        });
        sortedRecognitionScenarios.get(position).getProgressUpdater().update(sortedRecognitionScenarios.get(position));

        final ImageView downloadButton = (ImageView) convertView.findViewById(R.id.ico_download);
        downloadButton.setVisibility(View.VISIBLE);
        downloadButton.setEnabled(true);
        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RecognitionScenarioService.startDownloading(sortedRecognitionScenarios.get(position));
            }
        });

        sortedRecognitionScenarios.get(position).setButtonUpdater(new RecognitionScenarioService.Updater() {
            @Override
            public void update(final RecognitionScenario recognitionScenario) {

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if (recognitionScenario.getState().equals(RecognitionScenario.State.DOWNLOADING_IN_PROGRESS)) {
                            downloadButton.setImageResource(R.drawable.ico_preloading);
                            downloadButton.setEnabled(false);
                            downloadButton.setVisibility(View.VISIBLE);
                        } if (recognitionScenario.getState().equals(RecognitionScenario.State.DOWNLOADED)) {
                            downloadButton.setEnabled(false);
                            downloadButton.setVisibility(View.GONE);
                        }
//                        notifyDataSetChanged();
                    }
                });

            }
        });
        sortedRecognitionScenarios.get(position).getButtonUpdater().update(sortedRecognitionScenarios.get(position));

        ImageView button = (ImageView) convertView.findViewById(R.id.ico_arrowDropdown);
        button.setVisibility(View.GONE);
        return convertView;
    }
}
