package jlab.floatingfolder.view;

import android.graphics.Point;
import android.os.Bundle;
import android.view.View;
import jlab.floatingfolder.R;
import android.content.Intent;
import android.view.animation.AnimationUtils;
import android.app.DialogFragment;
import android.view.LayoutInflater;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.cardview.widget.CardView;
import jlab.floatingfolder.Utils;
import jlab.floatingfolder.db.FolderDetails;
import jlab.floatingfolder.db.ApplicationDbManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import static jlab.floatingfolder.view.AppListFragment.FOLDER_DETAILS_KEY;
import static jlab.floatingfolder.view.AppListFragment.FROM_POINT_ANIMATION_KEY;

public class AddEditFolderFragment extends DialogFragment {

    private FolderDetails folder;
    private Point fromPoint;
    public final static String END_SHOW_FOLDER_DATA_FRAGMENT_ACTION
            = "jlab.END_SHOW_FOLDER_DATA_FRAGMENT_ACTION";
    private ApplicationDbManager appMgr;
    public static final int[] btColorIds = new int[] {R.id.cvColor1,
            R.id.cvColor2,R.id.cvColor3,R.id.cvColor4,R.id.cvColor5,R.id.cvColor6,
            R.id.cvColor7,R.id.cvColor8,R.id.cvColor9,R.id.cvColor10,R.id.cvColor11,
            R.id.cvColor12};
    private AlertDialog dialog;
    private View.OnClickListener cancelOnClick, saveOnClick;
    private View rootView;

    public AddEditFolderFragment () {
        super();
    }

    @Override
    public void onCreate(Bundle saveInstance) {
        super.onCreate(saveInstance);
        setCancelable(false);
        setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        appMgr = new ApplicationDbManager(getActivity());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(FOLDER_DETAILS_KEY, this.folder);
        outState.putParcelable(FROM_POINT_ANIMATION_KEY, fromPoint);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public AlertDialog onCreateDialog(Bundle saveInstance) {
        if(saveInstance == null)
            saveInstance = getArguments();

        if (saveInstance != null && saveInstance.containsKey(FOLDER_DETAILS_KEY))
            this.folder = saveInstance.getParcelable(FOLDER_DETAILS_KEY);
        if(saveInstance != null && saveInstance.containsKey(FROM_POINT_ANIMATION_KEY))
            this.fromPoint = saveInstance.getParcelable(FROM_POINT_ANIMATION_KEY);


        LayoutInflater inflater = LayoutInflater.from(getActivity());
        final View fragmentView = inflater.inflate(R.layout.add_edit_folder, null, false);

        final AppCompatEditText etName = fragmentView.findViewById(R.id.etNameFolder);
        etName.setText(folder.getName());
        for(int i = 0; i < btColorIds.length; i++) {
            final CardView btColor = (CardView) fragmentView.findViewById(btColorIds[i]);
            if(folder.getColor() == i + 1)
                btColor.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.fast_beat));
            final int finalI = i;
            btColor.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CardView aux = (CardView) fragmentView.findViewById(btColorIds[folder.getColor() - 1]);
                    aux.clearAnimation();
                    btColor.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.fast_beat));
                    folder.setColor(finalI + 1);
                }
            });
            btColor.setOnTouchListener(SwitchMultiOptionButton.viewOnTouchListener());
        }
        cancelOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendOnHideBroadcast();
            }
        };
        saveOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                folder.setName(etName.getText().toString().trim());
                if (folder.getId() == -1)
                    appMgr.addFolderData(folder);
                else
                    appMgr.updateFolderData(folder.getId(), getActivity(), folder);
                sendOnHideBroadcast();
            }
        };
        rootView = fragmentView;
        AlertDialog.Builder adBuilder = new AlertDialog.Builder(inflater.getContext(), R.style.AppTheme_AlertDialog)
                .setView(fragmentView)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null);
        dialog = adBuilder.create();
        rootView.post(new Runnable() {
            @Override
            public void run() {
                rootView = Utils.getRootView(rootView);
                Utils.showAnimation(getActivity(), rootView, fromPoint, true, null);
            }
        });
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(saveOnClick);
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(cancelOnClick);
        startAnimation();
    }

    private void startAnimation() {
    }

    private void sendOnHideBroadcast () {
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