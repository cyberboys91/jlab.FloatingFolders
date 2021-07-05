package jlab.floatingfolder.db;

import android.provider.BaseColumns;

/*
 * Created by Javier on 24/04/2017.
 */
public class FolderContract implements BaseColumns {
    public static final String TABLE_NAME = "folders";
    public static final String NAME = "name";
    public static final String COLOR = "color";
    public static final String FLOATING = "floating";
    public static final String EXPANDED = "expanded";
}