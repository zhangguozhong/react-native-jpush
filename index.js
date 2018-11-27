import {
    NativeModules, DeviceEventEmitter,
    NativeAppEventEmitter,
    Platform, AppState
} from 'react-native';
const { TMJPushModule } = NativeModules;

export default class JPushModule {
    static initPush() {
        if (Platform.OS === 'android') {
            TMJPushModule.initPush();
        }
    }
    static setAlias(alias:String) {
        TMJPushModule.setAlias(alias);
    }
    static setTags(tags) {
        TMJPushModule.setTags(tags);
    }
    static getRegistrationID(handler) {
        TMJPushModule.getRegistrationID(handler);
    }

    static getDeviceToken(handler) {
        TMJPushModule.getDeviceToken(handler);
    }
    /**
     * Android 关闭推送
     */
    static stopPush() {
        TMJPushModule.stopPush();
    }
    /**
     * Android 开启推送
     */
    static resumePush() {
        TMJPushModule.resumePush();
    }

    static hasPermission(handler) {
        TMJPushModule.hasPermission(handler);
    }

    static setDebugMode() {
        TMJPushModule.setDebugMode();
    }
    static setLogOFF() {
        TMJPushModule.setLogOFF();
    }
    static pushNotificationSetPage() {
        TMJPushModule.pushNotificationSetPage();
    }

    static clearAllNotifications() {
        TMJPushModule.clearAllNotifications();
    }

    static didReceiveMessage(handler) {
        this.addEventListener(TMJPushModule.DidReceiveMessage, message => {
            //处于后台时，拦截收到的消息
            if(AppState.currentState === 'background') {
                return;
            }
            handler(message);
        });
    }
    static didReceiveNotification(handler) {
        if(Platform.OS === 'android') {
            this.addEventListener(TMJPushModule.DidReceiveNotification, message => {
                //处于后台时，拦截收到的通知
                if(AppState.currentState === 'background') {
                    return;
                }
                handler(message);
            });
        }
    }

    static didOpenMessage(handler) {
        this.addEventListener(TMJPushModule.DidOpenMessage, handler);
    }

    static addEventListener(eventName, handler) {
        if(Platform.OS === 'android') {
            return DeviceEventEmitter.addListener(eventName, (event) => {
                handler(event);
            });
        }
        else {
            return NativeAppEventEmitter.addListener(
                eventName, (userInfo) => {
                    handler(userInfo);
                });
        }
    }
    static removeListener(eventName) {
        if(Platform.OS === 'android') {
            return DeviceEventEmitter.removeAllListeners(eventName);
        }
        else {
            return NativeAppEventEmitter.removeAllListeners(eventName);
        }
    }
    static removeAllListeners() {
        this.removeListener(TMJPushModule.DidOpenMessage);
        this.removeListener(TMJPushModule.DidReceiveMessage);
    }
}
