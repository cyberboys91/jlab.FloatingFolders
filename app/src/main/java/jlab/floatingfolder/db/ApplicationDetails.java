package jlab.floatingfolder.db;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import jlab.floatingfolder.Utils;

/*
 * Created by Javier on 24/04/2017.
 */
public class ApplicationDetails implements Parcelable {

    private int id, folderId;
    private StringBuilder mName, mPackName;

    public ApplicationDetails(int id, int folderId, String packName, String name) {
        this.id = id;
        this.folderId = folderId;
        this.mPackName = new StringBuilder(packName);
        this.mName = new StringBuilder(name);
    }

    public ApplicationDetails(Parcel in) {
        this.id = in.readInt();
        this.folderId = in.readInt();
        this.mPackName = new StringBuilder(in.readString());
        this.mName = new StringBuilder(in.readString());
    }

    public static final Creator<ApplicationDetails> CREATOR = new Creator<ApplicationDetails>() {
        @Override
        public ApplicationDetails createFromParcel(Parcel in) {
            return new ApplicationDetails(in);
        }

        @Override
        public ApplicationDetails[] newArray(int size) {
            return new ApplicationDetails[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(id);
        parcel.writeInt(folderId);
        parcel.writeString(getPackName());
        parcel.writeString(getName());
    }

    public String getPackName() {
        return mPackName.toString();
    }

    public Bitmap getIcon(Context context) {
        return Utils.getIconForApp(mPackName.toString(), context);
    }

    public String getName() {
        return mName.toString();
    }

    public int getId() {
        return id;
    }

    public int getFolderId() {
        return folderId;
    }

    public void setName(String mName) {
        this.mName = new StringBuilder(mName);
    }

    public void setPackName(String mPackName) {
        this.mPackName = new StringBuilder(mPackName);
    }

    public void setFolderId(int folderId) {
        this.folderId = folderId;
    }

    public void setId(int id) {
        this.id = id;
    }

    public ContentValues toContentValues() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(ApplicationContract.FOLDER_ID, folderId);
        contentValues.put(ApplicationContract.PACKAGE_NAME, getPackName());
        contentValues.put(ApplicationContract.NAME, getName());
        return contentValues;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApplicationDetails that = (ApplicationDetails) o;
        return mPackName.toString().equals(that.mPackName.toString());
    }
}
