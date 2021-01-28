package com.tenny.tickview.widget;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;

/**
 * Created by TennyQ on 1/28/21
 */
public class SaveSateDemo extends View.BaseSavedState{

    private String textContent;
    private boolean showTick;
    private boolean showLoading;


    public SaveSateDemo(Parcelable superState) {
        super(superState);
    }

    private SaveSateDemo(Parcel source) {
        super(source);
        textContent = source.readString();
    }
}
