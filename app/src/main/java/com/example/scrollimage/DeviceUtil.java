package com.example.scrollimage;

import android.content.Context;

public class DeviceUtil {
    public static int dip2Px(Context context,float dpvalue){
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpvalue * scale +0.5f);
    }
}
