package jlab.floatingfolder.db;

import android.content.ContentValues;
import android.os.Parcel;
import android.os.Parcelable;

/*
 * Created by Javier on 24/04/2017.
 */
public class FolderDetails implements Parcelable {

    private int id, color;
    boolean floating, expanded;
    private String name;

    public FolderDetails(int id, String name, int color, boolean floating, boolean expanded) {
        this.id = id;
        this.color = color;
        this.name = name;
        this.floating = floating;
        this.expanded = expanded;
    }

    public FolderDetails(Parcel in) {
        this.id = in.readInt();
        this.color = in.readInt();
        this.name = in.readString();
        this.floating = in.readInt() > 0;
        this.expanded = in.readInt() > 0;
    }

    public static final Creator<FolderDetails> CREATOR = new Creator<FolderDetails>() {
        @Override
        public FolderDetails createFromParcel(Parcel in) {
            return new FolderDetails(in);
        }

        @Override
        public FolderDetails[] newArray(int size) {
            return new FolderDetails[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(id);
        parcel.writeInt(getColor());
        parcel.writeString(getName());
        parcel.writeInt(isFloating() ? 1 : 0);
        parcel.writeInt(isExpanded() ? 1 : 0);
    }

    public boolean isFloating() {
        return floating;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public int getColor() {
        return color;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ContentValues toContentValues() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(FolderContract.COLOR, getColor());
        contentValues.put(FolderContract.NAME, getName());
        contentValues.put(FolderContract.FLOATING, isFloating() ? 1 : 0);
        contentValues.put(FolderContract.EXPANDED, isExpanded() ? 1 : 0);
        return contentValues;
    }

    public void setColor(int newColor) {
        color = newColor;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public void setFloating(boolean floating) {
        this.floating = floating;
    }
}
