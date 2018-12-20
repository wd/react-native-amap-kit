package com.reactnativecomponent.amap;

import android.view.View;

import com.amap.api.maps.model.Marker;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableMap;

/**
 * Created by wd on 13/09/2017.
 */

public abstract class AMAPInfoWindowAdapter {
    public final ReactContext mContext;

    public AMAPInfoWindowAdapter(ReactContext context) {
        this.mContext = context;
    }

    abstract public View getInfoWindow(Marker marker, ReadableMap config);
    abstract public void removeInfoWindow(String key);
}
