package com.sagittarius.nfc;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

public class Tools {

    private static float getAPIVersion() {

        Float f = null;
        try {
            StringBuilder strBuild = new StringBuilder();
            double release=Double.parseDouble(Build.VERSION.RELEASE.replaceAll("(\\d+[.]\\d+)(.*)","$1"));
            strBuild.append(release);
            f = new Float(strBuild.toString());
        } catch (NumberFormatException e) {
            Log.e("", "TOOLS - get api version error" + e.getMessage());
        }
        return f;
    }

    public static void systemBarLolipop(Activity act){
        if (getAPIVersion() >= 5.0) {
            Window window = act.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(act.getResources().getColor(R.color.colorPrimaryDark));
        }
    }

    public static void changeStatusBarColor(Activity act) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = act.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }
}
