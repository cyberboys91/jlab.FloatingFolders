package jlab.floatingfolder;

/*
 * Created by Javier on 27/12/2020.
 */

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import androidx.collection.LruCache;
import androidx.appcompat.app.AlertDialog;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewParent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Transformation;
import android.widget.TextView;
import com.google.android.material.snackbar.Snackbar;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;

import jlab.floatingfolder.db.ApplicationDetails;

import static java.lang.Math.max;
import static java.util.Collections.sort;

public class Utils {

    public static final String FOLDER_CHANGED_ACTION = "jlab.action.FOLDER_CHANGED_ACTION",
            FOLDER_REMOVE_ACTION = "jlab.action.FOLDER_REMOVE_ACTION";
    private static final LruCache<String, Bitmap> iconsCache = new LruCache<>(500);
    private static ArrayList<ApplicationInfo> allApps = new ArrayList<>();
    private static Semaphore semaphore = new Semaphore(1);
    public static Activity activity;

    public static boolean isValidApp(PackageManager packageManager, String packageName) {
        return packageManager.getLaunchIntentForPackage(packageName) != null;
    }

    public static void resetAllApps () {
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            allApps.clear();
            semaphore.release();
        }
    }

    public static List<ApplicationDetails> getAllApplications(Context context, String query) {
        ArrayList<ApplicationDetails> result = new ArrayList<>();
        try {
            semaphore.acquire();
            if (query != null)
                query = query.toLowerCase();
            PackageManager pm = context.getPackageManager();
            boolean allAppsLoaded = allApps.size() > 0;
            List<ApplicationInfo> appsInfo = allAppsLoaded ? allApps : pm.getInstalledApplications(0);
            for (int i = 0; i < appsInfo.size(); i++) {
                ApplicationInfo current = appsInfo.get(i);
                if (allAppsLoaded || isValidApp(pm, current.packageName)) {
                    if (!allAppsLoaded && (query == null || query.isEmpty()))
                        allApps.add(current);
                    CharSequence name = pm.getApplicationLabel(current);
                    if (query == null || (name != null && name.toString().toLowerCase()
                            .contains(query))) {
                        current.name = name.toString();
                        result.add(new ApplicationDetails(0, -1,
                                current.packageName, current.name));
                    }
                }
            }
            sort(result, new Comparator<ApplicationDetails>() {
                @Override
                public int compare(ApplicationDetails o1, ApplicationDetails o2) {
                    return o1.getPackName().compareTo(o2.getPackName());
                }
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            semaphore.release();
        }
        return result;
    }

    public static Bitmap getIconForApp(String packageName, Context context) {
        Bitmap result = getIconForAppInCache(packageName);
        boolean inCache = result != null;
        if (!inCache)
            try {
                PackageManager pm = context.getPackageManager();
                Drawable icon = pm.getApplicationIcon(packageName);
                if (icon != null)
                    result = getBitmapFromDrawable(icon);
            } catch (Exception ignored) {
                //TODO: disable log
                //ignored.printStackTrace();
            }
        if (result == null)
            result = BitmapFactory.decodeResource(context.getResources(), R.drawable.icon);

        if (!inCache)
            iconsCache.put(packageName, result);
        return result;
    }

    private static Bitmap getBitmapFromDrawable(Drawable drawable) {
        final Bitmap bm = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bm);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bm;
    }

    public static Bitmap getIconForAppInCache(String packageName) {
        return iconsCache.get(packageName);
    }

    public static void freeMemory() {
        iconsCache.evictAll();
    }

    public static void rateApp(Activity activity) {
        Uri uri = Uri.parse(String.format("market://details?id=%s", activity.getPackageName()));
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        try {
            activity.startActivity(goToMarket);
        } catch (Exception ignored) {
            //TODO: disable log
            //ignored.printStackTrace();
            activity.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse(String.format("https://play.google.com/store/apps/details?id=%s"
                            , activity.getPackageName()))));
        }
    }

    public static void showAboutDialog(final Activity activity, final View viewForSnack) {
        try {
            new AlertDialog.Builder(activity, R.style.AppTheme_AlertDialog)
                    .setTitle(R.string.about)
                    .setMessage(R.string.about_content)
                    .setPositiveButton(R.string.accept, null)
                    .setNegativeButton(activity.getString(R.string.contact), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            try {
                                activity.startActivity(new Intent(Intent.ACTION_SENDTO)
                                        .setData(Uri.parse(String.format("mailto:%s", activity.getString(R.string.mail)))));
                            } catch (Exception ignored) {
                                //TODO: disable log
                                //ignored.printStackTrace();
                                Utils.showSnackBar(R.string.app_mail_not_found, viewForSnack);
                            }
                        }
                    })
                    .show();
        } catch (Exception ignored) {
            //TODO: disable log
            //ignored.printStackTrace();
        }
    }

    private static Snackbar createSnackBar(int message, View view) {
        return Snackbar.make(view, message, Snackbar.LENGTH_LONG);
    }

    public static Snackbar createSnackBar(String message, View view) {
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);
        ((TextView) snackbar.getView().findViewById(R.id.snackbar_text))
                .setTextColor(view.getResources().getColor(R.color.gray));
        snackbar.getView().setBackgroundResource(R.color.white);
        return snackbar;
    }

    public static Snackbar createSnackBar(Spanned message, View view) {
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);
        ((TextView) snackbar.getView().findViewById(R.id.snackbar_text))
                .setTextColor(view.getResources().getColor(R.color.gray));
        snackbar.getView().setBackgroundResource(R.color.white);
        return snackbar;
    }

    public static void showSnackBar(int msg, View view) {
        Snackbar snackbar = createSnackBar(msg, view);
        ((TextView) snackbar.getView().findViewById(R.id.snackbar_text))
                .setTextColor(view.getResources().getColor(R.color.gray));
        snackbar.getView().setBackgroundResource(R.color.white);
        snackbar.setActionTextColor(view.getResources().getColor(R.color.accent));
        snackbar.show();
    }

    public static void showSnackBar(String msg, View view) {
        Snackbar snackbar = Utils.createSnackBar(msg, view);
        ((TextView) snackbar.getView().findViewById(R.id.snackbar_text))
                .setTextColor(view.getResources().getColor(R.color.gray));
        snackbar.getView().setBackgroundResource(R.color.white);
        snackbar.show();
    }

    public static void showSnackBar(Spanned msg, View view) {
        Snackbar snackbar = Utils.createSnackBar(msg, view);
        ((TextView) snackbar.getView().findViewById(R.id.snackbar_text))
                .setTextColor(view.getResources().getColor(R.color.gray));
        snackbar.getView().setBackgroundResource(R.color.white);
        snackbar.show();
    }

    public static String getSizeString(double mSize, int dec) {
        String type = "B";
        if (mSize > 1024) {
            mSize /= 1024;
            type = "KB";
        }
        if (mSize > 1024) {
            mSize /= 1024;
            type = "MB";
        }
        if (mSize > 1024) {
            mSize /= 1024;
            type = "GB";
        }
        if (mSize > 1024) {
            mSize /= 1024;
            type = "TB";
        }
        StringBuilder format = new StringBuilder("###");
        if (dec > 0)
            format.append(".");
        while (dec-- > 0)
            format.append("#");
        return new StringBuilder(new DecimalFormat(format.toString())
                .format(mSize)).append(type).toString();
    }

    public static String getTimeString(float seconds) {
        StringBuilder result = new StringBuilder();
        if (seconds >= 86400) {
            seconds /= 86400;
            result.append((int) seconds).append("d");
            return result.toString();
        }
        if (seconds >= 3600) {
            seconds /= 3600;
            result.append((int) seconds).append("h");
            return result.toString();
        }
        if (seconds >= 60) {
            seconds /= 60;
            result.append((int) seconds).append("m");
            return result.toString();
        }
        result.append((int) (seconds)).append("s");
        return result.toString();
    }

    public static int getFolderColor (int color) {
        switch (color){
            case 1:
                return R.drawable.img_circle_folder_color_1;
            case 2:
                return R.drawable.img_circle_folder_color_2;
            case 3:
                return R.drawable.img_circle_folder_color_3;
            case 4:
                return R.drawable.img_circle_folder_color_4;
            case 5:
                return R.drawable.img_circle_folder_color_5;
            case 6:
                return R.drawable.img_circle_folder_color_6;
            case 7:
                return R.drawable.img_circle_folder_color_7;
            case 8:
                return R.drawable.img_circle_folder_color_8;
            case 9:
                return R.drawable.img_circle_folder_color_9;
            case 10:
                return R.drawable.img_circle_folder_color_10;
            case 12:
                return R.drawable.img_circle_folder_color_12;
            default:
                return R.drawable.img_circle_folder_color_11;
        }
    }

    public static int getBackgroundFolderColor (int color) {
        switch (color){
            case 1:
                return R.color.folder_color_1;
            case 2:
                return R.color.folder_color_2;
            case 3:
                return R.color.folder_color_3;
            case 4:
                return R.color.folder_color_4;
            case 5:
                return R.color.folder_color_5;
            case 6:
                return R.color.folder_color_6;
            case 7:
                return R.color.folder_color_7;
            case 8:
                return R.color.folder_color_8;
            case 9:
                return R.color.folder_color_9;
            case 10:
                return R.color.folder_color_10;
            case 12:
                return R.color.folder_color_12;
            default:
                return R.color.folder_color_11;
        }
    }

    public static int getTitleBarFolderColor (int color) {
        switch (color){
            case 1:
                return R.color.folder_stroke_color_1;
            case 2:
                return R.color.folder_stroke_color_2;
            case 3:
                return R.color.folder_stroke_color_3;
            case 4:
                return R.color.folder_stroke_color_4;
            case 5:
                return R.color.folder_stroke_color_5;
            case 6:
                return R.color.folder_stroke_color_6;
            case 7:
                return R.color.folder_stroke_color_7;
            case 8:
                return R.color.folder_stroke_color_8;
            case 9:
                return R.color.folder_stroke_color_9;
            case 10:
                return R.color.folder_stroke_color_10;
            case 12:
                return R.color.folder_stroke_color_12;
            default:
                return R.color.folder_stroke_color_11;
        }
    }

    public static void launchApp (Activity activity, String packName) {
        try {
            if (activity == null)
                activity = Utils.activity;
            if (activity != null) {
                Intent intent = activity.getPackageManager()
                        .getLaunchIntentForPackage(packName);
                if (intent != null) {
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    activity.startActivity(intent);
                }
            }
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
    }

    public static void animateChangeHeightEvent(final View view, final int fromHeight,
                                                final int toHeight, final Runnable onPostAnim) {
        final boolean isCollapsed = fromHeight > toHeight;
        final int dif = Math.abs(toHeight - fromHeight);
        Animation anim = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                view.getLayoutParams().height = !isCollapsed
                        ? fromHeight + (int) (dif * interpolatedTime)
                        : fromHeight - (int) (dif * interpolatedTime);
                view.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };
        int duration = (int) (dif / view.getContext().getResources().getDisplayMetrics().density);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                onPostAnim.run();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        anim.setDuration(duration);
        view.startAnimation(anim);
    }

    public static DisplayMetrics getDimensionScreen(Activity currentActivity) {
        if (currentActivity == null)
            currentActivity = activity;
        DisplayMetrics displaymetrics = new DisplayMetrics();
        if (activity != null)
            currentActivity.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        return displaymetrics;
    }

    public static void showAnimation (final Activity activity, final View rootView, final Point fromPoint,
                                      final boolean isEnter, final Runnable postOnAnim) {
        showAnimation(activity, rootView, fromPoint, true, isEnter, 0, -1, postOnAnim);
    }

    public static void showAnimation (final Activity activity, final View rootView, final Point fromPoint
            , final boolean posInScreen, final boolean isEnter, final int minRadius, final int maxRadius,
                                      final Runnable postOnAnim) {
        Activity activity1 = activity;
        if (activity1 == null)
            activity1 = Utils.activity;
        final Activity finalActivity = activity1;
        if (finalActivity != null) {
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    final DisplayMetrics dimen = Utils.getDimensionScreen(finalActivity);
                    Point posMenu = posInScreen
                            ? new Point((dimen.widthPixels - rootView.getWidth()) / 2
                            , (dimen.heightPixels - rootView.getHeight()) / 2)
                            : new Point(0, 0);
                    int radius = maxRadius < 0
                            ? max(rootView.getWidth(), rootView.getHeight())
                            : maxRadius;
                    float startRadius = isEnter ? minRadius : radius,
                            endRadius = isEnter ? radius : minRadius;
                    Animator animator = ViewAnimationUtils.createCircularReveal(rootView,
                            fromPoint.x - posMenu.x,
                            fromPoint.y - posMenu.y,
                            startRadius, endRadius);
                    animator.setInterpolator(AnimationUtils.loadInterpolator(finalActivity,
                            android.R.interpolator.fast_out_linear_in));
                    animator.setDuration(250);
                    animator.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            if (postOnAnim != null)
                                postOnAnim.run();
                        }
                    });
                    animator.start();
                } else if (postOnAnim != null)
                    postOnAnim.run();
            } catch (Exception ignored) {
                ignored.printStackTrace();
            }
        }
    }

    public static View getRootView(View view) {
        ViewParent parent = view.getParent();
        while (parent != null) {
            parent = parent.getParent();
            if (parent instanceof View)
                view = (View) parent;
        }
        return view;
    }
}
