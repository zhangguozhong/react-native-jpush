import {
    NativeModules, NativeEventEmitter,
    NativeAppEventEmitter,
    Platform, AppState
} from 'react-native';
const { TMJpushModule } = NativeModules;

export default class JPushModule {
    static initPush() {
        if (Platform.OS === 'android') {
            JPushModule.initPush();
        }
    }
    static setAlias(alias:String) {
        JPushModule.setAlias(alias);
    }
    static setTags(tags) {
        JPushModule.setTags(tags);
    }
    static getRegistrationID(handler) {
        JPushModule.getRegistrationID(handler);
    }

    static getDeviceToken(handler) {
        JPushModule.getDeviceToken(handler);
    }
    /**
     * Android 关闭推送
     */
    static stopPush() {
        JPushModule.stopPush();
    }
    static hasPermission(handler) {
        JPushModule.hasPermission(handler);
    }

    static setDebugMode() {
        JPushModule.setDebugMode();
    }
    static setLogOFF() {
        JPushModule.setLogOFF();
    }

    static didReceiveMessage(handler) {
        this.addEventListener(JPushModule.DidReceiveMessage, message => {
            //处于后台时，拦截收到的消息
            if(AppState.currentState === 'background') {
                return;
            }
            handler(message);
        });
    }
    static didReceiveNotification(handler) {
        if(Platform.OS === 'android') {
            this.addEventListener(JPushModule.DidReceiveNotification, message => {
                //处于后台时，拦截收到的通知
                if(AppState.currentState === 'background') {
                    return;
                }
                handler(message);
            });
        }
    }

    static didOpenMessage(handler) {
        this.addEventListener(JPushModule.DidOpenMessage, handler);
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
        this.removeListener(JPushModule.DidOpenMessage);
        this.removeListener(JPushModule.DidReceiveMessage);
    }
}
