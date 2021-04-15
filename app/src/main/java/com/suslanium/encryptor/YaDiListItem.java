package com.suslanium.encryptor;

import android.os.Parcel;
import android.os.Parcelable;

import com.yandex.disk.rest.json.Resource;

public class YaDiListItem implements Parcelable {

    private String name, path, etag, contentType, publicUrl, mediaType;
    private boolean dir;
    private long contentLength, lastUpdated;

    public YaDiListItem(Resource resource) {
        this.name = resource.getName();
        this.path = resource.getPath() != null ? resource.getPath().getPath() : null;  // Must throw an exception in real life code
        this.etag = resource.getMd5();
        this.contentType = resource.getMimeType();
        this.publicUrl = resource.getPublicUrl();
        this.mediaType = resource.getMediaType();
        this.dir = resource.isDir();
        this.contentLength = resource.getSize();
        this.lastUpdated = resource.getModified() != null ? resource.getModified().getTime() : 0;
    }

    private YaDiListItem(String path, String name, long contentLength, long lastUpdated, boolean dir,
                     String etag, String contentType, String publicUrl, String mediaType) {
        this.path = path;
        this.name = name;
        this.contentLength = contentLength;
        this.lastUpdated = lastUpdated;
        this.dir = dir;
        this.etag = etag;
        this.contentType = contentType;
        this.publicUrl = publicUrl;
        this.mediaType = mediaType;
    }

    @Override
    public String toString() {
        return "ListItem{" +
                "name='" + name + '\'' +
                ", path='" + path + '\'' +
                ", etag='" + etag + '\'' +
                ", contentType='" + contentType + '\'' +
                ", publicUrl='" + publicUrl + '\'' +
                ", mediaType='" + mediaType + '\'' +
                ", dir=" + dir +
                ", contentLength=" + contentLength +
                ", lastUpdated=" + lastUpdated +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(path);
        parcel.writeString(name);
        parcel.writeLong(contentLength);
        parcel.writeLong(lastUpdated);
        parcel.writeByte((byte) (dir ? 1 : 0));
        parcel.writeString(etag);
        parcel.writeString(contentType);
        parcel.writeString(publicUrl);
        parcel.writeString(mediaType);
    }

    public static final Parcelable.Creator<YaDiListItem> CREATOR = new Parcelable.Creator<YaDiListItem>() {

        public YaDiListItem createFromParcel(Parcel parcel) {
            return new YaDiListItem(parcel.readString(), parcel.readString(), parcel.readLong(),
                    parcel.readLong(), parcel.readByte() > 0, parcel.readString(),
                    parcel.readString(), parcel.readString(), parcel.readString());
        }

        public YaDiListItem[] newArray(int size) {
            return new YaDiListItem[size];
        }
    };

    public String getPath() {
        return path;
    }

    public String getName() {
        return name;
    }

    public long getContentLength() {
        return contentLength;
    }

    /**
     * @return time in milliseconds
     */
    public long getLastUpdated() {
        return lastUpdated;
    }

    public boolean isDir() {
        return dir;
    }

    public String getContentType() {
        return contentType;
    }

    public String getEtag() {
        return etag;
    }

    public String getPublicUrl() {
        return publicUrl;
    }

    public String getMediaType() {
        return mediaType;
    }

}
