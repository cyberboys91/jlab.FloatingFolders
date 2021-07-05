package jlab.floatingfolder.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import jlab.floatingfolder.R;
import jlab.floatingfolder.Utils;
import jlab.floatingfolder.db.ApplicationDbManager;
import jlab.floatingfolder.db.ApplicationDetails;
import jlab.floatingfolder.db.FolderDetails;

import static java.lang.Math.max;
import static java.lang.Math.min;

/*
 * Created by Javier on 27/06/2021.
 */

public class FloatingFolderView extends LinearLayout {

    private boolean isExpanded = false;
    private FolderDetails folder;
    private ApplicationDbManager appMgr;
    private Semaphore semaphoreLoadIcon = new Semaphore(6);
    private TextView tvTitle;
    private GridView gvAppsInFolder;
    private CardView cvFolderWrapper;
    private LinearLayout llContentTitle;
    private Semaphore semaphore = new Semaphore(1);
    private DisplayMetrics dimen = Utils.getDimensionScreen(null);
    private Point posCollapsedInScreen = new Point(0, 0),
            posCollapsedInWindow = new Point(0, 0);
    private ImageView ivCollapsed, ivAppInFloatCollapsedFolder0, ivAppInFloatCollapsedFolder1,
            ivAppInFloatCollapsedFolder2, ivAppInFloatCollapsedFolder3, ivAppInFloatCollapsedFolder4;
    private static OnRunOnUiThread onRunOnUiThread = new OnRunOnUiThread() {
        @Override
        public void runOnUiThread(Runnable runnable) {

        }
    };
    private WindowManager windowMgr;
    private WindowManager.LayoutParams paramsCollapsed = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    : WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSPARENT),
            paramsExpanded = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                                    : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSPARENT);
    private View llFloatingFolderWrapper;

    public static void setOnRunOnUiThread(OnRunOnUiThread onRunOnUiThread) {
        FloatingFolderView.onRunOnUiThread = onRunOnUiThread;
    }

    public WindowManager.LayoutParams setOnTouchListener (final WindowManager windowMgr) {
        this.windowMgr = windowMgr;

        setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            float initialTouchX, initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!isExpanded)
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            initialX = paramsCollapsed.x;
                            initialY = paramsCollapsed.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            v.setAlpha(.5f);
                            break;
                        case MotionEvent.ACTION_MOVE:
                            paramsCollapsed.x = initialX + (int) (event.getRawX() - initialTouchX);
                            paramsCollapsed.y = initialY + (int) (event.getRawY() - initialTouchY);
                            try {
                                windowMgr.updateViewLayout(FloatingFolderView.this, paramsCollapsed);
                            } catch (Exception ignored) {
                                ignored.printStackTrace();
                            }
                            v.setAlpha(.5f);
                            break;
                        case MotionEvent.ACTION_UP:
                            v.setAlpha(1f);
                            break;
                        case MotionEvent.ACTION_HOVER_ENTER:
                        case MotionEvent.ACTION_HOVER_EXIT:
                        case MotionEvent.ACTION_HOVER_MOVE:
                            v.setAlpha(.5f);
                            break;
                        default:
                            v.setAlpha(1f);
                            break;
                    }
                return false;
            }
        });
        return paramsCollapsed;
    }

    public FloatingFolderView(Context context) {
        super(context);
    }

    public FloatingFolderView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FloatingFolderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void addContentView () {
        appMgr = new ApplicationDbManager(getContext());
        collapseView();
    }

    public void setFolder (FolderDetails folder) {
        try {
            semaphore.acquire();
            this.folder = folder;
            addContentView();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            semaphore.release();
        }
    }

    public FolderDetails getFolder () {
        return this.folder;
    }

    public void folderChange (FolderDetails newFolder) {
        try {
            semaphore.acquire();
            if (folder.getId() == newFolder.getId() && folder.isFloating()) {
                folder = newFolder;
                if (isExpanded)
                    setExpandedProperties();
                else
                    setCollapsedProperties();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            semaphore.release();
        }
    }

    private void expandView() {
        int[] location = new int[2];
        getChildAt(0).getLocationOnScreen(location);
        int collapsedSize = getResources().getDimensionPixelSize(R.dimen.floating_collapsed_card_view_width);
        posCollapsedInScreen = new Point(location[0] + collapsedSize / 2,
                location[1] + collapsedSize / 2);
        getChildAt(0).getLocationInWindow(location);
        posCollapsedInWindow = new Point(location[0] + collapsedSize / 2,
                location[1] + collapsedSize / 2);
        removeAllViews();
        View.inflate(getContext(), R.layout.floating_folder_expand_content, this);
        try {
            windowMgr.updateViewLayout(this, paramsExpanded);
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
        isExpanded = true;
        tvTitle = findViewById(R.id.tvFolderName);
        llContentTitle = findViewById(R.id.llFolderTitle);
        gvAppsInFolder = findViewById(R.id.gvAppsInFolder);
        cvFolderWrapper = findViewById(R.id.cvFolderWrapper);
        setExpandedProperties();
    }

    private void closeExpanded () {
        Animation animation = AnimationUtils.loadAnimation(cvFolderWrapper.getContext(),
                R.anim.fast_fade_out);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                cvFolderWrapper.setVisibility(INVISIBLE);
                collapseView();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        llFloatingFolderWrapper.startAnimation(animation);
    }

    private void setExpandedProperties() {
        tvTitle.setText(folder.getName());
        int folderBackgroundColor = getResources()
                .getColor(Utils.getBackgroundFolderColor(folder.getColor()));
        cvFolderWrapper.setCardBackgroundColor(folderBackgroundColor);
        tvTitle.setTextColor(folderBackgroundColor);
        llContentTitle.setBackgroundResource(Utils.getTitleBarFolderColor(folder.getColor()));
        llFloatingFolderWrapper = findViewById(R.id.llFloatingFolderWrapper);
        cvFolderWrapper.post(new Runnable() {
            @Override
            public void run() {
                int cardWidth = cvFolderWrapper.getWidth();
                int posX = (int) (posCollapsedInScreen.x -  cardWidth/ 2),
                        posY = (int) (posCollapsedInScreen.y - cardWidth / 2);
                posX = max(posX, 0);
                posY = max(posY, 0);
                posX = min(dimen.widthPixels - cardWidth, posX);
                posY = min(dimen.heightPixels - cardWidth, posY);
                cvFolderWrapper.setX(posX);
                cvFolderWrapper.setY(posY);
                cvFolderWrapper.setVisibility(VISIBLE);
                cvFolderWrapper.startAnimation(AnimationUtils
                        .loadAnimation(cvFolderWrapper.getContext(), R.anim.fast_fade_in));
            }
        });
        llFloatingFolderWrapper.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                closeExpanded();
            }
        });
        llFloatingFolderWrapper
                .startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.fade_in));

        final AppListAdapter.IOnManagerContentListener<ApplicationDetails> appDetailsMgr =
                appDetailsContentMgr(folder);
        final AppListAdapter adapter = new AppListAdapter(getContext());
        adapter.setOnManagerContentListener(appDetailsMgr);
        gvAppsInFolder.setAdapter(adapter);
        new Thread(new Runnable() {
            @Override
            public void run() {
                onRunOnUiThread.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.reload(appDetailsMgr.getContent());
                    }
                });
            }
        }).start();
    }

    private void collapseView() {
        removeAllViews();
        setVisibility(INVISIBLE);
        View.inflate(getContext(), R.layout.floating_folder_collapsed_content,
                FloatingFolderView.this);
        try {
            windowMgr.updateViewLayout(this, paramsCollapsed);
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
        isExpanded = false;
        cvFolderWrapper = findViewById(R.id.cvFolderWrapper);
        ivCollapsed = findViewById(R.id.ivFloatCollapsedFolder);
        ivAppInFloatCollapsedFolder0 = findViewById(R.id.ivAppInFloatCollapsedFolder0);
        ivAppInFloatCollapsedFolder1 = findViewById(R.id.ivAppInFloatCollapsedFolder1);
        ivAppInFloatCollapsedFolder2 = findViewById(R.id.ivAppInFloatCollapsedFolder2);
        ivAppInFloatCollapsedFolder3 = findViewById(R.id.ivAppInFloatCollapsedFolder3);
        ivAppInFloatCollapsedFolder4 = findViewById(R.id.ivAppInFloatCollapsedFolder4);
        setCollapsedProperties();
    }

    private void setCollapsedProperties() {
        ivCollapsed.setBackgroundResource(Utils.getFolderColor(folder.getColor()));
        ivAppInFloatCollapsedFolder0.setImageBitmap(null);
        final ArrayList<ApplicationDetails> appFolders = (ArrayList<ApplicationDetails>)
                appDetailsContentMgr(folder).getContent();
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (appFolders.size() == 1)
                    Utils.launchApp(null, appFolders.get(0).getPackName());
                else if (!isExpanded)
                    expandView();
            }
        });

        if (!appFolders.isEmpty()) {
            if (appFolders.size() == 1) {
                ivAppInFloatCollapsedFolder1
                        .setImageResource(R.color.transparent);
                ivAppInFloatCollapsedFolder0.setImageBitmap(Utils.getIconForApp(appFolders.get(0).getPackName(),
                        getContext()));
            } else
                ivAppInFloatCollapsedFolder1
                        .setImageBitmap(Utils.getIconForApp(appFolders.get(0).getPackName(), getContext()));
            if (appFolders.size() > 1)
                ivAppInFloatCollapsedFolder2
                        .setImageBitmap(Utils.getIconForApp(appFolders.get(1).getPackName(), getContext()));
            else
                ivAppInFloatCollapsedFolder2
                        .setImageResource(R.color.transparent);
            if (appFolders.size() > 2)
                ivAppInFloatCollapsedFolder3
                        .setImageBitmap(Utils.getIconForApp(appFolders.get(2).getPackName(), getContext()));
            else
                ivAppInFloatCollapsedFolder3
                        .setImageResource(R.color.transparent);
            if (appFolders.size() > 3)
                ivAppInFloatCollapsedFolder4
                        .setImageBitmap(Utils.getIconForApp(appFolders.get(3).getPackName(), getContext()));
            else
                ivAppInFloatCollapsedFolder4
                        .setImageResource(R.color.transparent);
        }
        cvFolderWrapper.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.fade_in));
        setVisibility(VISIBLE);
    }

    private AppListAdapter.IOnManagerContentListener<ApplicationDetails> appDetailsContentMgr (final FolderDetails folder) {
        return new AppListAdapter.IOnManagerContentListener<ApplicationDetails>() {
            ArrayList<ApplicationDetails> appsForFolder = new ArrayList<>();
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.app_in_folder_details, parent, false);
                final ApplicationDetails currentApp = appsForFolder.get(position);
                final ImageView ivIconApp = convertView.findViewById(R.id.ivIcon);
                final TextView tvName = convertView.findViewById(R.id.tvName);
                convertView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Utils.launchApp(null, currentApp.getPackName());
                        closeExpanded();
                    }
                });
                convertView.setOnTouchListener(SwitchMultiOptionButton.viewOnTouchListener());
                tvName.setTextColor(getResources().getColor(Utils.getTitleBarFolderColor(folder.getColor())));
                tvName.setText(currentApp.getName());
                final Bitmap bmInCache = Utils.getIconForAppInCache(currentApp.getPackName());
                if (bmInCache != null)
                    Glide.with(ivIconApp).asBitmap().load(bmInCache).into(ivIconApp);
                else {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                semaphoreLoadIcon.acquire();
                            } catch (InterruptedException e) {
                                //TODO: disable log
                                //e.printStackTrace();
                            } finally {
                                final Bitmap bm = currentApp.getIcon(getContext());
                                onRunOnUiThread.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            Glide.with(ivIconApp).asBitmap().load(bm)
                                                    .into(ivIconApp);
                                            ivIconApp.startAnimation(AnimationUtils
                                                    .loadAnimation(getContext(), R.anim.fast_fade_in));
                                        } catch (Exception ignored) {
                                            //TODO: disable log
                                            //ignored.printStackTrace();
                                        }
                                    }
                                });
                                semaphoreLoadIcon.release();
                            }
                        }
                    }).start();
                }
                return convertView;
            }

            @Override
            public ArrayList<ApplicationDetails> getContent() {
                appsForFolder = appMgr.getAppsForFolder(folder.getId(), "");
                return appsForFolder;
            }
        };
    }
}
