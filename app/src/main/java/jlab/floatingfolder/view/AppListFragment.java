package jlab.floatingfolder.view;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import android.app.DialogFragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.widget.SearchView;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import jlab.floatingfolder.R;
import jlab.floatingfolder.Utils;
import jlab.floatingfolder.db.ApplicationDbManager;
import jlab.floatingfolder.db.ApplicationDetails;
import jlab.floatingfolder.db.FolderDetails;
import static java.util.Collections.binarySearch;
import static jlab.floatingfolder.Utils.getRootView;
import static jlab.floatingfolder.view.AddEditFolderFragment.END_SHOW_FOLDER_DATA_FRAGMENT_ACTION;

/**
 * Created by Javier on 28/12/2020.
 */

public class AppListFragment extends DialogFragment implements
        AppListAdapter.IOnManagerContentListener<ApplicationDetails>, OnReloadListener {

    private static final int CHECK_SWITCH_STATE = 1, NEUTRAL_SWITCH_STATE = 0;
    public static final int RUN_ON_REFRESH_DETAILS_LISTENER = 9300, ON_LOAD_CONTENT_FINISH = 9301;
    public static final String QUERY_KEY = "QUERY_KEY", FOLDER_DETAILS_KEY = "FOLDER_ID_KEY",
            FROM_POINT_ANIMATION_KEY = "FROM_POINT_ANIMATION_KEY";
    protected AppListAdapter adapter;
    protected Semaphore semaphoreLoadIcon = new Semaphore(3),
            semaphoreReload = new Semaphore(1);
    protected ApplicationDbManager appDbMgr;
    protected TextView tvEmptyList;
    protected List<ApplicationDetails> content = new ArrayList<>();
    private SearchView svSearch;
    protected String query;
    private FolderDetails folder;
    protected Runnable onRefreshDetailsListener = new Runnable() {
        @Override
        public void run() {
        }
    };
    protected static OnRunOnUiThread onRunOnUiThread = new OnRunOnUiThread() {
        @Override
        public void runOnUiThread(Runnable runnable) {
        }
    };
    protected Handler handler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case RUN_ON_REFRESH_DETAILS_LISTENER:
                    refreshDetails();
                    break;
                case ON_LOAD_CONTENT_FINISH:
                    adapter.reload(content);
                    pbLoading.setVisibility(View.INVISIBLE);
                    tvEmptyList.setVisibility(adapter.getCount() == 0
                            ? View.VISIBLE
                            : View.INVISIBLE);
                    refreshDetails();
                    semaphoreReload.release();
                    break;
                default:
                    break;
            }
            return false;
        }
    });
    private ProgressBar pbLoading;
    private Point fromPoint;
    private AlertDialog dialog;
    private View rootView;
    private View.OnClickListener closeOnClick;

    public AppListFragment () {
        super();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        loadDataFromBundle(savedInstanceState);
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        final View view = inflater.inflate(R.layout.app_list_fragment, null, false);
        TextView tvFolderName = view.findViewById(R.id.tvFolderName);
        this.tvEmptyList = view.findViewById(R.id.tvEmptyList);
        this.svSearch = view.findViewById(R.id.svSearch);
        LinearLayout llTitleFolder = view.findViewById(R.id.llFolderTitle);
        llTitleFolder.setBackgroundResource
                (Utils.getTitleBarFolderColor(folder.getColor()));
        tvFolderName.setText(folder.getName());
        pbLoading = view.findViewById(R.id.pbLoading);
        final ListView lvAppList = view.findViewById(R.id.lvAppList);
        lvAppList.setAdapter(adapter);

        this.svSearch.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                query = null;
                return true;
            }
        });
        this.svSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                query = newText.toLowerCase();
                reload();
                return true;
            }
        });
        if (this.svSearch != null) {
            if (query != null && !query.isEmpty() && !this.svSearch.getQuery()
                    .toString().equals(query))
                this.svSearch.setQuery(query, false);
        }
        rootView = view;
        closeOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendOnHideBroadcast();
            }
        };

        AlertDialog.Builder adBuilder = new AlertDialog.Builder(inflater.getContext(), R.style.AppTheme_AlertDialog)
                .setView(view)
                .setPositiveButton(R.string.close, null);
        dialog = adBuilder.create();
        rootView.post(new Runnable() {
            @Override
            public void run() {
                rootView = getRootView(rootView);
                Utils.showAnimation(getActivity(), rootView, fromPoint, true, null);
            }
        });
        reload();
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(closeOnClick);
        startAnimation();
    }

    private void startAnimation() {
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(QUERY_KEY, query);
        outState.putParcelable(FOLDER_DETAILS_KEY, folder);
        outState.putParcelable(FROM_POINT_ANIMATION_KEY, fromPoint);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.appDbMgr = new ApplicationDbManager(getActivity());
        adapter = new AppListAdapter(getActivity());
        adapter.setOnManagerContentListener(this);
        setCancelable(false);
        setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        loadDataFromBundle(savedInstanceState);
    }

    private void loadDataFromBundle(Bundle bundle) {
        if(bundle == null)
            bundle = getArguments();

        if (bundle != null && bundle.containsKey(QUERY_KEY))
            this.query = bundle.getString(QUERY_KEY);
        if(bundle != null && bundle.containsKey(FOLDER_DETAILS_KEY))
            this.folder = bundle.getParcelable(FOLDER_DETAILS_KEY);
        if(bundle != null && bundle.containsKey(FROM_POINT_ANIMATION_KEY))
            this.fromPoint = bundle.getParcelable(FROM_POINT_ANIMATION_KEY);
    }

    @Override
    public void reload() {
        this.pbLoading.setVisibility(View.VISIBLE);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    semaphoreReload.acquire();
                } catch (InterruptedException e) {
                    //TODO: disable log
                    //e.printStackTrace();
                } finally {
                    content = getContent();
                    handler.sendEmptyMessage(ON_LOAD_CONTENT_FINISH);
                }
            }
        }).start();
    }

    @Override
    public void refreshDetails() {
        onRefreshDetailsListener.run();
    }

    @Override
    public void setOnRefreshDetailsListener(Runnable newOnRefreshDetails) {
        this.onRefreshDetailsListener = newOnRefreshDetails;
    }

    @Override
    public boolean hasDetails() {
        return false;
    }

    @Override
    public int getCount() {
        return content.size();
    }

    @Override
    public String getName(Context context) {
        return context.getString(R.string.app_list);
    }

    @Override
    public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
        convertView = LayoutInflater.from(getActivity().getBaseContext())
                .inflate(R.layout.app_details_in_list, parent, false);
        final ApplicationDetails current = adapter.getItem(position);
        final TextView packNames = convertView.findViewById(R.id.tvPackagesName),
                name = convertView.findViewById(R.id.tvName);
        final ImageView icon = convertView.findViewById(R.id.ivIcon);
        final SwitchMultiOptionButton swInFolder = convertView.findViewById(R.id.swInternetStatus);
        if (current != null) {
            packNames.setText(current.getPackName());
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final SpannableStringBuilder text = getSpannableFromText(current.getName());
                    onRunOnUiThread.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            name.setText(text);
                        }
                    });
                }
            }).start();
            Bitmap bmInCache = Utils.getIconForAppInCache(current.getPackName());
            if (bmInCache != null)
                Glide.with(icon).asBitmap().load(bmInCache).into(icon);
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
                            final Bitmap bm = current.getIcon(getActivity());
                            onRunOnUiThread.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Glide.with(icon).asBitmap().load(bm)
                                                .into(icon);
                                        icon.startAnimation(AnimationUtils.loadAnimation(getActivity()
                                                , R.anim.fast_fade_in));
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
            swInFolder.setOnSwitchListener(new OnSwitchListener() {
                @Override
                public void onSwitchChange(int state) {
                    ApplicationDetails current = content.get(position);
                    if (state == CHECK_SWITCH_STATE) {
                        current.setFolderId(folder.getId());
                        current.setId((int) appDbMgr.addApplicationData(current, folder, getActivity()));
                    } else {
                        current.setFolderId(-1);
                        appDbMgr.deleteApplicationData(current, folder, getActivity());
                    }
                }

                @Override
                public int countStates() {
                    return 2;
                }

                @Override
                public int getBackground(int state) {
                    switch (state) {
                        case NEUTRAL_SWITCH_STATE:
                            return R.drawable.img_neutral;
                        default:
                            return R.drawable.img_checked;
                    }
                }
            });
            swInFolder.setState(getSwitchStateFromAppDetails(current));
        }
        return convertView;
    }

    private int getSwitchStateFromAppDetails(ApplicationDetails details) {
        if (details.getFolderId() == folder.getId())
            return CHECK_SWITCH_STATE;
        return NEUTRAL_SWITCH_STATE;
    }

    @Override
    public List<ApplicationDetails> getContent() {
        content = Utils.getAllApplications(getActivity(), query);
        ArrayList<ApplicationDetails> appsFolder = appDbMgr.getAppsForFolder(folder.getId(), query);
        for (int i = 0; i < appsFolder.size(); i++) {
            int index = binarySearch(content, appsFolder.get(i), new Comparator<ApplicationDetails>() {
                @Override
                public int compare(ApplicationDetails o1, ApplicationDetails o2) {
                    return o1.getPackName().compareTo(o2.getPackName());
                }
            });
            if (index >= 0)
                content.set(index, appsFolder.get(i));
        }
        return content;
    }

    public static void setOnRunOnUiThread(OnRunOnUiThread onRunOnUiThread) {
        AppListFragment.onRunOnUiThread = onRunOnUiThread;
    }

    protected SpannableStringBuilder getSpannableFromText (String text) {
        SpannableStringBuilder strBuilder = new SpannableStringBuilder(text);
        if(query != null) {
            int index = text.toLowerCase().indexOf(query);
            if(index > -1) {
                BackgroundColorSpan colorSpan = new BackgroundColorSpan(getResources()
                        .getColor(R.color.neutral));
                strBuilder.setSpan(colorSpan, index, index + query.length(), 0);
            }
        }
        Selection.selectAll(strBuilder);
        return strBuilder;
    }

    private void sendOnHideBroadcast (){
        Utils.showAnimation(getActivity(), rootView, fromPoint, false, new Runnable() {
            @Override
            public void run() {
                dismiss();
            }
        });
        LocalBroadcastManager.getInstance(getActivity())
                .sendBroadcast(new Intent(END_SHOW_FOLDER_DATA_FRAGMENT_ACTION));
    }
}