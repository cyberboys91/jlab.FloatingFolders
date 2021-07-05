package jlab.floatingfolder.view;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import jlab.floatingfolder.db.ApplicationDetails;
import jlab.floatingfolder.db.FolderDetails;

/*
 * Created by Javier on 27/12/2020.
 */

public class FolderListAdapter extends ArrayAdapter<FolderDetails> {

    private AppListAdapter.IOnManagerContentListener onMgrContentListener = new AppListAdapter.IOnManagerContentListener() {
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            return null;
        }

        @Override
        public List<ApplicationDetails> getContent() {
            return new ArrayList<>();
        }
    };

    public FolderListAdapter(Context context, List<FolderDetails> objects) {
        super(context, 0, objects);
    }

    public FolderListAdapter(Context context) {
        super(context, 0);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        convertView = this.onMgrContentListener.getView(position, convertView, parent);
        return convertView;
    }

    public void reload (List<FolderDetails> content) {
        clear();
        addAll(content);
    }

    public void setOnManagerContentListener(AppListAdapter.IOnManagerContentListener<FolderDetails> onGetViewListener) {
        this.onMgrContentListener = onGetViewListener;
    }
}
