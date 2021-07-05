package jlab.floatingfolder.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;

import jlab.floatingfolder.R;
import jlab.floatingfolder.activity.MainActivity;
import jlab.floatingfolder.db.ApplicationDbManager;
import jlab.floatingfolder.db.FolderDetails;
import jlab.floatingfolder.view.FloatingFolderView;

import static jlab.floatingfolder.Utils.FOLDER_CHANGED_ACTION;
import static jlab.floatingfolder.Utils.FOLDER_REMOVE_ACTION;
import static jlab.floatingfolder.view.AppListFragment.FOLDER_DETAILS_KEY;

/*
 * Created by Javier on 27/06/2021.
 */

public class FloatingFoldersService extends Service {

    private static final int NOTIFICATION_ID = 91015;
    public static final String STARTED_FLOATING_FOLDERS_SERVICE_ACTION
            = "jlab.action.STARTED_FLOATING_FOLDERS_SERVICE_ACTION",
            STOPPED_FLOATING_FOLDERS_SERVICE_ACTION
            = "jlab.action.STOPPED_FLOATING_FOLDERS_SERVICE_ACTION";
    private String CHANNEL_ID;
    private ApplicationDbManager appMgr;
    private static boolean isRunning;
    private NotificationCompat.Builder notBuilder;
    private ArrayList<FloatingFolderView> floatingFolders = new ArrayList<>();
    private WindowManager windowMgr;
    private BroadcastReceiver folderChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals(FOLDER_CHANGED_ACTION)) {
                FolderDetails folderIntent = intent.getParcelableExtra(FOLDER_DETAILS_KEY);
                int index = getIndexFolder(folderIntent.getId(), folderIntent);
                if (index == -1 && folderIntent.isFloating())
                    addFloatingView(folderIntent);
                if (index != -1 && !folderIntent.isFloating()) {
                    try {
                        windowMgr.removeViewImmediate(floatingFolders.get(index));
                        floatingFolders.remove(index);
                    } catch (Exception | OutOfMemoryError ignored) {
                        ignored.printStackTrace();
                    }
                }
            }
            else if(action != null && action.equals(FOLDER_REMOVE_ACTION)) {
                int id = intent.getIntExtra(FOLDER_DETAILS_KEY, -1);
                if(id != -1) {
                    int index = getIndexFolder(id, null);
                    if(index != -1) {
                        try {
                            windowMgr.removeViewImmediate(floatingFolders.get(index));
                            floatingFolders.remove(index);
                        } catch (Exception | OutOfMemoryError ignored) {
                            ignored.printStackTrace();
                        }
                    }
                }
            }
        }
    };

    private int getIndexFolder(int id, FolderDetails folder) {
        int index = -1;
        for (int i = 0; i < floatingFolders.size(); i++) {
            FloatingFolderView fold = floatingFolders.get(i);
            if (fold.getFolder().getId() == id) {
                index = i;
                if (folder != null)
                    fold.folderChange(folder);
                break;
            }
        }
        return index;
    }

    private static Intent getServiceIntent() {
        return new Intent().setComponent(new ComponentName("jlab.floatingfolder",
                "jlab.floatingfolder.service.FloatingFoldersService"));
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return false;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        appMgr = new ApplicationDbManager(this);
        if (isRunning) {
            super.onCreate();
            if (notBuilder == null) {
                CHANNEL_ID = getString(R.string.app_name);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    NotificationChannel chan = new NotificationChannel(CHANNEL_ID, CHANNEL_ID, NotificationManager.IMPORTANCE_NONE);
                    chan.setLightColor(getResources().getColor(R.color.accent));
                    chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
                    NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    manager.createNotificationChannel(chan);
                }
                notBuilder = new NotificationCompat.Builder(getBaseContext(), CHANNEL_ID)
                        .setContentText(getBaseContext().getString(R.string.started_floating_service))
                        .setContentTitle(getString(R.string.app_name))
                        .setAutoCancel(false)
                        .setSmallIcon(R.drawable.img_running_not)
                        .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                                R.drawable.icon))
                        .setContentIntent(getPendingIntentNotificationClicked());
            }
            try {
                LocalBroadcastManager.getInstance(this)
                        .sendBroadcast(new Intent(STARTED_FLOATING_FOLDERS_SERVICE_ACTION));
                startForeground(NOTIFICATION_ID, notBuilder.build());
                showFloatingFolders();
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(FOLDER_CHANGED_ACTION);
                intentFilter.addAction(FOLDER_REMOVE_ACTION);
                LocalBroadcastManager.getInstance(this)
                        .registerReceiver(folderChangeReceiver, intentFilter);
            } catch (Exception ignored) {
                ignored.printStackTrace();
                LocalBroadcastManager.getInstance(this)
                        .sendBroadcast(new Intent(STOPPED_FLOATING_FOLDERS_SERVICE_ACTION));
            }
        } else
            stopService(getServiceIntent());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            isRunning = false;
            LocalBroadcastManager.getInstance(this).unregisterReceiver(folderChangeReceiver);
            LocalBroadcastManager.getInstance(this)
                    .sendBroadcast(new Intent(STOPPED_FLOATING_FOLDERS_SERVICE_ACTION));
            try {
                if (!floatingFolders.isEmpty()) {
                    for (FloatingFolderView floatingFolderView : floatingFolders)
                        windowMgr.removeViewImmediate(floatingFolderView);
                    floatingFolders.clear();
                }
            }  catch (Exception | OutOfMemoryError ignored) {
                //TODO: disable log
                //ignored.printStackTrace();
            }
            //TODO: disable log
            //Log.i(TAG, "Stopped");
        } catch (Exception | OutOfMemoryError ignored) {
            //TODO: disable log
            //ignored.printStackTrace();
        } finally {

        }
    }

    private void showFloatingFolders() {
        try {
            if (!Thread.interrupted() && isRunning()) {
                floatingFolders.clear();
                windowMgr = (WindowManager) getSystemService(WINDOW_SERVICE);
                if(windowMgr != null) {
                    ArrayList<FolderDetails> folders = appMgr.getAllFolders("");
                    for (FolderDetails fold : folders) {
                        if(fold.isFloating())
                            addFloatingView(fold);
                    }
                }
            }
        }  catch (Exception | OutOfMemoryError ignored) {
            //TODO: disable log
            ignored.printStackTrace();
        }
    }

    private void addFloatingView(FolderDetails fold) {
        final FloatingFolderView floatingFolderView = (FloatingFolderView) LayoutInflater.from(this)
                .inflate(R.layout.floating_folder_view, null);
        windowMgr.addView(floatingFolderView, floatingFolderView.setOnTouchListener(windowMgr));
        floatingFolders.add(floatingFolderView);
        floatingFolderView.setFolder(fold);
    }

    public static boolean isRunning() {
        return isRunning;
    }

    private PendingIntent getPendingIntentNotificationClicked() {
        Intent intent = new Intent(getBaseContext(), MainActivity.class);
        return PendingIntent.getActivity(getBaseContext(), 0
                , intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    public static void startService(Context context) {
        if (!isRunning) {
            isRunning = true;
            context.startService(getServiceIntent());
        }
    }

    public static void stopService(Context context) {
        isRunning = false;
        context.stopService(getServiceIntent());
    }
}
