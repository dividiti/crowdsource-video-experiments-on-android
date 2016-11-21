package openscience.crowdsource.video.experiments;

import android.app.Application;

/**
 * @author Daniil Efremov
 */

public class CrowdSourceVideoExperimentsApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppLogger.cleanup();
    }
}