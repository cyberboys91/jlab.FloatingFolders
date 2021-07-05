package jlab.floatingfolder.view;

/*
 * Created by Javier on 02/01/2021.
 */

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import jlab.floatingfolder.R;

public class SwitchMultiOptionButton extends LinearLayout {

    private int state = 0;

    private OnSwitchListener onSwitchListener = new OnSwitchListener() {
        @Override
        public void onSwitchChange(int state) {

        }

        @Override
        public int countStates() {
            return 2;
        }

        @Override
        public int getBackground(int state) {
            return 0;
        }
    };

    public SwitchMultiOptionButton(Context context) {
        super(context);
        addContentView();
    }

    public SwitchMultiOptionButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        addContentView();
    }

    public SwitchMultiOptionButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        addContentView();
    }

    private void addContentView() {
        View.inflate(getContext(), R.layout.switch_option_button, this);
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setState((state + 1) % onSwitchListener.countStates());
                onSwitchListener.onSwitchChange(state);
            }
        });
        setOnTouchListener(viewOnTouchListener());
    }

    public void setState (int state) {
        if (state < this.onSwitchListener.countStates()) {
            this.state = state;
            setBackgroundResource(onSwitchListener.getBackground(state));
        }
    }

    public int getState () {
        return state;
    }

    public void setOnSwitchListener (OnSwitchListener onSwitchListener) {
        this.onSwitchListener = onSwitchListener;
    }

    public static View.OnTouchListener viewOnTouchListener () {
        return new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_HOVER_ENTER:
                    case MotionEvent.ACTION_HOVER_EXIT:
                    case MotionEvent.ACTION_HOVER_MOVE:
                    case MotionEvent.ACTION_MOVE:
                        v.setAlpha(.5f);
                        break;
                    case MotionEvent.ACTION_UP:
                        v.setAlpha(1f);
                        break;
                    default:
                        v.setAlpha(1f);
                        break;
                }
                return false;
            }
        };
    }

}
