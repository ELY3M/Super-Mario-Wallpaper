	package own.supermariowallpaper;

    import android.app.Application;
    import android.content.Context;

    import org.acra.ACRA;
    import org.acra.ReportField;
    import org.acra.annotation.ReportsCrashes;
    import org.acra.*;
    import org.acra.config.ACRAConfiguration;
    import org.acra.config.ConfigurationBuilder;
    import org.acra.sender.ReportSenderFactory;

    @ReportsCrashes(
            ///customReportContent = { ReportField.STACK_TRACE }
            reportSenderFactoryClasses = {own.supermariowallpaper.LogSenderFactory.class}
    )

    public class CrashReport extends Application {
        @Override
        protected void attachBaseContext(Context base) {
            super.attachBaseContext(base);


            // Initialise ACRA
            ACRA.init(this);
        }
    }