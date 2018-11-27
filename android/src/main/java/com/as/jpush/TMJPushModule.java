
package com.as.jpush;

import android.app.AppOpsManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import android.content.BroadcastReceiver;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;


import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import cn.jpush.android.api.JPushInterface;


public class TMJPushModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;
    private static String TAG = "TMJPushModule";
    private Context mContext;

    protected static final String DidReceiveMessage = "DidReceiveMessage";
    protected static final String DidReceiveNotification = "DidReceiveNotification";
    protected static final String DidOpenMessage = "DidOpenMessage";

    private static SparseArray<Callback> sCacheMap;
    private static Callback mGetRidCallback;
    private static ReactApplicationContext mRAC;
    private static CountDownLatch mLatch;

    public TMJPushModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return TAG;
    }

    @Override
    public boolean canOverrideExistingModule() {
        return true;
    }

    @Override
    public void initialize() {
        super.initialize();
        mLatch.countDown();
        sCacheMap = new SparseArray<>();
    }

    @Override
    public void onCatalystInstanceDestroy() {
        super.onCatalystInstanceDestroy();
        Logger.i(TAG, "onCatalystInstanceDestroy");
        if (null != sCacheMap) {
            sCacheMap.clear();
        }
        mGetRidCallback = null;
        mRAC = null;
    }

    @ReactMethod
    public void initPush() {
        mContext = getCurrentActivity();
        JPushInterface.init(getReactApplicationContext());
        Logger.toast(mContext, "Init push success");
        Logger.i(TAG, "init Success!");
    }


    @ReactMethod
    public void stopPush() {
        mContext = getCurrentActivity();
        JPushInterface.stopPush(getReactApplicationContext());
        Logger.i(TAG, "Stop push");
        Logger.toast(mContext, "Stop push success");
    }


    @ReactMethod
    public void hasPermission(Callback callback) {
        callback.invoke(hasPermission("OP_POST_NOTIFICATION"));
    }


    @ReactMethod
    public void clearAllNotifications() {
        mContext = getCurrentActivity();
        assert mContext != null;
        JPushInterface.clearAllNotifications(mContext);
    }

    @ReactMethod
    public void setAlias(String alias, Callback callback) {
        int sequence = getSequence();
        Logger.i(TAG, "Set alias, sequence: " + sequence);
        sCacheMap.put(sequence, callback);
        JPushInterface.setAlias(getReactApplicationContext(), sequence, alias);
    }

    /**
     * JPush v3.0.7 Add this API
     * See document https://docs.jiguang.cn/jpush/client/Android/android_api/#aliastag for detail
     * Delete alias
     *
     * @param callback callback
     */
    @ReactMethod
    public void deleteAlias(Callback callback) {
        int sequence = getSequence();
        Logger.i(TAG, "Delete alias, sequence: " + sequence);
        sCacheMap.put(sequence, callback);
        JPushInterface.deleteAlias(getReactApplicationContext(), sequence);
    }


    @ReactMethod
    public void getInfo(Callback successCallback) {
        WritableMap map = Arguments.createMap();
        String appKey = "AppKey:" + ExampleUtil.getAppKey(getReactApplicationContext());
        map.putString("myAppKey", appKey);
        String imei = "IMEI: " + ExampleUtil.getImei(getReactApplicationContext(), "");
        map.putString("myImei", imei);
        String packageName = "PackageName: " + getReactApplicationContext().getPackageName();
        map.putString("myPackageName", packageName);
        String deviceId = "DeviceId: " + ExampleUtil.getDeviceId(getReactApplicationContext());
        map.putString("myDeviceId", deviceId);
        String version = "Version: " + ExampleUtil.GetVersion(getReactApplicationContext());
        map.putString("myVersion", version);
        successCallback.invoke(map);
    }


    /**
     * JPush v3.0.7 Add this API
     * See document https://docs.jiguang.cn/jpush/client/Android/android_api/#aliastag for detail
     * Set tags
     *
     * @param tags     tags array
     * @param callback callback
     */
    @ReactMethod
    public void setTags(final ReadableArray tags, final Callback callback) {
        int sequence = getSequence();
        Logger.i(TAG, "sequence: " + sequence);
        sCacheMap.put(sequence, callback);
        Logger.i(TAG, "tag: " + tags.toString());
        Set<String> tagSet = getSet(tags);
        JPushInterface.setTags(getReactApplicationContext(), sequence, tagSet);
    }

    private int getSequence() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMddHHmmss");
        String date = sdf.format(new Date());
        return Integer.valueOf(date);
    }

    private Set<String> getSet(ReadableArray strArray) {
        Set<String> tagSet = new LinkedHashSet<>();
        for (int i = 0; i < strArray.size(); i++) {
            if (!ExampleUtil.isValidTagAndAlias(strArray.getString(i))) {
                Logger.toast(getReactApplicationContext(), "Invalid tag !");
            }
            tagSet.add(strArray.getString(i));
        }
        return tagSet;
    }

    /**
     * 获取设备id/registrationId
     *
     * @param callback
     */
    @ReactMethod
    public void getDeviceToken(Callback callback) {
        try {
            mContext = getCurrentActivity();
            assert mContext != null;
            String id = JPushInterface.getRegistrationID(mContext);
            callback.invoke(id);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Get registration id, different from JPushModule.addGetRegistrationIdListener, this
     * method has no calling limits.
     *
     * @param callback callback with registrationId
     */
    @ReactMethod
    public void getRegistrationID(Callback callback) {
        try {
            String id = JPushInterface.getRegistrationID(getReactApplicationContext());
            if (id != null) {
                callback.invoke(id);
            } else {
                mGetRidCallback = callback;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 接收自定义消息,通知,通知点击事件等事件的广播
     * 文档链接:http://docs.jiguang.cn/client/android_api/
     */
    public static class JPushReceiver extends BroadcastReceiver {

        public JPushReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent data) {
            Bundle bundle = data.getExtras();
            if (JPushInterface.ACTION_MESSAGE_RECEIVED.equals(data.getAction())) {
                String message = data.getStringExtra(JPushInterface.EXTRA_MESSAGE);
                assert bundle != null;
                String extras = bundle.getString(JPushInterface.EXTRA_EXTRA);
                WritableMap map = Arguments.createMap();
                map.putString("message", message);
                map.putString("extras", extras);
                Log.i(TAG, "收到自定义消息: " + message);
                sendEvent(DidReceiveMessage, map, null);
            } else if (JPushInterface.ACTION_NOTIFICATION_RECEIVED.equals(data.getAction())) {
                try {
                    DeviceUtil.applyCount(context);
                    // 通知内容
                    assert bundle != null;
                    String alertContent = bundle.getString(JPushInterface.EXTRA_ALERT);
                    // extra 字段的 json 字符串
                    String extras = bundle.getString(JPushInterface.EXTRA_EXTRA);
                    Log.i(TAG, "收到推送下来的通知: " + alertContent);

                    WritableMap map = Arguments.createMap();
                    map.putString("alertContent", alertContent);
                    map.putString("extras", extras);
                    sendEvent(DidReceiveNotification, map, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // 这里点击通知跳转到指定的界面可以定制化一下
            } else if (JPushInterface.ACTION_NOTIFICATION_OPENED.equals(data.getAction())) {
                try {
                    Log.d(TAG, "用户点击打开了通知");
                    // 通知内容
                    assert bundle != null;
                    String alertContent = bundle.getString(JPushInterface.EXTRA_ALERT);
                    // extra 字段的 json 字符串
                    String extras = bundle.getString(JPushInterface.EXTRA_EXTRA);
                    WritableMap map = Arguments.createMap();
                    map.putString("alertContent", alertContent);
                    map.putString("extras", extras);
                    map.putString("jumpTo", "second");
                    // judge if application is running in background, opening initial Activity.
                    // You can change here to open appointed Activity. All you need to do is create
                    // the appointed Activity, and use JS render the appointed Activity.
                    // Please reference examples' SecondActivity for detail,
                    // and JS files are in folder: example/react-native-android
                    Intent intent = new Intent();
                    String package_path = getAppMetaData(context, "package_path");
                    intent.setClassName(context.getPackageName(), package_path + ".MainActivity");
                    intent.putExtras(bundle);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    context.startActivity(intent);

                    sendEvent(DidOpenMessage, map, null);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i(TAG, "Shouldn't access here");
                }
                // 应用注册完成后会发送广播，在 JS 中 JPushModule.addGetRegistrationIdListener 接口可以第一时间得到 registrationId
                // After JPush finished registering, will send this broadcast, use JPushModule.addGetRegistrationIdListener
                // to get registrationId in the first instance.
            } else if (JPushInterface.ACTION_REGISTRATION_ID.equals(data.getAction())) {
                String registrationId = data.getExtras().getString(JPushInterface.EXTRA_REGISTRATION_ID);
                Log.d(TAG, "注册成功, registrationId: " + registrationId);
                try {
                    sendEvent("getRegistrationId", null, registrationId);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
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

    private static void sendEvent(String methodName, WritableMap map, String data) {
        try {
            mLatch.await();
            if (mRAC != null) {
                if (map != null) {
                    mRAC.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                            .emit(methodName, map);
                } else {
                    mRAC.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                            .emit(methodName, data);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean hasPermission(String appOpsServiceId) {

        Context context = getCurrentActivity().getApplicationContext();
        if (Build.VERSION.SDK_INT >= 24) {
            NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(
                    Context.NOTIFICATION_SERVICE);
            return mNotificationManager.areNotificationsEnabled();
        }else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            AppOpsManager mAppOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            ApplicationInfo appInfo = context.getApplicationInfo();

            String pkg = context.getPackageName();
            int uid = appInfo.uid;
            Class appOpsClazz;

            try {
                appOpsClazz = Class.forName(AppOpsManager.class.getName());
                Method checkOpNoThrowMethod = appOpsClazz.getMethod("checkOpNoThrow", Integer.TYPE, Integer.TYPE,
                        String.class);
                Field opValue = appOpsClazz.getDeclaredField(appOpsServiceId);
                int value = opValue.getInt(Integer.class);
                Object result = checkOpNoThrowMethod.invoke(mAppOps, value, uid, pkg);
                return Integer.parseInt(result.toString()) == AppOpsManager.MODE_ALLOWED;
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        return false;
    }
}