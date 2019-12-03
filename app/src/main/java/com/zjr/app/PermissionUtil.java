package com.zjr.app;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PermissionUtil {
    //Manifest.permission.READ_PHONE_STATE

    public static boolean checkPermissions(Activity context, String permission, int requestCode) {
//        if (Build.VERSION.SDK_INT < 23) {
//            return true;
//        }
        int status = ActivityCompat.checkSelfPermission(context,permission);
        if (status != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(context, new String[]{permission},requestCode);
            return false;
        }
        return true;
    }

//    public static boolean checkPermissions(Activity context, String[] permission, int requestCode) {
////        if (Build.VERSION.SDK_INT < 23) {
////            return true;
////        }
//        int status = ActivityCompat.checkSelfPermission(context,permission);
//        if (status != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(context, new String[]{permission},requestCode);
//            return false;
//        }
//        return true;
//    }

    public static boolean checkPermissions(Activity context, String[] permissions, int requestCode) {
//        if (Build.VERSION.SDK_INT < 23) {
//            return true;
//        }
        ArrayList<String> request = new ArrayList<>();
        for (int i = 0; i < permissions.length; i++) {
            int status = ActivityCompat.checkSelfPermission(context,permissions[i]);
            if (status != PackageManager.PERMISSION_GRANTED) {
                request.add(permissions[i]);
            }
        }
        if (request.size()>0) {
            String[] requestPermissions = new String[request.size()];
            request.toArray(requestPermissions);
            ActivityCompat.requestPermissions(context, requestPermissions,requestCode);
            return false;
        }
        return true;
    }

    public static void handlePermissionsResult(int requestCode, String[] permissions, int[] grantResults, ResultCallback callback) {

        for (int i = 0; i < grantResults.length; i++) {
            if (grantResults[i] == PackageManager.PERMISSION_GRANTED){
                callback.onGranted(requestCode,Arrays.asList(permissions));
            }else{
                //用户不同意，向用户展示该权限作用
               // ActivityCompat.shouldShowRequestPermissionRationale(@NonNull Activity activity, @NonNull String permission)
            }
        }
    }

    public interface ResultCallback{
        void onGranted(int requestCode, List<String> list);
        void onDenied(int requestCode, List<String> list);
    }

}
