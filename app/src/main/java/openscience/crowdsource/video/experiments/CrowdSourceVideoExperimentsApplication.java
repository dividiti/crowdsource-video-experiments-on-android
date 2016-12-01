package openscience.crowdsource.video.experiments;

import android.app.Application;

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
    }
}