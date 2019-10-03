//
//  RNCallKeep.m
//  RNCallKeep
//
//  Copyright 2016-2019 The CallKeep Authors (see the AUTHORS file)
//  SPDX-License-Identifier: ISC, MIT
//

#import "RNCallKeep.h"

#import <React/RCTBridge.h>
#import <React/RCTConvert.h>
#import <React/RCTEventDispatcher.h>
#import <React/RCTUtils.h>

#import <AVFoundation/AVAudioSession.h>

#ifdef DEBUG
static int const OUTGOING_CALL_WAKEUP_DELAY = 10;
#else
static int const OUTGOING_CALL_WAKEUP_DELAY = 5;
#endif

static NSString *const RNCallKeepHandleStartCallNotification = @"RNCallKeepHandleStartCallNotification";
static NSString *const RNCallKeepDidReceiveStartCallAction = @"RNCallKeepDidReceiveStartCallAction";
static NSString *const RNCallKeepPerformAnswerCallAction = @"RNCallKeepPerformAnswerCallAction";
static NSString *const RNCallKeepPerformEndCallAction = @"RNCallKeepPerformEndCallAction";
static NSString *const RNCallKeepDidActivateAudioSession = @"RNCallKeepDidActivateAudioSession";
static NSString *const RNCallKeepDidDeactivateAudioSession = @"RNCallKeepDidDeactivateAudioSession";
static NSString *const RNCallKeepDidDisplayIncomingCall = @"RNCallKeepDidDisplayIncomingCall";
static NSString *const RNCallKeepDidPerformSetMutedCallAction = @"RNCallKeepDidPerformSetMutedCallAction";
static NSString *const RNCallKeepPerformPlayDTMFCallAction = @"RNCallKeepDidPerformDTMFAction";
static NSString *const RNCallKeepDidToggleHoldAction = @"RNCallKeepDidToggleHoldAction";
static NSString *const RNCallKeepProviderReset = @"RNCallKeepProviderReset";
static NSString *const RNCallKeepCheckReachability = @"RNCallKeepCheckReachability";

@implementation RNCallKeep
{
    NSOperatingSystemVersion _version;
    BOOL _isStartCallActionEventListenerAdded;
}

static CXProvider* sharedProvider;

// should initialise in AppDelegate.m
RCT_EXPORT_MODULE()

- (instancetype)init
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][init]");
#endif
    if (self = [super init]) {
        _isStartCallActionEventListenerAdded = NO;
    }
    return self;
}

+ (id)allocWithZone:(NSZone *)zone {
    static RNCallKeep *sharedInstance = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        sharedInstance = [super allocWithZone:zone];
    });
    return sharedInstance;
}

- (void)dealloc
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][dealloc]");
#endif
    [[NSNotificationCenter defaultCenter] removeObserver:self];

    if (self.callKeepProvider != nil) {
        [self.callKeepProvider invalidate];
    }
    sharedProvider = nil;
}

// Override method of RCTEventEmitter
- (NSArray<NSString *> *)supportedEvents
{
    return @[
        RNCallKeepDidReceiveStartCallAction,
        RNCallKeepPerformAnswerCallAction,
        RNCallKeepPerformEndCallAction,
        RNCallKeepDidActivateAudioSession,
        RNCallKeepDidDeactivateAudioSession,
        RNCallKeepDidDisplayIncomingCall,
        RNCallKeepDidPerformSetMutedCallAction,
        RNCallKeepPerformPlayDTMFCallAction,
        RNCallKeepDidToggleHoldAction,
        RNCallKeepProviderReset,
        RNCallKeepCheckReachability
    ];
}

+ (void)initCallKitProvider {
    if (sharedProvider == nil) {
        NSDictionary *settings = [[NSUserDefaults standardUserDefaults] dictionaryForKey:@"RNCallKeepSettings"];
        sharedProvider = [[CXProvider alloc] initWithConfiguration:[RNCallKeep getProviderConfiguration:settings]];
    }
}

RCT_EXPORT_METHOD(setup:(NSDictionary *)options)
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][setup] options = %@", options);
#endif
    _version = [[[NSProcessInfo alloc] init] operatingSystemVersion];
    self.callKeepCallController = [[CXCallController alloc] init];
    NSDictionary *settings = [[NSMutableDictionary alloc] initWithDictionary:options];
    // Store settings in NSUserDefault
    [[NSUserDefaults standardUserDefaults] setObject:settings forKey:@"RNCallKeepSettings"];
    [[NSUserDefaults standardUserDefaults] synchronize];

    [RNCallKeep initCallKitProvider];

    self.callKeepProvider = sharedProvider;
    [self.callKeepProvider setDelegate:self queue:nil];
}

RCT_REMAP_METHOD(checkIfBusy,
                 checkIfBusyWithResolver:(RCTPromiseResolveBlock)resolve
                 rejecter:(RCTPromiseRejectBlock)reject)
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][checkIfBusy]");
#endif
    resolve(@(self.callKeepCallController.callObserver.calls.count > 0));
}

RCT_REMAP_METHOD(checkSpeaker,
                 checkSpeakerResolver:(RCTPromiseResolveBlock)resolve
                 rejecter:(RCTPromiseRejectBlock)reject)
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][checkSpeaker]");
#endif
    NSString *output = [AVAudioSession sharedInstance].currentRoute.outputs.count > 0 ? [AVAudioSession sharedInstance].currentRoute.outputs[0].portType : nil;
    resolve(@([output isEqualToString:@"Speaker"]));
}

#pragma mark - CXCallController call actions

// Display the incoming call to the user
RCT_EXPORT_METHOD(displayIncomingCall:(NSString *)uuidString
                               handle:(NSString *)handle
                           handleType:(NSString *)handleType
                             hasVideo:(BOOL)hasVideo
                  localizedCallerName:(NSString * _Nullable)localizedCallerName)
{
    [RNCallKeep reportNewIncomingCall: uuidString handle:handle handleType:handleType hasVideo:hasVideo localizedCallerName:localizedCallerName fromPushKit: NO];
}

RCT_EXPORT_METHOD(startCall:(NSString *)uuidString
                     handle:(NSString *)handle
          contactIdentifier:(NSString * _Nullable)contactIdentifier
                 handleType:(NSString *)handleType
                      video:(BOOL)video)
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][startCall] uuidString = %@", uuidString);
#endif
    int _handleType = [RNCallKeep getHandleType:handleType];
    NSUUID *uuid = [[NSUUID alloc] initWithUUIDString:uuidString];
    CXHandle *callHandle = [[CXHandle alloc] initWithType:_handleType value:handle];
    CXStartCallAction *startCallAction = [[CXStartCallAction alloc] initWithCallUUID:uuid handle:callHandle];
    [startCallAction setVideo:video];
    [startCallAction setContactIdentifier:contactIdentifier];

    CXTransaction *transaction = [[CXTransaction alloc] initWithAction:startCallAction];

    [self requestTransaction:transaction];
}

RCT_EXPORT_METHOD(endCall:(NSString *)uuidString)
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][endCall] uuidString = %@", uuidString);
#endif
    NSUUID *uuid = [[NSUUID alloc] initWithUUIDString:uuidString];
    CXEndCallAction *endCallAction = [[CXEndCallAction alloc] initWithCallUUID:uuid];
    CXTransaction *transaction = [[CXTransaction alloc] initWithAction:endCallAction];

    [self requestTransaction:transaction];
}

RCT_EXPORT_METHOD(endAllCalls)
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][endAllCalls] calls = %@", self.callKeepCallController.callObserver.calls);
#endif
    for (CXCall *call in self.callKeepCallController.callObserver.calls) {
        CXEndCallAction *endCallAction = [[CXEndCallAction alloc] initWithCallUUID:call.UUID];
        CXTransaction *transaction = [[CXTransaction alloc] initWithAction:endCallAction];
        [self requestTransaction:transaction];
    }
}

RCT_EXPORT_METHOD(setOnHold:(NSString *)uuidString :(BOOL)shouldHold)
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][setOnHold] uuidString = %@, shouldHold = %d", uuidString, shouldHold);
#endif
    NSUUID *uuid = [[NSUUID alloc] initWithUUIDString:uuidString];
    CXSetHeldCallAction *setHeldCallAction = [[CXSetHeldCallAction alloc] initWithCallUUID:uuid onHold:shouldHold];
    CXTransaction *transaction = [[CXTransaction alloc] init];
    [transaction addAction:setHeldCallAction];

    [self requestTransaction:transaction];
}

RCT_EXPORT_METHOD(_startCallActionEventListenerAdded)
{
    _isStartCallActionEventListenerAdded = YES;
}

RCT_EXPORT_METHOD(reportConnectingOutgoingCallWithUUID:(NSString *)uuidString)
{
    NSUUID *uuid = [[NSUUID alloc] initWithUUIDString:uuidString];
    [self.callKeepProvider reportOutgoingCallWithUUID:uuid startedConnectingAtDate:[NSDate date]];
}

RCT_EXPORT_METHOD(reportConnectedOutgoingCallWithUUID:(NSString *)uuidString)
{
    NSUUID *uuid = [[NSUUID alloc] initWithUUIDString:uuidString];
    [self.callKeepProvider reportOutgoingCallWithUUID:uuid connectedAtDate:[NSDate date]];
}

RCT_EXPORT_METHOD(reportEndCallWithUUID:(NSString *)uuidString :(int)reason)
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][reportEndCallWithUUID] uuidString = %@ reason = %d", uuidString, reason);
#endif
    NSUUID *uuid = [[NSUUID alloc] initWithUUIDString:uuidString];
    switch (reason) {
        case CXCallEndedReasonFailed:
            [self.callKeepProvider reportCallWithUUID:uuid endedAtDate:[NSDate date] reason:CXCallEndedReasonFailed];
            break;
        case CXCallEndedReasonRemoteEnded:
            [self.callKeepProvider reportCallWithUUID:uuid endedAtDate:[NSDate date] reason:CXCallEndedReasonRemoteEnded];
            break;
        case CXCallEndedReasonUnanswered:
            [self.callKeepProvider reportCallWithUUID:uuid endedAtDate:[NSDate date] reason:CXCallEndedReasonUnanswered];
            break;
        default:
            break;
    }
}

RCT_EXPORT_METHOD(updateDisplay:(NSString *)uuidString :(NSString *)displayName :(NSString *)uri)
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][updateDisplay] uuidString = %@ displayName = %@ uri = %@", uuidString, displayName, uri);
#endif
    NSUUID *uuid = [[NSUUID alloc] initWithUUIDString:uuidString];
    CXHandle *callHandle = [[CXHandle alloc] initWithType:CXHandleTypePhoneNumber value:uri];
    CXCallUpdate *callUpdate = [[CXCallUpdate alloc] init];
    callUpdate.localizedCallerName = displayName;
    callUpdate.remoteHandle = callHandle;
    [self.callKeepProvider reportCallWithUUID:uuid updated:callUpdate];
}

RCT_EXPORT_METHOD(setMutedCall:(NSString *)uuidString :(BOOL)muted)
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][setMutedCall] muted = %i", muted);
#endif
    NSUUID *uuid = [[NSUUID alloc] initWithUUIDString:uuidString];
    CXSetMutedCallAction *setMutedAction = [[CXSetMutedCallAction alloc] initWithCallUUID:uuid muted:muted];
    CXTransaction *transaction = [[CXTransaction alloc] init];
    [transaction addAction:setMutedAction];

    [self requestTransaction:transaction];
}

RCT_EXPORT_METHOD(sendDTMF:(NSString *)uuidString dtmf:(NSString *)key)
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][sendDTMF] key = %@", key);
#endif
    NSUUID *uuid = [[NSUUID alloc] initWithUUIDString:uuidString];
    CXPlayDTMFCallAction *dtmfAction = [[CXPlayDTMFCallAction alloc] initWithCallUUID:uuid digits:key type:CXPlayDTMFCallActionTypeHardPause];
    CXTransaction *transaction = [[CXTransaction alloc] init];
    [transaction addAction:dtmfAction];

    [self requestTransaction:transaction];
}

- (void)requestTransaction:(CXTransaction *)transaction
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][requestTransaction] transaction = %@", transaction);
#endif
    if (self.callKeepCallController == nil) {
        self.callKeepCallController = [[CXCallController alloc] init];
    }
    [self.callKeepCallController requestTransaction:transaction completion:^(NSError * _Nullable error) {
        if (error != nil) {
            NSLog(@"[RNCallKeep][requestTransaction] Error requesting transaction (%@): (%@)", transaction.actions, error);
        } else {
            NSLog(@"[RNCallKeep][requestTransaction] Requested transaction successfully");

            // CXStartCallAction
            if ([[transaction.actions firstObject] isKindOfClass:[CXStartCallAction class]]) {
                CXStartCallAction *startCallAction = [transaction.actions firstObject];
                CXCallUpdate *callUpdate = [[CXCallUpdate alloc] init];
                callUpdate.remoteHandle = startCallAction.handle;
                callUpdate.hasVideo = startCallAction.video;
                callUpdate.localizedCallerName = startCallAction.contactIdentifier;
                callUpdate.supportsDTMF = YES;
                callUpdate.supportsHolding = YES;
                callUpdate.supportsGrouping = YES;
                callUpdate.supportsUngrouping = YES;
                [self.callKeepProvider reportCallWithUUID:startCallAction.callUUID updated:callUpdate];
            }
        }
    }];
}

+ (void)reportNewIncomingCall:(NSString *)uuidString
                       handle:(NSString *)handle
                   handleType:(NSString *)handleType
                     hasVideo:(BOOL)hasVideo
          localizedCallerName:(NSString * _Nullable)localizedCallerName
                  fromPushKit:(BOOL)fromPushKit
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][reportNewIncomingCall] uuidString = %@", uuidString);
#endif
    int _handleType = [RNCallKeep getHandleType:handleType];
    NSUUID *uuid = [[NSUUID alloc] initWithUUIDString:uuidString];
    CXCallUpdate *callUpdate = [[CXCallUpdate alloc] init];
    callUpdate.remoteHandle = [[CXHandle alloc] initWithType:_handleType value:handle];
    callUpdate.supportsDTMF = YES;
    callUpdate.supportsHolding = YES;
    callUpdate.supportsGrouping = YES;
    callUpdate.supportsUngrouping = YES;
    callUpdate.hasVideo = hasVideo;
    callUpdate.localizedCallerName = localizedCallerName;

    [RNCallKeep initCallKitProvider];
    [sharedProvider reportNewIncomingCallWithUUID:uuid update:callUpdate completion:^(NSError * _Nullable error) {
        RNCallKeep *callKeep = [RNCallKeep allocWithZone: nil];
        [callKeep sendEventWithName:RNCallKeepDidDisplayIncomingCall body:@{ @"error": error ? error.localizedDescription : @"", @"callUUID": uuidString, @"handle": handle, @"localizedCallerName": localizedCallerName, @"fromPushKit": fromPushKit ? @"1" : @"0" }];
        if (error == nil) {
            // Workaround per https://forums.developer.apple.com/message/169511
            if ([callKeep lessThanIos10_2]) {
                [callKeep configureAudioSession];
            }
        }
    }];
}

- (BOOL)lessThanIos10_2
{
    if (_version.majorVersion < 10) {
        return YES;
    } else if (_version.majorVersion > 10) {
        return NO;
    } else {
        return _version.minorVersion < 2;
    }
}

+ (int)getHandleType:(NSString *)handleType
{
    int _handleType;
    if ([handleType isEqualToString:@"generic"]) {
        _handleType = CXHandleTypeGeneric;
    } else if ([handleType isEqualToString:@"number"]) {
        _handleType = CXHandleTypePhoneNumber;
    } else if ([handleType isEqualToString:@"email"]) {
        _handleType = CXHandleTypeEmailAddress;
    } else {
        _handleType = CXHandleTypeGeneric;
    }
    return _handleType;
}

+ (CXProviderConfiguration *)getProviderConfiguration:(NSDictionary*)settings
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][getProviderConfiguration]");
#endif
    CXProviderConfiguration *providerConfiguration = [[CXProviderConfiguration alloc] initWithLocalizedName:settings[@"appName"]];
    providerConfiguration.supportsVideo = YES;
    providerConfiguration.maximumCallGroups = 3;
    providerConfiguration.maximumCallsPerCallGroup = 1;
    providerConfiguration.supportedHandleTypes = [NSSet setWithObjects:[NSNumber numberWithInteger:CXHandleTypePhoneNumber], nil];
    if (settings[@"supportsVideo"]) {
        providerConfiguration.supportsVideo = [settings[@"supportsVideo"] boolValue];
    }
    if (settings[@"maximumCallGroups"]) {
        providerConfiguration.maximumCallGroups = [settings[@"maximumCallGroups"] integerValue];
    }
    if (settings[@"maximumCallsPerCallGroup"]) {
        providerConfiguration.maximumCallsPerCallGroup = [settings[@"maximumCallsPerCallGroup"] integerValue];
    }
    if (settings[@"imageName"]) {
        providerConfiguration.iconTemplateImageData = UIImagePNGRepresentation([UIImage imageNamed:settings[@"imageName"]]);
    }
    if (settings[@"ringtoneSound"]) {
        providerConfiguration.ringtoneSound = settings[@"ringtoneSound"];
    }
    return providerConfiguration;
}

- (void)configureAudioSession
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][configureAudioSession] Activating audio session");
#endif

    AVAudioSession* audioSession = [AVAudioSession sharedInstance];
    [audioSession setCategory:AVAudioSessionCategoryPlayAndRecord withOptions:AVAudioSessionCategoryOptionAllowBluetooth error:nil];

    [audioSession setMode:AVAudioSessionModeVoiceChat error:nil];

    double sampleRate = 44100.0;
    [audioSession setPreferredSampleRate:sampleRate error:nil];

    NSTimeInterval bufferDuration = .005;
    [audioSession setPreferredIOBufferDuration:bufferDuration error:nil];
    [audioSession setActive:TRUE error:nil];
}

+ (BOOL)application:(UIApplication *)application
            openURL:(NSURL *)url
            options:(NSDictionary<UIApplicationOpenURLOptionsKey, id> *)options NS_AVAILABLE_IOS(9_0)
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][application:openURL]");
#endif
    /*
    NSString *handle = [url startCallHandle];
    if (handle != nil && handle.length > 0 ){
        NSDictionary *userInfo = @{
            @"handle": handle,
            @"video": @NO
        };
        [[NSNotificationCenter defaultCenter] postNotificationName:RNCallKeepHandleStartCallNotification
                                                            object:self
                                                          userInfo:userInfo];
        return YES;
    }
    return NO;
    */
    return YES;
}

+ (BOOL)application:(UIApplication *)application
continueUserActivity:(NSUserActivity *)userActivity
 restorationHandler:(void(^)(NSArray * __nullable restorableObjects))restorationHandler
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][application:continueUserActivity]");
#endif
    INInteraction *interaction = userActivity.interaction;
    INPerson *contact;
    NSString *handle;
    BOOL isAudioCall;
    BOOL isVideoCall;

//HACK TO AVOID XCODE 10 COMPILE CRASH
//REMOVE ON NEXT MAJOR RELEASE OF RNCALLKIT
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 130000
    //XCode 11
    // iOS 13 returns an INStartCallIntent userActivity type
    if (@available(iOS 13, *)) {
        INStartCallIntent *intent = (INStartCallIntent*)interaction.intent;
        isAudioCall = intent.callCapability == INCallCapabilityAudioCall;
        isVideoCall = intent.callCapability == INCallCapabilityVideoCall;
    } else {
#endif
        //XCode 10 and below
        isAudioCall = [userActivity.activityType isEqualToString:INStartAudioCallIntentIdentifier];
        isVideoCall = [userActivity.activityType isEqualToString:INStartVideoCallIntentIdentifier];
//HACK TO AVOID XCODE 10 COMPILE CRASH
//REMOVE ON NEXT MAJOR RELEASE OF RNCALLKIT
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 130000
    }
#endif

    if (isAudioCall) {
        INStartAudioCallIntent *startAudioCallIntent = (INStartAudioCallIntent *)interaction.intent;
        contact = [startAudioCallIntent.contacts firstObject];
    } else if (isVideoCall) {
        INStartVideoCallIntent *startVideoCallIntent = (INStartVideoCallIntent *)interaction.intent;
        contact = [startVideoCallIntent.contacts firstObject];
    }

    if (contact != nil) {
        handle = contact.personHandle.value;
    }

    if (handle != nil && handle.length > 0 ){
        NSDictionary *userInfo = @{
            @"handle": handle,
            @"video": @(isVideoCall)
        };

        RNCallKeep *callKeep = [RNCallKeep allocWithZone: nil];
        [callKeep handleStartCallNotification: userInfo];
        return YES;
    }
    return NO;
}

+ (BOOL)requiresMainQueueSetup
{
    return YES;
}

- (void)handleStartCallNotification:(NSDictionary *)userInfo
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][handleStartCallNotification] userInfo = %@", userInfo);
#endif
    int delayInSeconds;
    if (!_isStartCallActionEventListenerAdded) {
        // Workaround for when app is just launched and JS side hasn't registered to the event properly
        delayInSeconds = OUTGOING_CALL_WAKEUP_DELAY;
    } else {
        delayInSeconds = 0;
    }
    dispatch_time_t popTime = dispatch_time(DISPATCH_TIME_NOW, (int64_t)(delayInSeconds * NSEC_PER_SEC));
    dispatch_after(popTime, dispatch_get_main_queue(), ^{
        [self sendEventWithName:RNCallKeepDidReceiveStartCallAction body:userInfo];
    });
}

#pragma mark - CXProviderDelegate

- (void)providerDidReset:(CXProvider *)provider{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][providerDidReset]");
#endif
    //this means something big changed, so tell the JS. The JS should
    //probably respond by hanging up all calls.
    [self sendEventWithName:RNCallKeepProviderReset body:nil];
}

// Starting outgoing call
- (void)provider:(CXProvider *)provider performStartCallAction:(CXStartCallAction *)action
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][CXProviderDelegate][provider:performStartCallAction]");
#endif
    //do this first, audio sessions are flakey
    [self configureAudioSession];
    //tell the JS to actually make the call
    [self sendEventWithName:RNCallKeepDidReceiveStartCallAction body:@{ @"callUUID": [action.callUUID.UUIDString lowercaseString], @"handle": action.handle.value }];
    [action fulfill];
}

// Update call contact info
// @deprecated
RCT_EXPORT_METHOD(reportUpdatedCall:(NSString *)uuidString contactIdentifier:(NSString *)contactIdentifier)
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][reportUpdatedCall] contactIdentifier = %i", contactIdentifier);
#endif
    NSUUID *uuid = [[NSUUID alloc] initWithUUIDString:uuidString];
    CXCallUpdate *callUpdate = [[CXCallUpdate alloc] init];
    callUpdate.localizedCallerName = contactIdentifier;

    [self.callKeepProvider reportCallWithUUID:uuid updated:callUpdate];
}

// Answering incoming call
- (void)provider:(CXProvider *)provider performAnswerCallAction:(CXAnswerCallAction *)action
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][CXProviderDelegate][provider:performAnswerCallAction]");
#endif
    [self configureAudioSession];
    [self sendEventWithName:RNCallKeepPerformAnswerCallAction body:@{ @"callUUID": [action.callUUID.UUIDString lowercaseString] }];
    [action fulfill];
}

// Ending incoming call
- (void)provider:(CXProvider *)provider performEndCallAction:(CXEndCallAction *)action
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][CXProviderDelegate][provider:performEndCallAction]");
#endif
    [self sendEventWithName:RNCallKeepPerformEndCallAction body:@{ @"callUUID": [action.callUUID.UUIDString lowercaseString] }];
    [action fulfill];
}

-(void)provider:(CXProvider *)provider performSetHeldCallAction:(CXSetHeldCallAction *)action
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][CXProviderDelegate][provider:performSetHeldCallAction]");
#endif

    [self sendEventWithName:RNCallKeepDidToggleHoldAction body:@{ @"hold": @(action.onHold), @"callUUID": [action.callUUID.UUIDString lowercaseString] }];
    [action fulfill];
}

- (void)provider:(CXProvider *)provider performPlayDTMFCallAction:(CXPlayDTMFCallAction *)action {
#ifdef DEBUG
    NSLog(@"[RNCallKeep][CXProviderDelegate][provider:performPlayDTMFCallAction]");
#endif
    [self sendEventWithName:RNCallKeepPerformPlayDTMFCallAction body:@{ @"digits": action.digits, @"callUUID": [action.callUUID.UUIDString lowercaseString] }];
    [action fulfill];
}

-(void)provider:(CXProvider *)provider performSetMutedCallAction:(CXSetMutedCallAction *)action
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][CXProviderDelegate][provider:performSetMutedCallAction]");
#endif

    [self sendEventWithName:RNCallKeepDidPerformSetMutedCallAction body:@{ @"muted": @(action.muted), @"callUUID": [action.callUUID.UUIDString lowercaseString] }];
    [action fulfill];
}

- (void)provider:(CXProvider *)provider timedOutPerformingAction:(CXAction *)action
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][CXProviderDelegate][provider:timedOutPerformingAction]");
#endif
}

- (void)provider:(CXProvider *)provider didActivateAudioSession:(AVAudioSession *)audioSession
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][CXProviderDelegate][provider:didActivateAudioSession]");
#endif
    NSDictionary *userInfo
    = @{
        AVAudioSessionInterruptionTypeKey: [NSNumber numberWithInt:AVAudioSessionInterruptionTypeEnded],
        AVAudioSessionInterruptionOptionKey: [NSNumber numberWithInt:AVAudioSessionInterruptionOptionShouldResume]
    };
    [[NSNotificationCenter defaultCenter] postNotificationName:AVAudioSessionInterruptionNotification object:nil userInfo:userInfo];

    [self configureAudioSession];
    [self sendEventWithName:RNCallKeepDidActivateAudioSession body:nil];
}

- (void)provider:(CXProvider *)provider didDeactivateAudioSession:(AVAudioSession *)audioSession
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][CXProviderDelegate][provider:didDeactivateAudioSession]");
#endif
    [self sendEventWithName:RNCallKeepDidDeactivateAudioSession body:nil];
}

@end
