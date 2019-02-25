//
//  RNCallKeep.m
//  RNCallKeep
//
//  Copyright 2016-2018 The CallKeep Authors (see the AUTHORS file)
//  SPDX-License-Identifier: ISC, MIT
//

#import "RNCallKeep.h"

#import <React/RCTBridge.h>
#import <React/RCTConvert.h>
#import <React/RCTEventDispatcher.h>
#import <React/RCTUtils.h>

#import <AVFoundation/AVAudioSession.h>

static int const DelayInSeconds = 3;

static NSString *const RNCallKeepHandleStartCallNotification = @"RNCallKeepHandleStartCallNotification";
static NSString *const RNCallKeepDidReceiveStartCallAction = @"RNCallKeepDidReceiveStartCallAction";
static NSString *const RNCallKeepPerformAnswerCallAction = @"RNCallKeepPerformAnswerCallAction";
static NSString *const RNCallKeepPerformEndCallAction = @"RNCallKeepPerformEndCallAction";
static NSString *const RNCallKeepDidActivateAudioSession = @"RNCallKeepDidActivateAudioSession";
static NSString *const RNCallKeepDidDisplayIncomingCall = @"RNCallKeepDidDisplayIncomingCall";
static NSString *const RNCallKeepDidPerformSetMutedCallAction = @"RNCallKeepDidPerformSetMutedCallAction";
static NSString *const RNCallKeepPerformPlayDTMFCallAction = @"RNCallKeepDidPerformDTMFAction";
static NSString *const RNCallKeepDidToggleHoldAction = @"RNCallKeepDidToggleHoldAction";

@implementation RNCallKeep
{
    NSMutableDictionary *_settings;
    NSOperatingSystemVersion _version;
    BOOL _isStartCallActionEventListenerAdded;
}

// should initialise in AppDelegate.m
RCT_EXPORT_MODULE()

- (instancetype)init
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][init]");
#endif
    if (self = [super init]) {
        [[NSNotificationCenter defaultCenter] addObserver:self
                                                 selector:@selector(handleStartCallNotification:)
                                                     name:RNCallKeepHandleStartCallNotification
                                                   object:nil];
        _isStartCallActionEventListenerAdded = NO;
    }
    return self;
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
}

// Override method of RCTEventEmitter
- (NSArray<NSString *> *)supportedEvents
{
    return @[
             RNCallKeepDidReceiveStartCallAction,
             RNCallKeepPerformAnswerCallAction,
             RNCallKeepPerformEndCallAction,
             RNCallKeepDidActivateAudioSession,
             RNCallKeepDidDisplayIncomingCall,
             RNCallKeepDidPerformSetMutedCallAction,
             RNCallKeepPerformPlayDTMFCallAction,
             RNCallKeepDidToggleHoldAction
             ];
}

RCT_EXPORT_METHOD(setup:(NSDictionary *)options)
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][setup] options = %@", options);
#endif
    _version = [[[NSProcessInfo alloc] init] operatingSystemVersion];
    self.callKeepCallController = [[CXCallController alloc] init];
    _settings = [[NSMutableDictionary alloc] initWithDictionary:options];
    self.callKeepProvider = [[CXProvider alloc] initWithConfiguration:[self getProviderConfiguration]];
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
#ifdef DEBUG
    NSLog(@"[RNCallKeep][displayIncomingCall] uuidString = %@", uuidString);
#endif
    int _handleType = [self getHandleType:handleType];
    NSUUID *uuid = [[NSUUID alloc] initWithUUIDString:uuidString];
    CXCallUpdate *callUpdate = [[CXCallUpdate alloc] init];
    callUpdate.remoteHandle = [[CXHandle alloc] initWithType:_handleType value:handle];
    callUpdate.supportsDTMF = YES;
    callUpdate.supportsHolding = YES;
    callUpdate.supportsGrouping = YES;
    callUpdate.supportsUngrouping = YES;
    callUpdate.hasVideo = NO;
    callUpdate.localizedCallerName = localizedCallerName;

    [self.callKeepProvider reportNewIncomingCallWithUUID:uuid update:callUpdate completion:^(NSError * _Nullable error) {
        [self sendEventWithName:RNCallKeepDidDisplayIncomingCall body:@{ @"error": error ? error.localizedDescription : @"" }];
        if (error == nil) {
            // Workaround per https://forums.developer.apple.com/message/169511
            if ([self lessThanIos10_2]) {
                [self configureAudioSession];
            }
        }
    }];
}

RCT_EXPORT_METHOD(startCall:(NSString *)uuidString
                     handle:(NSString *)handle
                 handleType:(NSString *)handleType
                      video:(BOOL)video
          contactIdentifier:(NSString * _Nullable)contactIdentifier)
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][startCall] uuidString = %@", uuidString);
#endif
    int _handleType = [self getHandleType:handleType];
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

RCT_EXPORT_METHOD(setHeldCall:(NSString *)uuidString onHold:(BOOL)onHold)
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][setHeldCall] uuidString = %@", uuidString);
#endif
    NSUUID *uuid = [[NSUUID alloc] initWithUUIDString:uuidString];
    CXSetHeldCallAction *setHeldCallAction = [[CXSetHeldCallAction alloc] initWithCallUUID:uuid onHold:onHold];
    CXTransaction *transaction = [[CXTransaction alloc] init];
    [transaction addAction:setHeldCallAction];

    [self requestTransaction:transaction];
}

RCT_EXPORT_METHOD(_startCallActionEventListenerAdded)
{
    _isStartCallActionEventListenerAdded = YES;
}

RCT_EXPORT_METHOD(reportConnectedOutgoingCallWithUUID:(NSString *)uuidString)
{
    NSUUID *uuid = [[NSUUID alloc] initWithUUIDString:uuidString];
    [self.callKeepProvider reportOutgoingCallWithUUID:uuid connectedAtDate:[NSDate date]];
}

RCT_EXPORT_METHOD(setMutedCall:(NSString *)uuidString muted:(BOOL)muted)
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
                callUpdate.supportsDTMF = YES;
                callUpdate.supportsHolding = YES;
                callUpdate.supportsGrouping = YES;
                callUpdate.supportsUngrouping = YES;
                callUpdate.hasVideo = NO;
                [self.callKeepProvider reportCallWithUUID:startCallAction.callUUID updated:callUpdate];
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

- (BOOL)containsLowerCaseLetter:(NSString *)callUUID
{
    NSRegularExpression* regex = [[NSRegularExpression alloc] initWithPattern:@"[a-z]" options:0 error:nil];
    return [regex numberOfMatchesInString:callUUID options:0 range:NSMakeRange(0, [callUUID length])] > 0;
}

- (int)getHandleType:(NSString *)handleType
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

- (CXProviderConfiguration *)getProviderConfiguration
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][getProviderConfiguration]");
#endif
    CXProviderConfiguration *providerConfiguration = [[CXProviderConfiguration alloc] initWithLocalizedName:_settings[@"appName"]];
    providerConfiguration.supportsVideo = YES;
    providerConfiguration.maximumCallGroups = 3;
    providerConfiguration.maximumCallsPerCallGroup = 1;
    providerConfiguration.supportedHandleTypes = [NSSet setWithObjects:[NSNumber numberWithInteger:CXHandleTypePhoneNumber], [NSNumber numberWithInteger:CXHandleTypeEmailAddress], [NSNumber numberWithInteger:CXHandleTypeGeneric], nil];
    if (_settings[@"imageName"]) {
        providerConfiguration.iconTemplateImageData = UIImagePNGRepresentation([UIImage imageNamed:_settings[@"imageName"]]);
    }
    if (_settings[@"ringtoneSound"]) {
        providerConfiguration.ringtoneSound = _settings[@"ringtoneSound"];
    }
    return providerConfiguration;
}

- (void)configureAudioSession
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][configureAudioSession] Activating audio session");
#endif

    AVAudioSession* audioSession = [AVAudioSession sharedInstance];
    [audioSession setCategory:AVAudioSessionCategoryPlayAndRecord error:nil];

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
    BOOL isAudioCall = [userActivity.activityType isEqualToString:INStartAudioCallIntentIdentifier];
    BOOL isVideoCall = [userActivity.activityType isEqualToString:INStartVideoCallIntentIdentifier];

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

        [[NSNotificationCenter defaultCenter] postNotificationName:RNCallKeepHandleStartCallNotification
                                                            object:self
                                                          userInfo:userInfo];
        return YES;
    }
    return NO;
}

+ (BOOL)requiresMainQueueSetup
{
    return YES;
}

- (void)handleStartCallNotification:(NSNotification *)notification
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][handleStartCallNotification] userInfo = %@", notification.userInfo);
#endif
    int delayInSeconds;
    if (!_isStartCallActionEventListenerAdded) {
        // Workaround for when app is just launched and JS side hasn't registered to the event properly
        delayInSeconds = DelayInSeconds;
    } else {
        delayInSeconds = 0;
    }
    dispatch_time_t popTime = dispatch_time(DISPATCH_TIME_NOW, (int64_t)(delayInSeconds * NSEC_PER_SEC));
    dispatch_after(popTime, dispatch_get_main_queue(), ^{
        [self sendEventWithName:RNCallKeepDidReceiveStartCallAction body:notification.userInfo];
    });
}

#pragma mark - CXProviderDelegate

- (void)providerDidReset:(CXProvider *)provider{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][providerDidReset]");
#endif
}

// Starting outgoing call
- (void)provider:(CXProvider *)provider performStartCallAction:(CXStartCallAction *)action
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][CXProviderDelegate][provider:performStartCallAction]");
#endif
    [self.callKeepProvider reportOutgoingCallWithUUID:action.callUUID startedConnectingAtDate:[NSDate date]];
    [self configureAudioSession];
    [action fulfill];
}

// Answering incoming call
- (void)provider:(CXProvider *)provider performAnswerCallAction:(CXAnswerCallAction *)action
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][CXProviderDelegate][provider:performAnswerCallAction]");
#endif
    if (![self lessThanIos10_2]) {
        [self configureAudioSession];
    }
    NSString *callUUID = [self containsLowerCaseLetter:action.callUUID.UUIDString] ? action.callUUID.UUIDString : [action.callUUID.UUIDString lowercaseString];
    [self sendEventWithName:RNCallKeepPerformAnswerCallAction body:@{ @"callUUID": callUUID }];
    [action fulfill];
}

// Ending incoming call
- (void)provider:(CXProvider *)provider performEndCallAction:(CXEndCallAction *)action
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][CXProviderDelegate][provider:performEndCallAction]");
#endif
    NSString *callUUID = [self containsLowerCaseLetter:action.callUUID.UUIDString] ? action.callUUID.UUIDString : [action.callUUID.UUIDString lowercaseString];
    [self sendEventWithName:RNCallKeepPerformEndCallAction body:@{ @"callUUID": callUUID }];
    [action fulfill];
}

-(void)provider:(CXProvider *)provider performSetHeldCallAction:(CXSetHeldCallAction *)action
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][CXProviderDelegate][provider:performSetHeldCallAction]");
#endif
    NSString *callUUID = [self containsLowerCaseLetter:action.callUUID.UUIDString] ? action.callUUID.UUIDString : [action.callUUID.UUIDString lowercaseString];

    [self sendEventWithName:RNCallKeepDidToggleHoldAction body:@{ @"hold": @(action.onHold), @"callUUID": callUUID }];
    [action fulfill];
}

- (void)provider:(CXProvider *)provider performPlayDTMFCallAction:(CXPlayDTMFCallAction *)action {
#ifdef DEBUG
    NSLog(@"[RNCallKeep][CXProviderDelegate][provider:performPlayDTMFCallAction]");
#endif
    NSString *callUUID = [self containsLowerCaseLetter:action.callUUID.UUIDString] ? action.callUUID.UUIDString : [action.callUUID.UUIDString lowercaseString];
    [self sendEventWithName:RNCallKeepPerformPlayDTMFCallAction body:@{ @"digits": action.digits, @"callUUID": callUUID }];
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
    [self sendEventWithName:RNCallKeepDidActivateAudioSession body:nil];
}

- (void)provider:(CXProvider *)provider didDeactivateAudioSession:(AVAudioSession *)audioSession
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][CXProviderDelegate][provider:didDeactivateAudioSession]");
#endif
}

-(void)provider:(CXProvider *)provider performSetMutedCallAction:(CXSetMutedCallAction *)action
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][CXProviderDelegate][provider:performSetMutedCallAction]");
#endif
    [self sendEventWithName:RNCallKeepDidPerformSetMutedCallAction body:@{ @"muted": @(action.muted) }];
    [action fulfill];
}

@end
