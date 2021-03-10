//
//  RNCallKeep.h
//  RNCallKeep
//
//  Copyright 2016-2019 The CallKeep Authors (see the AUTHORS file)
//  SPDX-License-Identifier: ISC, MIT
//

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import <CallKit/CallKit.h>
#import <Intents/Intents.h>
//#import <AVFoundation/AVAudioSession.h>

#import <React/RCTEventEmitter.h>

@interface RNCallKeep: RCTEventEmitter <CXProviderDelegate>

@property (nonatomic, strong) CXCallController *callKeepCallController;
@property (nonatomic, strong) CXProvider *callKeepProvider;

extern NSString * const RNCallKeepHandleStartCallNotification;
extern NSString * const RNCallKeepDidReceiveStartCallAction;
extern NSString * const RNCallKeepPerformAnswerCallAction;
extern NSString * const RNCallKeepPerformEndCallAction;
extern NSString * const RNCallKeepDidActivateAudioSession;
extern NSString * const RNCallKeepDidDeactivateAudioSession;
extern NSString * const RNCallKeepDidDisplayIncomingCall;
extern NSString * const RNCallKeepDidPerformSetMutedCallAction;
extern NSString * const RNCallKeepPerformPlayDTMFCallAction;
extern NSString * const RNCallKeepDidToggleHoldAction;
extern NSString * const RNCallKeepProviderReset;
extern NSString * const RNCallKeepCheckReachability;
extern NSString * const RNCallKeepDidLoadWithEvents;

+(BOOL) application: (UIApplication *) application
            openURL: (NSURL *) url
            options: (NSDictionary<UIApplicationOpenURLOptionsKey, id> *) options NS_AVAILABLE_IOS(9_0);

+(BOOL) application: (UIApplication *) application
continueUserActivity: (NSUserActivity *) userActivity
 restorationHandler: (void(^) (NSArray * __nullable restorableObjects)) restorationHandler;

+(void) reportNewIncomingCall: (NSString *) uuidString
                       handle: (NSString *) handle
                   handleType: (NSString *) handleType
                     hasVideo: (BOOL) hasVideo
          localizedCallerName: (NSString * _Nullable) localizedCallerName
              supportsHolding: (BOOL) supportsHolding
                 supportsDTMF: (BOOL) supportsDTMF
             supportsGrouping: (BOOL) supportsGrouping
           supportsUngrouping: (BOOL) supportsUngrouping
                  fromPushKit: (BOOL) fromPushKit
                      payload: (NSDictionary * _Nullable) payload
        withCompletionHandler: (void (^_Nullable)(void)) completion;

+(void) endCallWithUUID: (NSString *) uuidString
                 reason: (int) reason;

+(BOOL) isCallActive: (NSString *) uuidString;

-(void) initCallKitProvider: (NSDictionary *) settings withEventHandler: (void (^) (NSString * eventName, id data)) onEvent;

@end
