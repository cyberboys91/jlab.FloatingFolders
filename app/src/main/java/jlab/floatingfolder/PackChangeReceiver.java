package jlab.floatingfolder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import androidx.core.app.NotificationManagerCompat;

import java.util.ArrayList;

import jlab.floatingfolder.db.ApplicationDbManager;
import jlab.floatingfolder.db.ApplicationDetails;

/**
 * Created by Javier on 5/1/2021.
 */

public class PackChangeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        ApplicationDbManager appMgr = new ApplicationDbManager(context);
        PackageManager packMgr = context.getPackageManager();
        String action = (intent == null ? null : intent.getAction());
        if (action == null || intent.getData() == null)
            return;
        String packName = intent.getData().getEncodedSchemeSpecificPart();
        int uid = intent.getIntExtra(Intent.EXTRA_UID, 0);
        if (uid > 0) {
            switch (action) {
                case Intent.ACTION_PACKAGE_FULLY_REMOVED:
                    appMgr.deleteApplicationData(packName);
                    Utils.resetAllApps();
                    NotificationManagerCompat.from(context).cancel(uid); // installed notification
                    NotificationManagerCompat.from(context).cancel(uid + 10000); // access notification
                    break;
                case Intent.ACTION_PACKAGE_ADDED:
                    Utils.resetAllApps();
                    break;
                case Intent.ACTION_PACKAGE_REPLACED:
                    Utils.resetAllApps();
                    ArrayList<ApplicationDetails> appsDetails = appMgr.getApplicationForPackName(packName);
                    if (!appsDetails.isEmpty()) {
                        try {
                            ApplicationInfo appInfo = packMgr.getApplicationInfo(packName, PackageManager.GET_META_DATA);
                            String name = packMgr.getApplicationLabel(appInfo).toString();
                            for (ApplicationDetails app : appsDetails) {
                                app.setName(name);
                                appMgr.updateApplicationData(app.getId(), app);
                            }
                        } catch (PackageManager.NameNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
