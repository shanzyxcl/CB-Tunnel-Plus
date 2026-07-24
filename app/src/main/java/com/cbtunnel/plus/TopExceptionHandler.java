package com.cbtunnel.plus;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.cbtunnel.plus.R;

/**
 * Reporta erros
 * @author dFiR30n
 */
public class TopExceptionHandler implements Thread.UncaughtExceptionHandler {
    private static final String FILE_ERROR = "stack.trace";
    private final Executor executor = Executors.newSingleThreadExecutor();
    private static TopExceptionHandler mExceptionHandler;

    private final Thread.UncaughtExceptionHandler defaultUEH;
    private final Context mContext;

    // inicia
    public static void init(Context context) {
        if (mExceptionHandler == null) {
            mExceptionHandler = new TopExceptionHandler(context);
        }
        Thread.setDefaultUncaughtExceptionHandler(mExceptionHandler);
    }

    private TopExceptionHandler(Context context) {
        this.defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
        this.mContext = context;
    }

    public void uncaughtException(Thread t, Throwable e) {
        StackTraceElement[] arr = e.getStackTrace();
        String report = e.toString()+"\n\n";
        report += "--------- Stack trace ---------\n\n";
        for (int i = 0; i < arr.length; i++) {
            report += "    " + arr[i].toString() + "\n";
        }
        report += "-------------------------------\n\n";
        report += "--------- Cause ---------\n\n";
        Throwable cause = e.getCause();
        if (cause != null) {
            report += cause.toString() + "\n\n";
            arr = cause.getStackTrace();
            for (int i = 0; i < arr.length; i++) {
                report += "    " + arr[i].toString() + "\n";
            }
        }
        report += "-------------------------------\n\n";
        inboxNotification(mContext.getResources().getString(R.string.app_name)+" Application error",report);
        fileAppError("Application",report);
        defaultUEH.uncaughtException(t, e);
    }

    private void inboxNotification(String title, String msg) {
        Notification.Builder mBuilder = new Notification.Builder(mContext)
                .setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.icon_icon))
                .setSmallIcon(R.drawable.ic_bug_report)
                .setContentTitle(title)
                .setContentText(msg)
                .setAutoCancel(true);
        Notification.BigTextStyle inboxStyle = new Notification.BigTextStyle();
        inboxStyle.setBigContentTitle(title);
        inboxStyle.bigText(msg);
        mBuilder.setStyle(inboxStyle);
        /*Intent intent = mContext.getIntent();
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(mContext);
        stackBuilder.addNextIntent(intent);
        mBuilder.setContentIntent(ConfigUtil.getPendingIntent(OpenVPNClient.this));*/
        NotificationManager mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = mContext.getResources().getString(R.string.channel_name_userreq);
            NotificationChannel mChannel = new NotificationChannel("openvpn_userreq",name, NotificationManager.IMPORTANCE_HIGH);
            mChannel.setDescription(mContext.getResources().getString(R.string.channel_description_userreq));
            mChannel.enableVibration(true);
            mChannel.setLightColor(Color.CYAN);
            mBuilder.setChannelId("openvpn_userreq");
            if (mNotificationManager != null) {
                mNotificationManager.createNotificationChannel(mChannel);
            }
        } else {
            mBuilder.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE);
        }
        if (mNotificationManager != null) {
            mNotificationManager.notify(1990, mBuilder.build());
        }
    }
    public void fileAppError(String fileName,String content) {
        executor.execute(() -> {
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS+"/"+ mContext.getResources().getString(R.string.app_name)+" Exception");
            dir.mkdirs();
            File file=new File(dir,fileName+".error");
            try (Writer os = new OutputStreamWriter(new FileOutputStream(file))) {
                os.write(content);
                os.flush();
                os.close();
            }
            catch (Throwable ignored) {
            }
        });
    }

}
