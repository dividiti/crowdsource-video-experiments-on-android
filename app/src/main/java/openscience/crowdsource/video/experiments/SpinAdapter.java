package openscience.crowdsource.video.experiments;

import android.content.Context;
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
    private Context context;
    private ArrayList<RecognitionScenario> values = new ArrayList<>();

    public SpinAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
        this.context = context;
        inflator = LayoutInflater.from(context);
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

//    @Override
//    public void sort(Comparator<? super MainActivity.RecognitionScenario> comparator) {
////        comparator = new Comparator<MainActivity.RecognitionScenario>()
//        comparator = new Comparator<MainActivity.RecognitionScenario>() {
//            @SuppressLint("NewApi")
//            @Override
//            public int compare(MainActivity.RecognitionScenario lhs, MainActivity.RecognitionScenario rhs) {
//                return Long.compare(lhs.getTotalFileSizeBytes().longValue(), rhs.getTotalFileSizeBytes().longValue());
//            }
//        };
//        super.sort(comparator);
//    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = inflator.inflate(R.layout.custom_spinner, null);
        TextView scenarioTextView = (TextView) convertView.findViewById(R.id.scenario);
        scenarioTextView.setText(values.get(position).getTitle());

        TextView volumeTextView = (TextView) convertView.findViewById(R.id.volume_mb);
        volumeTextView.setText(values.get(position).getTotalFileSize());

        ImageView button = (ImageView) convertView.findViewById(R.id.ico_download);
        button.setVisibility(View.GONE);
        return convertView;
    }

    @Override
    public View getDropDownView(int position, View convertView,
                                ViewGroup parent) {
        convertView = inflator.inflate(R.layout.custom_spinner, null);
        TextView scenarioTextView = (TextView) convertView.findViewById(R.id.scenario);
        scenarioTextView.setText(values.get(position).getTitle());

        TextView volumeTextView = (TextView) convertView.findViewById(R.id.volume_mb);
        volumeTextView.setText(values.get(position).getTotalFileSize());
        return convertView;
    }
}
