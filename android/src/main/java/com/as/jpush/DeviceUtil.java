package com.as.jpush;


import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.text.TextUtils;

import java.util.List;

import me.leolin.shortcutbadger.ShortcutBadger;

public class DeviceUtil {

    private static String mCurrentLauncherName;
    protected static final String BadgeCountKey = "badgeCount";

    public static boolean isXiaoMi(Context context) {
        if (TextUtils.isEmpty(mCurrentLauncherName)) {
            mCurrentLauncherName = getCurrentLaunchname(context);
        }
        if (mCurrentLauncherName.contains("Xiaomi")) {
            return true;
        }
        if (mCurrentLauncherName.contains("miui")) {
            return true;
        }
        return false;
    }

    public static String getCurrentLaunchname(Context context) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        ResolveInfo resolveInfo = context.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);

        if (resolveInfo == null || resolveInfo.activityInfo.name.toLowerCase().contains("resolver")) {
            return "";
        }

        return resolveInfo.activityInfo.packageName;
    }

    /**
     * 获取application中指定的meta-data
     *
     * @return 如果没有获取成功(没有对应值或者异常)，则返回值为空
     */
    public static String getAppMetaData(Context ctx, String key) {
        if (ctx == null || TextUtils.isEmpty(key)) {
            return null;
        }
        String resultData = null;
        try {
            PackageManager packageManager = ctx.getPackageManager();
            if (packageManager != null) {
                ApplicationInfo applicationInfo = packageManager.getApplicationInfo(ctx.getPackageName(), PackageManager.GET_META_DATA);
                if (applicationInfo != null) {
                    if (applicationInfo.metaData != null) {
                        resultData = applicationInfo.metaData.getString(key);
                    } else {
                        return ctx.getPackageName();
                    }
                }

            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return resultData;
    }

    /**
     * 设置角标
     *
     * @param context
     */
    public static void applyCount(Context context) {
        int badgeCount = SharePreferencesUtil.getInt(context, BadgeCountKey, 0);
        //极光会自动在通知发送一个通知，如果App不在最前面，角标会自动+1，这就是这时候就是等于小米可以跟极光已经配合做好角标了，不需要我们做什么，如果这时候我们再手动更新角标，则用户会收到两条通知，所以在发送之前需要判断一下是否小米手机
        if (!isXiaoMi(context) && !isForeground(context)) {
            ShortcutBadger.applyCount(context, ++badgeCount); //for 1.1.4+
        }
        SharePreferencesUtil.saveInt(context, BadgeCountKey, badgeCount);
    }

    /**
     * 删除角标
     */
    public static void removeCount(Context context) {
        SharePreferencesUtil.saveInt(context, BadgeCountKey, 0);
        ShortcutBadger.removeCount(context); //for 1.1.4+
    }

    /**
     * 判断当前应用是否处于前台
     *
     * @param context
     * @return
     */
    private static boolean isForeground(Context context) {
        if (context != null) {
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            assert activityManager != null;
            List<ActivityManager.RunningAppProcessInfo> processes = activityManager.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo processInfo : processes) {
                if (processInfo.processName.equals(context.getPackageName())) {
                    if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
