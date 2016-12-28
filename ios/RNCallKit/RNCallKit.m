//
//  RNCallKit.m
//  RNCallKit
//
//  Created by Ian Yu-Hsun Lin on 12/22/16.
//  Copyright © 2016 Ian Yu-Hsun Lin. All rights reserved.
//

#import "RNCallKit.h"

#import "RCTBridge.h"
#import "RCTConvert.h"
#import "RCTEventDispatcher.h"
#import "RCTUtils.h"

NSString *const RNCallKitHandleStartCallNotification = @"RNCallKitHandleStartCallNotification";
NSString *const RNCallKitDidReceiveStartCallAction = @"RNCallKitDidReceiveStartCallAction";
NSString *const RNCallKitPerformAnswerCallAction = @"RNCallKitPerformAnswerCallAction";
NSString *const RNCallKitPerformEndCallAction = @"RNCallKitPerformEndCallAction";
NSString *const RNCallKitConfigureAudioSession = @"RNCallKitConfigureAudioSession";

@implementation RNCallKit
{
    //NSMutableDictionary *settings;
}

RCT_EXPORT_MODULE()

// Override method of RCTEventEmitter
- (void)startObserving
{
#ifdef DEBUG
    NSLog(@"[RNCallKit][startObserving]");
#endif
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(handleStartCallNotification:)
                                                 name:RNCallKitHandleStartCallNotification
                                               object:nil];
}

// Override method of RCTEventEmitter
- (void)stopObserving
{
#ifdef DEBUG
    NSLog(@"[RNCallKit][stopObserving]");
#endif
    [[NSNotificationCenter defaultCenter] removeObserver:self];
}

// Override method of RCTEventEmitter
- (NSArray<NSString *> *)supportedEvents
{
    return @[
        RNCallKitDidReceiveStartCallAction,
        RNCallKitPerformAnswerCallAction,
        RNCallKitPerformEndCallAction,
        RNCallKitConfigureAudioSession
    ];
}

RCT_EXPORT_METHOD(setupWithAppName:(NSString *)appName)
{
#ifdef DEBUG
    NSLog(@"[RNCallKit][setupWithAppName] appName = %@", appName);
#endif
    self.callKitCallController = [[CXCallController alloc] init];
    self.callKitProvider = [[CXProvider alloc] initWithConfiguration:[self providerConfigurationWithAppName:appName]];
    [self.callKitProvider setDelegate:self queue:nil];
    //settings = [NSMutableDictionary new];
}

#pragma mark - CXCallController call actions

// Display the incoming call to the user
RCT_EXPORT_METHOD(displayIncomingCall:(NSString *)uuidString
                               handle:(NSString *)handle
                             hasVideo:(BOOL)hasVideo)
{
#ifdef DEBUG
    NSLog(@"[RNCallKit][displayIncomingCall] uuidString = %@", uuidString);
#endif
    NSUUID *uuid = [[NSUUID alloc] initWithUUIDString:uuidString];

    CXCallUpdate *callUpdate = [[CXCallUpdate alloc] init];
    callUpdate.remoteHandle = [[CXHandle alloc] initWithType:CXHandleTypeGeneric value:handle];
    callUpdate.supportsDTMF = YES;
    callUpdate.supportsHolding = NO;
    callUpdate.supportsGrouping = NO;
    callUpdate.supportsUngrouping = NO;
    callUpdate.hasVideo = hasVideo;

    [self.callKitProvider reportNewIncomingCallWithUUID:uuid update:callUpdate completion:^(NSError * _Nullable error) {
        if (error == nil) {
            // Workaround per https://forums.developer.apple.com/message/169511
            [self configureAudioSession:@"incomingCall"];
        }
    }];
}

RCT_EXPORT_METHOD(startCall:(NSString *)uuidString
                       handle:(NSString *)handle
                      video:(BOOL)video)
{
#ifdef DEBUG
    NSLog(@"[RNCallKit][startCall] uuidString = %@", uuidString);
#endif
    NSUUID *uuid = [[NSUUID alloc] initWithUUIDString:uuidString];
    CXHandle *callHandle = [[CXHandle alloc] initWithType:CXHandleTypeGeneric value:handle];
    CXStartCallAction *startCallAction = [[CXStartCallAction alloc] initWithCallUUID:uuid handle:callHandle];
    [startCallAction setVideo:video];

    CXTransaction *transaction = [[CXTransaction alloc] initWithAction:startCallAction];

    [self requestTransaction:transaction];
}

RCT_EXPORT_METHOD(endCall:(NSString *)uuidString)
{
#ifdef DEBUG
    NSLog(@"[RNCallKit][endCall] uuidString = %@", uuidString);
#endif
    NSUUID *uuid = [[NSUUID alloc] initWithUUIDString:uuidString];
    CXEndCallAction *endCallAction = [[CXEndCallAction alloc] initWithCallUUID:uuid];
    CXTransaction *transaction = [[CXTransaction alloc] initWithAction:endCallAction];

    [self requestTransaction:transaction];
}

RCT_EXPORT_METHOD(setHeldCall:(NSString *)uuidString onHold:(BOOL)onHold)
{
#ifdef DEBUG
    NSLog(@"[RNCallKit][setHeldCall] uuidString = %@", uuidString);
#endif
    NSUUID *uuid = [[NSUUID alloc] initWithUUIDString:uuidString];
    CXSetHeldCallAction *setHeldCallAction = [[CXSetHeldCallAction alloc] initWithCallUUID:uuid onHold:onHold];
    CXTransaction *transaction = [[CXTransaction alloc] init];
    [transaction addAction:setHeldCallAction];

    [self requestTransaction:transaction];
}

- (void)requestTransaction:(CXTransaction *)transaction
{
#ifdef DEBUG
    NSLog(@"[RNCallKit][requestTransaction] transaction = %@", transaction);
#endif
    if (self.callKitCallController == nil) {
        self.callKitCallController = [[CXCallController alloc] init];
    }
    [self.callKitCallController requestTransaction:transaction completion:^(NSError * _Nullable error) {
        if (error != nil) {
            NSLog(@"[RNCallKit][requestTransaction] Error requesting transaction (%@): (%@)", transaction.actions, error);
        } else {
            NSLog(@"[RNCallKit][requestTransaction] Requested transaction successfully");
        }
    }];
}

- (CXProviderConfiguration *)providerConfigurationWithAppName:(NSString *)appName
{
#ifdef DEBUG
    NSLog(@"[RNCallKit][providerConfigurationWithAppName] appName = %@", appName);
#endif
    CXProviderConfiguration *providerConfiguration = [[CXProviderConfiguration alloc] initWithLocalizedName:appName];
    //providerConfiguration.supportsVideo = YES;
    providerConfiguration.maximumCallGroups = 1;
    providerConfiguration.maximumCallsPerCallGroup = 1;
    //providerConfiguration.supportedHandleTypes = [NSSet setWithObjects:[NSNumber numberWithInteger:CXHandleTypePhoneNumber], nil];
    providerConfiguration.iconTemplateImageData = UIImagePNGRepresentation([UIImage imageNamed:@"ahoy_img_blue"]);
    providerConfiguration.ringtoneSound = @"incallmanager_ringtone.mp3";
    return providerConfiguration;
}

- (void)configureAudioSession:(NSString *)type
{
#ifdef DEBUG
    NSLog(@"[RNCallKit][configureAudioSession] Activating audio session");
#endif

    /* Leave this to JS side by sending event

    AVAudioSession* audioSession = [AVAudioSession sharedInstance];
    [audioSession setCategory:AVAudioSessionCategoryPlayAndRecord error:nil];
    
    [audioSession setMode:AVAudioSessionModeVoiceChat error:nil];
    
    double sampleRate = 44100.0;
    [audioSession setPreferredSampleRate:sampleRate error:nil];
    
    NSTimeInterval bufferDuration = .005;
    [audioSession setPreferredIOBufferDuration:bufferDuration error:nil];
    [audioSession setActive:TRUE error:nil];

    */

    [self sendEventWithName:RNCallKitConfigureAudioSession body:@{@"type": type}];
}

+ (BOOL)application:(UIApplication *)application
            openURL:(NSURL *)url
            options:(NSDictionary<UIApplicationOpenURLOptionsKey, id> *)options NS_AVAILABLE_IOS(9_0)
{
#ifdef DEBUG
    NSLog(@"[RNCallKit][application:openURL]");
#endif
    /*
    NSString *handle = [url startCallHandle];
    if (handle != nil && handle.length > 0 ){
        NSDictionary *userInfo = @{
            @"handle": handle,
            @"video": @NO
        };
        [[NSNotificationCenter defaultCenter] postNotificationName:RNCallKitHandleStartCallNotification
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
    NSLog(@"[RNCallKit][application:continueUserActivity]");
#endif
    INInteraction  *interaction = userActivity.interaction;
    INPerson *contact;
    NSString *handle;

    if ([userActivity.activityType isEqualToString:INStartAudioCallIntentIdentifier]) {
        INStartAudioCallIntent *startAudioCallIntent = (INStartAudioCallIntent *)interaction.intent;
        contact = [startAudioCallIntent.contacts firstObject];
    }

    if (contact != nil) {
        handle = contact.personHandle.value;
    }

    if (handle != nil && handle.length > 0 ){
        NSDictionary *userInfo = @{
            @"handle": handle,
            @"video": @NO
        };
        [[NSNotificationCenter defaultCenter] postNotificationName:RNCallKitHandleStartCallNotification
                                                            object:self
                                                          userInfo:userInfo];
        return YES;
    }
    return NO;
}

- (void)handleStartCallNotification:(NSNotification *)notification
{
#ifdef DEBUG
    NSLog(@"[RNCallKit][handleStartCallNotification] userInfo = %@", notification.userInfo);
#endif
    [self sendEventWithName:RNCallKitDidReceiveStartCallAction body:notification.userInfo];
}

#pragma mark - CXProviderDelegate

- (void)providerDidReset:(CXProvider *)provider{
#ifdef DEBUG
    NSLog(@"[RNCallKit][providerDidReset]");
#endif
}

// Starting outgoing call
- (void)provider:(CXProvider *)provider performStartCallAction:(CXStartCallAction *)action
{
#ifdef DEBUG
    NSLog(@"[RNCallKit][CXProviderDelegate][provider:performStartCallAction]");
#endif
    [self configureAudioSession:@"outgoingCall"];
    [action fulfill];
}

// Answering incoming call
- (void)provider:(CXProvider *)provider performAnswerCallAction:(CXAnswerCallAction *)action{
#ifdef DEBUG
    NSLog(@"[RNCallKit][CXProviderDelegate][provider:performAnswerCallAction]");
#endif
    [self sendEventWithName:RNCallKitPerformAnswerCallAction body:nil];
    [action fulfill];
}

// Ending incoming call
- (void)provider:(CXProvider *)provider performEndCallAction:(CXEndCallAction *)action{
#ifdef DEBUG
    NSLog(@"[RNCallKit][CXProviderDelegate][provider:performEndCallAction]");
#endif
    [self sendEventWithName:RNCallKitPerformEndCallAction body:nil];
    [action fulfill];
}

- (void)provider:(CXProvider *)provider performSetHeldCallAction:(CXSetHeldCallAction *)action{
#ifdef DEBUG
    NSLog(@"[RNCallKit][CXProviderDelegate][provider:performSetHeldCallAction]");
#endif
}

- (void)provider:(CXProvider *)provider timedOutPerformingAction:(CXAction *)action{
#ifdef DEBUG
    NSLog(@"[RNCallKit][CXProviderDelegate][provider:timedOutPerformingAction]");
#endif
}

- (void)provider:(CXProvider *)provider didActivateAudioSession:(AVAudioSession *)audioSession{
#ifdef DEBUG
    NSLog(@"[RNCallKit][CXProviderDelegate][provider:didActivateAudioSession]");
#endif
}
- (void)provider:(CXProvider *)provider didDeactivateAudioSession:(AVAudioSession *)audioSession{
#ifdef DEBUG
    NSLog(@"[RNCallKit][CXProviderDelegate][provider:didDeactivateAudioSession]");
#endif
}


@end
