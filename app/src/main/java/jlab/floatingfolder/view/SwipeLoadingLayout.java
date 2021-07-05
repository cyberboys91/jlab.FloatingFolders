package jlab.floatingfolder.view;
/*
 * Created by Javier on 24/06/2021.
 */

import android.content.Context;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class SwipeLoadingLayout extends SwipeRefreshLayout {
    public SwipeLoadingLayout(@NonNull Context context) {
        super(context);
    }

    public SwipeLoadingLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean canChildScrollUp() {
        return true;
    }

    public void show () {
        setRefreshing(true);
    }

    public void hide () {
        setRefreshing(false);
    }
}
