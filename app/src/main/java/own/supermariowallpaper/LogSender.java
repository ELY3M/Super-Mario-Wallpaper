package own.supermariowallpaper;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;

import org.acra.ACRAConstants;
import org.acra.ReportField;
import org.acra.annotation.ReportsCrashes;
import org.acra.collector.CrashReportData;
import org.acra.config.ACRAConfiguration;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class LogSender implements ReportSender {

    private final ACRAConfiguration config;

    public LogSender(@NonNull ACRAConfiguration config) {
        this.config = config;
    }

    @Override
    public void send(@NonNull Context context, @NonNull CrashReportData errorContent) throws ReportSenderException {

        final String body = buildBody(errorContent);

        Date date = new Date();
        Date time = new Date();
        String logdate = new SimpleDateFormat("EEE MMM, d yyyy").format(date);
        String logtime = new SimpleDateFormat("h:mm:ss").format(time);
        String appname = context.getPackageName();
        File setfilename = new File(Environment.getExternalStorageDirectory(), appname+"-crashlogs.txt");


        try {
            FileWriter writer = new FileWriter(setfilename, true);
            BufferedWriter bufferedWriter = new BufferedWriter(writer);
            bufferedWriter.write("------ Crash on "+logdate+" at "+logtime+" ---------------------------------------------------------------------");
            bufferedWriter.newLine();
            bufferedWriter.write(body);
            bufferedWriter.newLine();
            bufferedWriter.close();
        } catch (IOException e) {
            ///Log.i(TAG, "writer crash..." + e);
            e.printStackTrace();
        }
				
    }

    private String buildBody(@NonNull CrashReportData errorContent) {
        ReportField[] fields = config.customReportContent();
        if(fields.length == 0) {
            fields = ACRAConstants.DEFAULT_REPORT_FIELDS;
        }

        final StringBuilder builder = new StringBuilder();
        for (ReportField field : fields) {
            builder.append(field.toString()).append("=");
            builder.append(errorContent.get(field));
            builder.append('\n');
        }
        return builder.toString();
    }
}
