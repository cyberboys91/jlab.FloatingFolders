package jlab.floatingfolder.db;

import android.provider.BaseColumns;

/*
 * Created by Javier on 24/04/2017.
 */
public class ApplicationContract implements BaseColumns {
    public static final String TABLE_NAME = "apps";
    public static final String PACKAGE_NAME = "packName";
    public static final String NAME = "name";
    public static final String FOLDER_ID = "folderId";
}