package openscience.crowdsource.video.experiments;

import android.app.Application;

import static openscience.crowdsource.video.experiments.BuildConfig.APPLICATION_ID;

/**
 * Crowd Source Video Experiments Application class with application startup initialization
 *
 * @author Daniil Efremov
 */
public class CrowdSourceVideoExperimentsApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppLogger.cleanup();
        AppConfigService.updateState(AppConfigService.AppConfig.State.READY);
        AppLogger.logMessage("Start application " + APPLICATION_ID + " version " + BuildConfig.VERSION_NAME);
    }
}