#import "TMJPushModule.h"
#import "JPUSHService.h"
#import "RCTEventDispatcher.h"

#ifdef NSFoundationVersionNumber_iOS_9_x_Max
#import <UserNotifications/UserNotifications.h>
#endif

@interface TMJPushModule ()
@property (nonatomic, copy) NSString *deviceToken;
@property (nonatomic,strong) NSDateFormatter *dateFormatter;
@end

static TMJPushModule *_instance = nil;
static NSString *const DidReceiveMessage = @"DidReceiveMessage";
static NSString *const DidOpenMessage = @"DidOpenMessage";

@implementation TMJPushModule
RCT_EXPORT_MODULE();
@synthesize bridge = _bridge;

+ (instancetype)sharedInstance {
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        if(!_instance) {
            _instance = [[self alloc] init];
        }
    });
    return _instance;
}

+ (instancetype)allocWithZone:(struct _NSZone *)zone {
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        if(!_instance) {
            _instance = [super allocWithZone:zone];
        }
    });
    return _instance;
}

+ (dispatch_queue_t)sharedMethodQueue {
    static dispatch_queue_t methodQueue;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        methodQueue = dispatch_queue_create("com.taimei.react-native-jpush", DISPATCH_QUEUE_SERIAL);
    });
    return methodQueue;
}

- (dispatch_queue_t)methodQueue {
    return [TMJPushModule sharedMethodQueue];
}

+(BOOL)requiresMainQueueSetup {
    return YES;
}

- (NSDictionary<NSString *, id> *)constantsToExport {
    return @{
             DidReceiveMessage: DidReceiveMessage,
             DidOpenMessage: DidOpenMessage
             };
}
- (void)didReceiveRemoteNotification:(NSDictionary *)userInfo {
    [self.bridge.eventDispatcher sendAppEventWithName:DidReceiveMessage body:userInfo];
}
- (void)didOpenRemoteNotification:(NSDictionary *)userInfo {
    [self.bridge.eventDispatcher sendAppEventWithName:DidOpenMessage body:userInfo];
}


RCT_EXPORT_METHOD(getRegistrationID:(RCTResponseSenderBlock)callback) {
    NSString *registrationID = [JPUSHService registrationID];
    if (!registrationID) {
        registrationID = @"";
    }
    callback(@[registrationID]);
}
RCT_EXPORT_METHOD(getDeviceToken:(RCTResponseSenderBlock)callback) {
    NSString *deviceToken = self.deviceToken;
    if(!deviceToken) {
        deviceToken = @"";
    }
    callback(@[deviceToken]);
}
RCT_EXPORT_METHOD(setAlias:(NSString *)alias {
    [JPUSHService setAlias:alias completion:nil seq:[self getSequence]];
})
RCT_EXPORT_METHOD(deleteAlias:(NSString *)alias {
    [JPUSHService deleteAlias:nil seq:[self getSequence]];
})
RCT_EXPORT_METHOD(setDebugMode) {
    [JPUSHService setDebugMode];
}
RCT_EXPORT_METHOD(setLogOFF) {
    [JPUSHService setLogOFF];
}


+ (void)registerWithlaunchOptions:(NSDictionary *)launchOptions appKey:(NSString *)appKey appChannel:(NSString *)appChannel withAppDelegate:(id)delegate
{
    if ([[UIDevice currentDevice].systemVersion floatValue] >= 10.0) {
#ifdef NSFoundationVersionNumber_iOS_9_x_Max
        JPUSHRegisterEntity *entity = [[JPUSHRegisterEntity alloc] init];
        if (@available(iOS 12.0, *)) {
            entity.types = UNAuthorizationOptionAlert|UNAuthorizationOptionBadge|UNAuthorizationOptionSound|UNAuthorizationOptionProvidesAppNotificationSettings;
        }else {
            entity.types = UNAuthorizationOptionAlert|UNAuthorizationOptionBadge|UNAuthorizationOptionSound;
        }
        [JPUSHService registerForRemoteNotificationConfig:entity delegate:delegate];
        
#endif
    }else if ([[UIDevice currentDevice].systemVersion floatValue] >= 8.0)
    {
        NSInteger types = UIUserNotificationTypeBadge|UIUserNotificationTypeSound|UIUserNotificationTypeAlert;
        [JPUSHService registerForRemoteNotificationTypes:types categories:nil];
    }
    
#ifdef DEBUG
    [JPUSHService setDebugMode];
    [JPUSHService setupWithOption:launchOptions appKey:appKey channel:appChannel apsForProduction:NO];
#else
    [JPUSHService setLogOFF];
    [JPUSHService setupWithOption:launchOptions appKey:appKey channel:appChannel apsForProduction:YES];
#endif
}

/*
 * 设置 tags 的方法
 */
RCT_EXPORT_METHOD(setTags:(NSArray *)tags) {
    NSSet *tagSet;
    if (tags) {
        tagSet = [NSSet setWithArray:tags];
    }
    [JPUSHService setTags:tagSet completion:nil seq:[self getSequence]];
}
RCT_EXPORT_METHOD(hasPermission:(RCTResponseSenderBlock)callback) {
    float systemVersion = [[UIDevice currentDevice].systemVersion floatValue];
    if (systemVersion >= 8.0) {
        UIUserNotificationSettings *settings = [[UIApplication sharedApplication] currentUserNotificationSettings];
        UIUserNotificationType type = settings.types;
        if (type == UIUserNotificationTypeNone) {
            callback(@[@(NO)]);
        } else {
            callback(@[@(YES)]);
        }
        
    } else if (systemVersion >= 10.0) {
        [[UNUserNotificationCenter currentNotificationCenter] getNotificationSettingsWithCompletionHandler:^(UNNotificationSettings * _Nonnull settings) {
            switch (settings.authorizationStatus)
            {
                case UNAuthorizationStatusDenied:
                case UNAuthorizationStatusNotDetermined:
                callback(@[@(NO)]);
                break;
                case UNAuthorizationStatusAuthorized:
                callback(@[@(YES)]);
                break;
                case UNAuthorizationStatusProvisional:
                callback(@[@(YES)]);
                break;
            }
        }];
    }
}
RCT_EXPORT_METHOD(pushNotificationSetPage){
    [[UIApplication sharedApplication] openURL:[NSURL URLWithString:UIApplicationOpenSettingsURLString]];
}
RCT_EXPORT_METHOD(clearAllNotifications) {
    if ([[UIDevice currentDevice].systemVersion floatValue] >= 10.0) {
        [UNUserNotificationCenter.currentNotificationCenter removeAllPendingNotificationRequests];
    } else {
        [[UIApplication sharedApplication] cancelAllLocalNotifications];
    }
}

+ (void)application:(UIApplication *)application didRegisterDeviceToken:(NSData *)deviceToken {
    [JPUSHService registerDeviceToken:deviceToken];
}

+ (void)application:(UIApplication *)application didReceiveRemoteNotification:(NSDictionary *)userInfo {
    //send event
    if ([UIApplication sharedApplication].applicationState == UIApplicationStateActive) {
        [[TMJPushModule sharedInstance] didReceiveRemoteNotification:userInfo];
    }
    else {
        [[TMJPushModule sharedInstance] didOpenRemoteNotification:userInfo];
    }
    [JPUSHService handleRemoteNotification:userInfo];
}

+ (void)didReceiveRemoteNotificationWhenFirstLaunchApp:(NSDictionary *)launchOptions {
    if(launchOptions && [launchOptions objectForKey:UIApplicationLaunchOptionsRemoteNotificationKey]){
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(0.5 * NSEC_PER_SEC)), [self sharedMethodQueue], ^{
            //判断当前模块是否正在加载，已经加载成功，则发送事件
            if([TMJPushModule sharedInstance].bridge.isLoading) {
                [self didReceiveRemoteNotificationWhenFirstLaunchApp:launchOptions];
            }
            else {
                [JPUSHService handleRemoteNotification:launchOptions];
                [[TMJPushModule sharedInstance] didOpenRemoteNotification:launchOptions];
            }
        });
    }
}
+ (void)setBadgeNumber:(int)badge {
    [[UIApplication sharedApplication] setApplicationIconBadgeNumber:badge];
    [JPUSHService setBadge:badge];
}


-(NSDateFormatter *)dateFormatter
{
    if (!_dateFormatter) {
        _dateFormatter = [[NSDateFormatter alloc] init];
        _dateFormatter.dateFormat = @"MMddHHmmss";
    }
    return _dateFormatter;
}
-(NSInteger)getSequence {
    NSString *dateString = [self.dateFormatter stringFromDate:[NSDate date]];
    if (!dateString || dateString.length == 0) {
        return 0;
    }
    return [dateString integerValue];
}

@end
  
