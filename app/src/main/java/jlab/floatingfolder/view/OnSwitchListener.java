package jlab.floatingfolder.view;

/**
 * Created by Javier on 02/01/2021.
 */

public interface OnSwitchListener {

    void onSwitchChange(int state);

    int countStates();

    int getBackground(int state);

}
