package jlab.floatingfolder.view;

import android.content.Context;
import androidx.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import java.util.ArrayList;
import java.util.List;
import jlab.floatingfolder.db.ApplicationDetails;

/*
 * Created by Javier on 27/12/2020.
 */

public class AppListAdapter extends ArrayAdapter<ApplicationDetails> {

    private IOnManagerContentListener onMgrContentListener = new IOnManagerContentListener() {
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            return null;
        }

        @Override
        public List<ApplicationDetails> getContent() {
            return new ArrayList<>();
        }
    };

    public AppListAdapter(Context context) {
        super(context, 0);
    }

    public AppListAdapter(Context context, List<ApplicationDetails> list) {
        super(context, 0, list);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        convertView = this.onMgrContentListener.getView(position, convertView, parent);
        return convertView;
    }

    public void reload (List<ApplicationDetails> content) {
        clear();
        addAll(content);
    }

    public void setOnManagerContentListener(IOnManagerContentListener onGetViewListener) {
        this.onMgrContentListener = onGetViewListener;
    }

    public interface IOnManagerContentListener<T> {
        View getView (int position, View convertView, @NonNull ViewGroup parent);
        List<T> getContent ();
    }
}
