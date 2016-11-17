package openscience.crowdsource.video.experiments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * @author Daniil Efremov
 */

public class SpinAdapter extends ArrayAdapter<RecognitionScenario> {

    private LayoutInflater inflator;
    private Activity activity;
    private ArrayList<RecognitionScenario> values = new ArrayList<>();
    public static final Comparator<? super RecognitionScenario> COMPARATOR = new Comparator<RecognitionScenario>() {
        @SuppressLint("NewApi")
        @Override
        public int compare(RecognitionScenario lhs, RecognitionScenario rhs) {
            return Long.compare(lhs.getTotalFileSizeBytes().longValue(), rhs.getTotalFileSizeBytes().longValue());
        }
    };

    public SpinAdapter(Activity activity, int textViewResourceId) {
        super(activity, textViewResourceId);
        this.activity = activity;
        inflator = LayoutInflater.from(activity);
    }

    @Override
    public void add(RecognitionScenario object) {
        super.add(object);
        values.add(object);
    }

    @Override
    public void clear() {
        super.clear();
        values = new ArrayList<>();
    }

    public int getCount(){
        return values.size();
    }

    public RecognitionScenario getItem(int position){
        return values.get(position);
    }

    public long getItemId(int position){
        return position;
    }

    @Override
    public void sort(Comparator<? super RecognitionScenario> comparator) {
        if (values != null) {
            Collections.sort(values, COMPARATOR);
        }
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
        scenarioTextView.setText(values.get(position).getTitle());

        final TextView volumeTextView = (TextView) convertView.findViewById(R.id.volume_mb);
        volumeTextView.setText(values.get(position).getTotalFileSize());

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
        scenarioTextView.setText(values.get(position).getTitle());

        final TextView volumeTextView = (TextView) convertView.findViewById(R.id.volume_mb);
        volumeTextView.setText(values.get(position).getTotalFileSize());
        values.get(position).setProgressUpdater(new RecognitionScenarioService.Updater() {
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
                    }
                });

                // todo change view volumeTextView.
            }
        });
        values.get(position).getProgressUpdater().update(values.get(position));

        final ImageView downloadButton = (ImageView) convertView.findViewById(R.id.ico_download);
        downloadButton.setVisibility(View.VISIBLE);
        downloadButton.setEnabled(true);
        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RecognitionScenarioService.startDownloading(values.get(position));
            }
        });

        values.get(position).setButtonUpdater(new RecognitionScenarioService.Updater() {
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
                    }
                });

            }
        });
        values.get(position).getButtonUpdater().update(values.get(position));

        ImageView button = (ImageView) convertView.findViewById(R.id.ico_arrowDropdown);
        button.setVisibility(View.GONE);


        return convertView;
    }
}
