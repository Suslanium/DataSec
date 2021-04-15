package com.suslanium.encryptor;

import android.os.Parcel;
import android.os.Parcelable;

public class YaDiCredentials extends com.yandex.disk.rest.Credentials implements Parcelable {

    public YaDiCredentials(String user, String token) {
        super(user, token);
    }

    public static final Parcelable.Creator<YaDiCredentials> CREATOR = new Parcelable.Creator<YaDiCredentials>() {

        public YaDiCredentials createFromParcel(Parcel parcel) {
            return new YaDiCredentials(parcel.readString(), parcel.readString());
        }

        public YaDiCredentials[] newArray(int size) {
            return new YaDiCredentials[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(user);
        parcel.writeString(token);
    }
}
