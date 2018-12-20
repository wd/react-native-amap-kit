package com.reactnativecomponent.amap;

import android.view.View;

import com.amap.api.maps.model.Marker;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableMap;

import java.lang.reflect.Constructor;

/**
 * Created by wd on 14/09/2017.
 */

public class RCTInfoWindow {
    AMAPInfoWindowAdapter infowindow;

    public RCTInfoWindow(ReactContext context, String className) {
        try {
            Class<?> c = Class.forName(className);
            Constructor cons = c.getConstructor(ReactContext.class);
            infowindow = (AMAPInfoWindowAdapter) cons.newInstance(context);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public View getInfoWindow(Marker marker, ReadableMap config) {
        try {
            return infowindow.getInfoWindow(marker, config);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void removeInfoWindow(String key) {
        try {
            infowindow.removeInfoWindow(key);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
