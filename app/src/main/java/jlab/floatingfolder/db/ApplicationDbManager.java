package jlab.floatingfolder.db;

import java.util.List;
import java.util.ArrayList;

import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import jlab.floatingfolder.Utils;

import static jlab.floatingfolder.view.AppListFragment.FOLDER_DETAILS_KEY;

/*
 * Created by Javier on 24/04/2017.
 */
public class ApplicationDbManager extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "jlab.floatinfolder.db";
    private static final String SQL_DELETE_APPS_ENTRIES = "DROP TABLE IF EXISTS " + ApplicationContract.TABLE_NAME;
    private static final String SQL_DELETE_FOLDERS_ENTRIES = "DROP TABLE IF EXISTS " + FolderContract.TABLE_NAME;

    public ApplicationDbManager(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL("CREATE TABLE " + ApplicationContract.TABLE_NAME + " ("
                    + ApplicationContract._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + ApplicationContract.PACKAGE_NAME + " TEXT NOT NULL,"
                    + ApplicationContract.NAME + " TEXT NOT NULL,"
                    + ApplicationContract.FOLDER_ID + " INT NOT NULL)");

            db.execSQL("CREATE TABLE " + FolderContract.TABLE_NAME + " ("
                    + FolderContract._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + FolderContract.NAME + " TEXT NOT NULL,"
                    + FolderContract.FLOATING + " INT NOT NULL,"
                    + FolderContract.EXPANDED + " INT NOT NULL,"
                    + FolderContract.COLOR + " INT NOT NULL)");

        } catch (Exception e) {
            //TODO: disable log
            //e.printStackTrace();
        }
    }

    public void addApps(SQLiteDatabase db, List<ApplicationDetails> appsDetails, int folderId) {
        for (ApplicationDetails app : appsDetails)
            addApplicationData(db, app, folderId);
    }

    public void addApps(List<ApplicationDetails> appsDetails, int folderId) {
        addApps(getWritableDatabase(), appsDetails, folderId);
    }

    public void addFolderData(FolderDetails folderDetails) {
        addFolderData(getWritableDatabase(), folderDetails);
    }

    public long addApplicationData(SQLiteDatabase sqLiteDatabase, ApplicationDetails applicationDetails, int folderId) {
        if (getFolderForId(folderId) != null)
            return sqLiteDatabase.insert(ApplicationContract.TABLE_NAME, null, applicationDetails.toContentValues());
        return -1;
    }

    public long addApplicationData(ApplicationDetails applicationDetails, FolderDetails folderDetails, Context context) {
        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        long count = sqLiteDatabase.insert(ApplicationContract.TABLE_NAME,
                null, applicationDetails.toContentValues());
        if (count > 0) {
            Intent intent = new Intent(Utils.FOLDER_CHANGED_ACTION);
            intent.putExtra(FOLDER_DETAILS_KEY, folderDetails);
            LocalBroadcastManager.getInstance(context)
                    .sendBroadcast(intent);
        }
        return count;
    }

    public long addFolderData(SQLiteDatabase sqLiteDatabase, FolderDetails folderDetails) {
        return sqLiteDatabase.insert(FolderContract.TABLE_NAME, null, folderDetails.toContentValues());
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        sqLiteDatabase.execSQL(SQL_DELETE_APPS_ENTRIES);
        sqLiteDatabase.execSQL(SQL_DELETE_FOLDERS_ENTRIES);
        onCreate(sqLiteDatabase);
    }

    public ArrayList<ApplicationDetails> getAllAppDetails(String namesQuery) {
        SQLiteDatabase sqLiteDatabase = getReadableDatabase();
        ArrayList<ApplicationDetails> result = new ArrayList<>();
        Cursor cursor = sqLiteDatabase.query(ApplicationContract.TABLE_NAME, null,
                null, null, null, null, null);
        try {
            while (cursor.moveToNext()) {
                String packName = cursor.getString(cursor.getColumnIndex(ApplicationContract.PACKAGE_NAME)),
                        name = cursor.getString(cursor.getColumnIndex(ApplicationContract.NAME));
                int id = cursor.getInt(cursor.getColumnIndex(ApplicationContract._ID)),
                        folderId = cursor.getInt(cursor.getColumnIndex(ApplicationContract.FOLDER_ID));
                if (namesQuery == null || namesQuery.isEmpty() || name.toLowerCase().contains(namesQuery))
                    result.add(new ApplicationDetails(id, folderId, packName, name));
            }
        } catch (Exception ignored) {
            //TODO: disable log
            //ignored.printStackTrace();
        } finally {
            cursor.close();
        }
        return result;
    }

    public int updateApplicationData(int id, ApplicationDetails newApplicationDetails) {
        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        return sqLiteDatabase.update(ApplicationContract.TABLE_NAME,
                newApplicationDetails.toContentValues(),
                ApplicationContract._ID + " LIKE ?",
                new String[]{Integer.toString(id)});
    }

    public int updateFolderData(int id, Context context, FolderDetails newFolderDetails) {
        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        int count = sqLiteDatabase.update(FolderContract.TABLE_NAME,
                newFolderDetails.toContentValues(),
                ApplicationContract._ID + " LIKE ?",
                new String[]{Integer.toString(id)});
        if (count > 0) {
            Intent intent = new Intent(Utils.FOLDER_CHANGED_ACTION);
            intent.putExtra(FOLDER_DETAILS_KEY, newFolderDetails);
            LocalBroadcastManager.getInstance(context)
                    .sendBroadcast(intent);
        }
        return count;
    }

    public ArrayList<ApplicationDetails> getApplicationForPackName(String packName) {
        SQLiteDatabase sqLiteDatabase = getReadableDatabase();
        Cursor cursor = sqLiteDatabase.query(ApplicationContract.TABLE_NAME, null,
                ApplicationContract.PACKAGE_NAME + " LIKE ?", new String[]{packName},
                null, null, null);
        ArrayList<ApplicationDetails> result = new ArrayList<>();
        try {
            if (cursor.moveToFirst()) {
                int id = cursor.getInt(cursor.getColumnIndex(ApplicationContract._ID));
                String name = cursor.getString(cursor.getColumnIndex(ApplicationContract.NAME));
                int folderId = cursor.getInt(cursor.getColumnIndex(ApplicationContract.FOLDER_ID));
                cursor.close();
                result.add(new ApplicationDetails(id, folderId, packName, name));
            }
        } catch (Exception ignored) {
            //TODO: disable log
            //ignored.printStackTrace();
        } finally {
            cursor.close();
        }
        return result;
    }

    public FolderDetails getFolderForId (int id) {
        SQLiteDatabase sqLiteDatabase = getReadableDatabase();
        Cursor cursor = sqLiteDatabase.query(FolderContract.TABLE_NAME, null,
                FolderContract._ID + " LIKE ?", new String[]{String.valueOf(id)},
                null, null, null);
        try {
            if (cursor.moveToFirst()) {
                int color = cursor.getInt(cursor.getColumnIndex(FolderContract.COLOR)),
                    floating = cursor.getInt(cursor.getColumnIndex(FolderContract.FLOATING)),
                        expanded = cursor.getInt(cursor.getColumnIndex(FolderContract.EXPANDED));
                String name = cursor.getString(cursor.getColumnIndex(FolderContract.NAME));
                cursor.close();
                return new FolderDetails(id, name, color, floating > 0, expanded > 0);
            }
        } catch (Exception ignored) {
            //TODO: disable log
            //ignored.printStackTrace();
        } finally {
            cursor.close();
        }
        return null;
    }

    public ArrayList<ApplicationDetails> getAppsForFolder(int folderId, String namesQuery) {
        SQLiteDatabase sqLiteDatabase = getReadableDatabase();
        ArrayList<ApplicationDetails> result = new ArrayList<>();
        Cursor cursor = sqLiteDatabase.query(ApplicationContract.TABLE_NAME, null,
                null, null, null, null, null);
        try {
            while (cursor.moveToNext()) {
                String packName = cursor.getString(cursor.getColumnIndex(ApplicationContract.PACKAGE_NAME)),
                        name = cursor.getString(cursor.getColumnIndex(ApplicationContract.NAME));
                int id = cursor.getInt(cursor.getColumnIndex(ApplicationContract._ID)),
                        foldId = cursor.getInt(cursor.getColumnIndex(ApplicationContract.FOLDER_ID));
                if (foldId == folderId && (namesQuery == null || namesQuery.isEmpty()
                        || name.toLowerCase().contains(namesQuery)))
                    result.add(new ApplicationDetails(id, folderId, packName, name));
            }
        } catch (Exception ignored) {
            //TODO: disable log
            //ignored.printStackTrace();
        } finally {
            cursor.close();
        }
        return result;
    }

    public ArrayList<FolderDetails> getAllFolders(String namesQuery) {
        SQLiteDatabase sqLiteDatabase = getReadableDatabase();
        ArrayList<FolderDetails> result = new ArrayList<>();
        Cursor cursor = sqLiteDatabase.query(FolderContract.TABLE_NAME, null,
                null, null, null, null, null);
        try {
            while (cursor.moveToNext()) {
                String name = cursor.getString(cursor.getColumnIndex(FolderContract.NAME));
                int id = cursor.getInt(cursor.getColumnIndex(FolderContract._ID)),
                        color = cursor.getInt(cursor.getColumnIndex(FolderContract.COLOR)),
                        floating = cursor.getInt(cursor.getColumnIndex(FolderContract.FLOATING)),
                        expanded = cursor.getInt(cursor.getColumnIndex(FolderContract.EXPANDED));
                if (namesQuery == null || namesQuery.isEmpty()
                        || name.toLowerCase().contains(namesQuery))
                    result.add(new FolderDetails(id, name, color, floating > 0, expanded > 0));
            }
        } catch (Exception ignored) {
            //TODO: disable log
            //ignored.printStackTrace();
        } finally {
            cursor.close();
        }
        return result;
    }

    public int deleteApplicationData(ApplicationDetails applicationDetails, FolderDetails folderDetails,
                                     Context context) {
        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        int count = sqLiteDatabase.delete(ApplicationContract.TABLE_NAME,
                ApplicationContract._ID + " LIKE ?",
                new String[]{String.valueOf(applicationDetails.getId())});
        if (count > 0) {
            Intent intent = new Intent(Utils.FOLDER_CHANGED_ACTION);
            intent.putExtra(FOLDER_DETAILS_KEY, folderDetails);
            LocalBroadcastManager.getInstance(context)
                    .sendBroadcast(intent);
        }
        return count;
    }

    public int deleteApplicationData(String packName) {
        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        return sqLiteDatabase.delete(ApplicationContract.TABLE_NAME,
                ApplicationContract.PACKAGE_NAME + " LIKE ?",
                new String[]{packName});
    }

    public int deleteAppsForFolder (int folderId) {
        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        return sqLiteDatabase.delete(ApplicationContract.TABLE_NAME,
                ApplicationContract.FOLDER_ID + " LIKE ?",
                new String[]{String.valueOf(folderId)});
    }

    public int deleteFolderData (int id, Context context) {
        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        deleteAppsForFolder(id);
        int count = sqLiteDatabase.delete(FolderContract.TABLE_NAME,
                ApplicationContract._ID + " LIKE ?",
                new String[]{String.valueOf(id)});
        if (count > 0) {
            Intent intent = new Intent(Utils.FOLDER_REMOVE_ACTION);
            intent.putExtra(FOLDER_DETAILS_KEY, id);
            LocalBroadcastManager.getInstance(context)
                    .sendBroadcast(intent);
        }
        return count;
    }
}