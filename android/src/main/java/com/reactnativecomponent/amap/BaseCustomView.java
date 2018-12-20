package com.reactnativecomponent.amap;


import android.content.Context;
import android.widget.LinearLayout;

import java.util.HashMap;

public abstract class BaseCustomView extends LinearLayout {
    public BaseCustomView(Context context) {
        super(context);
    }
    public abstract HashMap<String,Float> getOffset();
}
