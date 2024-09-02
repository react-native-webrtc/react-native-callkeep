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
#import <React/RCTLog.h>

#import <AVFoundation/AVAudioSession.h>
#import <CallKit/CallKit.h>

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
static NSString *const RNCallKeepDidChangeAudioRoute = @"RNCallKeepDidChangeAudioRoute";
static NSString *const RNCallKeepDidLoadWithEvents = @"RNCallKeepDidLoadWithEvents";

@implementation RNCallKeep
{
    NSOperatingSystemVersion _version;
    BOOL _isStartCallActionEventListenerAdded;
    bool _hasListeners;
    bool _isReachable;
    NSMutableArray *_delayedEvents;
}

static bool isSetupNatively;
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
        _isReachable = NO;
        if (_delayedEvents == nil) _delayedEvents = [NSMutableArray array];

        [[NSNotificationCenter defaultCenter] addObserver:self
                                                 selector:@selector(onAudioRouteChange:)
                                                     name:AVAudioSessionRouteChangeNotification
                                                   object:nil];
        // Init provider directly, in case of an app killed and when we've already stored our settings
        [RNCallKeep initCallKitProvider];

        self.callKeepProvider = sharedProvider;
        [self.callKeepProvider setDelegate:self queue:nil];
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
    _isReachable = NO;
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
        RNCallKeepCheckReachability,
        RNCallKeepDidLoadWithEvents,
        RNCallKeepDidChangeAudioRoute
    ];
}

- (void)startObserving
{
    NSLog(@"[RNCallKeep][startObserving]");
    _hasListeners = YES;
    if ([_delayedEvents count] > 0) {
        [self sendEventWithName:RNCallKeepDidLoadWithEvents body:_delayedEvents];
    }
}

- (void)stopObserving
{
    _hasListeners = FALSE;

    // Fix for https://github.com/react-native-webrtc/react-native-callkeep/issues/406
    // We use Objective-C Key Value Coding(KVC) to sync _RTCEventEmitter_ `_listenerCount`.
    @try {
        [self setValue:@0 forKey:@"_listenerCount"];
    }
    @catch ( NSException *e ){
        NSLog(@"[RNCallKeep][stopObserving] exception: %@",e);
        NSLog(@"[RNCallKeep][stopObserving] RNCallKeep parent class RTCEventEmitter might have a broken state.");
        NSLog(@"[RNCallKeep][stopObserving] Please verify that the parent RTCEventEmitter.m has iVar `_listenerCount`.");
    }
}

- (void)onAudioRouteChange:(NSNotification *)notification
{
    NSDictionary *info = notification.userInfo;
    NSInteger reason = [[info valueForKey:AVAudioSessionRouteChangeReasonKey] integerValue];
    NSString *output = [RNCallKeep getAudioOutput];

    if (output == nil) {
        return;
    }

    [self sendEventWithName:RNCallKeepDidChangeAudioRoute body:@{
        @"output": output,
        @"reason": @(reason),
    }];
}

- (void)sendEventWithNameWrapper:(NSString *)name body:(id)body {
    NSLog(@"[RNCallKeep] sendEventWithNameWrapper: %@, hasListeners : %@", name, _hasListeners ? @"YES": @"NO");

    if (_hasListeners) {
        [self sendEventWithName:name body:body];
    } else {
        NSDictionary *dictionary = [NSDictionary dictionaryWithObjectsAndKeys:
            name, @"name",
            body, @"data",
            nil
        ];
        [_delayedEvents addObject:dictionary];
    }
}

+ (NSDictionary *) getSettings {
    return [[NSUserDefaults standardUserDefaults] dictionaryForKey:@"RNCallKeepSettings"];
}

+ (void)initCallKitProvider {
    if (sharedProvider == nil) {
        NSDictionary *settings = [self getSettings];
        if (settings != nil) {
            sharedProvider = [[CXProvider alloc] initWithConfiguration:[RNCallKeep getProviderConfiguration:settings]];
        }
    }
}

+ (NSString *) getAudioOutput {
    @try{
        NSArray<AVAudioSessionPortDescription *>* outputs = [AVAudioSession sharedInstance].currentRoute.outputs;
        if(outputs != nil && outputs.count > 0){
            return outputs[0].portType;
        }
    } @catch(NSException* error) {
        NSLog(@"getAudioOutput error :%@", [error description]);
    }

    return nil;
}

+ (void)setup:(NSDictionary *)options {
    RNCallKeep *callKeep = [RNCallKeep allocWithZone: nil];
    [callKeep setup:options];
    isSetupNatively = YES;
}

RCT_EXPORT_METHOD(setup:(NSDictionary *)options)
{
    if (isSetupNatively) {
#ifdef DEBUG
        NSLog(@"[RNCallKeep][setup] already setup");
        RCTLog(@"[RNCallKeep][setup] already setup in native code");
#endif
        return;
    }

#ifdef DEBUG
    NSLog(@"[RNCallKeep][setup] options = %@", options);
#endif
    _version = [[[NSProcessInfo alloc] init] operatingSystemVersion];
    self.callKeepCallController = [[CXCallController alloc] init];

    [self setSettings: options];

    [RNCallKeep initCallKitProvider];

    self.callKeepProvider = sharedProvider;
    [self.callKeepProvider setDelegate:self queue:nil];
}

RCT_EXPORT_METHOD(setSettings:(NSDictionary *)options)
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][setSettings] options = %@", options);
#endif
    NSDictionary *settings = [[NSMutableDictionary alloc] initWithDictionary:options];

    // Store settings in NSUserDefault
    [[NSUserDefaults standardUserDefaults] setObject:settings forKey:@"RNCallKeepSettings"];
    [[NSUserDefaults standardUserDefaults] synchronize];
}

RCT_EXPORT_METHOD(setReachable)
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][setReachable]");
#endif
    _isReachable = YES;
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
    NSString *output = [RNCallKeep getAudioOutput];
    resolve(@([output isEqualToString:@"Speaker"]));
}

#pragma mark - CXCallController call actions

// Display the incoming call to the user
RCT_EXPORT_METHOD(displayIncomingCall:(NSString *)uuidString
                  handle:(NSString *)handle
                  handleType:(NSString *)handleType
                  hasVideo:(BOOL)hasVideo
                  localizedCallerName:(NSString * _Nullable)localizedCallerName
                  supportsHolding:(BOOL)supportsHolding
                  supportsDTMF:(BOOL)supportsDTMF
                  supportsGrouping:(BOOL)supportsGrouping
                  supportsUngrouping:(BOOL)supportsUngrouping)
{
    [RNCallKeep reportNewIncomingCall: uuidString
                               handle: handle
                           handleType: handleType
                             hasVideo: hasVideo
                  localizedCallerName: localizedCallerName
                      supportsHolding: supportsHolding
                         supportsDTMF: supportsDTMF
                     supportsGrouping: supportsGrouping
                   supportsUngrouping: supportsUngrouping
                          fromPushKit: NO
                              payload: nil
                withCompletionHandler: nil];

    NSDictionary *settings = [RNCallKeep getSettings];
    NSNumber *timeout = settings[@"displayCallReachabilityTimeout"];

    if (timeout) {
        dispatch_time_t popTime = dispatch_time(DISPATCH_TIME_NOW, (int64_t)([timeout intValue] * NSEC_PER_MSEC));
        dispatch_after(popTime, dispatch_get_main_queue(), ^(void){
            if (!self->_isReachable) {
#ifdef DEBUG
                NSLog(@"[RNCallKeep]Displayed a call without a reachable app, ending the call: %@", uuidString);
#endif
                [RNCallKeep endCallWithUUID: uuidString reason: 1];
            }
        });
    }
}

RCT_EXPORT_METHOD(getInitialEvents:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][getInitialEvents]");
#endif
    resolve(_delayedEvents);
}

RCT_EXPORT_METHOD(clearInitialEvents)
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][clearInitialEvents]");
#endif
    _delayedEvents = [NSMutableArray array];
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

RCT_EXPORT_METHOD(answerIncomingCall:(NSString *)uuidString)
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][answerIncomingCall] uuidString = %@", uuidString);
#endif
    NSUUID *uuid = [[NSUUID alloc] initWithUUIDString:uuidString];
    CXAnswerCallAction *answerCallAction = [[CXAnswerCallAction alloc] initWithCallUUID:uuid];
    CXTransaction *transaction = [[CXTransaction alloc] init];
    [transaction addAction:answerCallAction];

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
    [RNCallKeep endCallWithUUID: uuidString reason:reason];
}

RCT_EXPORT_METHOD(updateDisplay:(NSString *)uuidString :(NSString *)displayName :(NSString *)uri :(NSDictionary *)options)
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][updateDisplay] uuidString = %@ displayName = %@ uri = %@", uuidString, displayName, uri);
#endif
    NSUUID *uuid = [[NSUUID alloc] initWithUUIDString:uuidString];
    CXHandle *callHandle = [[CXHandle alloc] initWithType:CXHandleTypePhoneNumber value:uri];
    CXCallUpdate *callUpdate = [[CXCallUpdate alloc] init];
    callUpdate.localizedCallerName = displayName;
    callUpdate.remoteHandle = callHandle;

    if ([options valueForKey:@"hasVideo"] != nil) {
        callUpdate.hasVideo = [RCTConvert BOOL:options[@"hasVideo"]];
    }
    if ([options valueForKey:@"supportsHolding"] != nil) {
        callUpdate.supportsHolding = [RCTConvert BOOL:options[@"supportsHolding"]];
    }
    if ([options valueForKey:@"supportsDTMF"] != nil) {
        callUpdate.supportsDTMF = [RCTConvert BOOL:options[@"supportsDTMF"]];
    }
    if ([options valueForKey:@"supportsGrouping"] != nil) {
        callUpdate.supportsGrouping = [RCTConvert BOOL:options[@"supportsGrouping"]];
    }
    if ([options valueForKey:@"supportsUngrouping"] != nil) {
        callUpdate.supportsUngrouping = [RCTConvert BOOL:options[@"supportsUngrouping"]];
    }

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

RCT_EXPORT_METHOD(isCallActive:(NSString *)uuidString
                  isCallActiveResolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][isCallActive] uuid = %@", uuidString);
#endif
    BOOL isActive = [RNCallKeep isCallActive: uuidString];
    if (isActive) {
        resolve(@YES);
    } else {
        resolve(@NO);
    }
}

RCT_EXPORT_METHOD(getCalls:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][getCalls]");
#endif
    resolve([RNCallKeep getCalls]);
}

RCT_EXPORT_METHOD(setAudioRoute: (NSString *)uuid
                  inputName:(NSString *)inputName
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][setAudioRoute] - inputName: %@", inputName);
#endif
    @try {
        NSError* err = nil;
        AVAudioSession* myAudioSession = [AVAudioSession sharedInstance];
        if ([inputName isEqualToString:@"Speaker"]) {
            BOOL isOverrided = [myAudioSession overrideOutputAudioPort:AVAudioSessionPortOverrideSpeaker error:&err];
            if(!isOverrided){
                [NSException raise:@"overrideOutputAudioPort failed" format:@"error: %@", err];
            }
            resolve(@"Speaker");
            return;
        }

        NSArray *ports = [RNCallKeep getAudioInputs];
        for (AVAudioSessionPortDescription *port in ports) {
            if ([port.portName isEqualToString:inputName]) {
                BOOL isSetted = [myAudioSession setPreferredInput:(AVAudioSessionPortDescription *)port error:&err];
                if(!isSetted){
                    [NSException raise:@"setPreferredInput failed" format:@"error: %@", err];
                }
                resolve(inputName);
                return;
            }
        }
    }
    @catch ( NSException *e ){
        NSLog(@"[RNCallKeep][setAudioRoute] exception: %@",e);
        reject(@"Failure to set audio route", e, nil);
    }
}

RCT_EXPORT_METHOD(getAudioRoutes: (RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][getAudioRoutes]");
#endif
    @try {
        NSArray *inputs = [RNCallKeep getAudioInputs];
        NSMutableArray *formatedInputs = [RNCallKeep formatAudioInputs: inputs];
        resolve(formatedInputs);
    }
    @catch ( NSException *e ) {
        NSLog(@"[RNCallKeep][getAudioRoutes] exception: %@",e);
        reject(@"Failure to get audio routes", e, nil);
    }
}

+ (NSMutableArray *) formatAudioInputs: (NSMutableArray *)inputs
{
    NSMutableArray *newInputs = [NSMutableArray new];
    NSString * selected = [RNCallKeep getSelectedAudioRoute];

    NSMutableDictionary *speakerDict = [[NSMutableDictionary alloc]init];
    [speakerDict setObject:@"Speaker" forKey:@"name"];
    [speakerDict setObject:AVAudioSessionPortBuiltInSpeaker forKey:@"type"];
    if(selected && [selected isEqualToString:AVAudioSessionPortBuiltInSpeaker]){
        [speakerDict setObject:@YES forKey:@"selected"];
    }
    [newInputs addObject:speakerDict];

    for (AVAudioSessionPortDescription* input in inputs)
    {
        NSString *str = [NSString stringWithFormat:@"PORTS :\"%@\": UID:%@", input.portName, input.UID ];
        NSMutableDictionary *dict = [[NSMutableDictionary alloc]init];
        [dict setObject:input.portName forKey:@"name"];
        NSString * type = [RNCallKeep getAudioInputType: input.portType];
        if(type)
        {
            if([selected isEqualToString:type]){
                [dict setObject:@YES forKey:@"selected"];
            }
            [dict setObject:type forKey:@"type"];
            [newInputs addObject:dict];
        }
    }
    return newInputs;
}

+ (NSArray *) getAudioInputs
{
    NSError* err = nil;
    NSString *str = nil;

    AVAudioSession* myAudioSession = [AVAudioSession sharedInstance];
    NSString *category = [myAudioSession category];
    NSUInteger options = [myAudioSession categoryOptions];


    if(![category isEqualToString:AVAudioSessionCategoryPlayAndRecord] && (options != AVAudioSessionCategoryOptionAllowBluetooth) && (options !=AVAudioSessionCategoryOptionAllowBluetoothA2DP))
    {
        BOOL isCategorySetted = [myAudioSession setCategory:AVAudioSessionCategoryPlayAndRecord withOptions:AVAudioSessionCategoryOptionAllowBluetooth error:&err];
        if (!isCategorySetted)
        {
            NSLog(@"setCategory failed");
            [NSException raise:@"setCategory failed" format:@"error: %@", err];
        }
    }

    BOOL isCategoryActivated = [myAudioSession setActive:YES error:&err];
    if (!isCategoryActivated)
    {
        NSLog(@"[RNCallKeep][getAudioInputs] setActive failed");
        [NSException raise:@"setActive failed" format:@"error: %@", err];
    }

    NSArray *inputs = [myAudioSession availableInputs];
    return inputs;
}

+ (NSString *) getAudioInputType: (NSString *) type
{
    if ([type isEqualToString:AVAudioSessionPortBuiltInMic]){
        return @"Phone";
    }
    else if ([type isEqualToString:AVAudioSessionPortHeadsetMic]){
        return @"Headset";
    }
    else if ([type isEqualToString:AVAudioSessionPortHeadphones]){
        return @"Headset";
    }
    else if ([type isEqualToString:AVAudioSessionPortBluetoothHFP]){
        return @"Bluetooth";
    }
    else if ([type isEqualToString:AVAudioSessionPortBluetoothA2DP]){
        return @"Bluetooth";
    }
    else if ([type isEqualToString:AVAudioSessionPortBuiltInSpeaker]){
        return @"Speaker";
    }
    else if ([type isEqualToString:AVAudioSessionPortCarAudio]) {
        return @"CarAudio";
    }
    else{
        return nil;
    }
}

+ (NSString *) getSelectedAudioRoute
{
    AVAudioSession* myAudioSession = [AVAudioSession sharedInstance];
    AVAudioSessionRouteDescription *currentRoute = [myAudioSession currentRoute];
    NSArray *selectedOutputs = currentRoute.outputs;

    AVAudioSessionPortDescription *selectedOutput = selectedOutputs[0];

    if(selectedOutput && [selectedOutput.portType isEqualToString:AVAudioSessionPortBuiltInReceiver]) {
        return @"Phone";
    }

    return [RNCallKeep getAudioInputType: selectedOutput.portType];
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

+ (BOOL)isCallActive:(NSString *)uuidString
{
    CXCallObserver *callObserver = [[CXCallObserver alloc] init];
    NSUUID *uuid = [[NSUUID alloc] initWithUUIDString:uuidString];

    for(CXCall *call in callObserver.calls){
        NSLog(@"[RNCallKeep] isCallActive %@ %d ?", call.UUID, [call.UUID isEqual:uuid]);
        if([call.UUID isEqual:[[NSUUID alloc] initWithUUIDString:uuidString]]){
            return call.hasConnected;
        }
    }
    return false;
}

+ (NSMutableArray *) getCalls
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][getCalls]");
#endif
    CXCallObserver *callObserver = [[CXCallObserver alloc] init];
    NSMutableArray *currentCalls = [NSMutableArray array];
    for(CXCall *call in callObserver.calls){
        NSString *uuidString = [call.UUID UUIDString];
        NSDictionary *requestedCall= @{
            @"callUUID": uuidString,
            @"outgoing": call.outgoing? @YES : @NO,
            @"onHold": call.onHold? @YES : @NO,
            @"hasConnected": call.hasConnected ? @YES : @NO,
            @"hasEnded": call.hasEnded ? @YES : @NO
        };
        [currentCalls addObject:requestedCall];
    }
    return currentCalls;
}

+ (void)endCallWithUUID:(NSString *)uuidString
                 reason:(int)reason
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][reportEndCallWithUUID] uuidString = %@ reason = %d", uuidString, reason);
#endif
    NSUUID *uuid = [[NSUUID alloc] initWithUUIDString:uuidString];
    switch (reason) {
        case 1:
            [sharedProvider reportCallWithUUID:uuid endedAtDate:[NSDate date] reason:CXCallEndedReasonFailed];
            break;
        case 2:
        case 6:
            [sharedProvider reportCallWithUUID:uuid endedAtDate:[NSDate date] reason:CXCallEndedReasonRemoteEnded];
            break;
        case 3:
            [sharedProvider reportCallWithUUID:uuid endedAtDate:[NSDate date] reason:CXCallEndedReasonUnanswered];
            break;
        case 4:
            [sharedProvider reportCallWithUUID:uuid endedAtDate:[NSDate date] reason:CXCallEndedReasonAnsweredElsewhere];
            break;
        case 5:
            [sharedProvider reportCallWithUUID:uuid endedAtDate:[NSDate date] reason:CXCallEndedReasonDeclinedElsewhere];
            break;
        default:
            break;
    }
}

+ (void)reportNewIncomingCall:(NSString *)uuidString
                       handle:(NSString *)handle
                   handleType:(NSString *)handleType
                     hasVideo:(BOOL)hasVideo
          localizedCallerName:(NSString * _Nullable)localizedCallerName
              supportsHolding:(BOOL)supportsHolding
                 supportsDTMF:(BOOL)supportsDTMF
             supportsGrouping:(BOOL)supportsGrouping
           supportsUngrouping:(BOOL)supportsUngrouping
                  fromPushKit:(BOOL)fromPushKit
                      payload:(NSDictionary * _Nullable)payload
        withCompletionHandler:(void (^_Nullable)(void))completion
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][reportNewIncomingCall] uuidString = %@", uuidString);
#endif
    int _handleType = [RNCallKeep getHandleType:handleType];
    NSUUID *uuid = [[NSUUID alloc] initWithUUIDString:uuidString];
    CXCallUpdate *callUpdate = [[CXCallUpdate alloc] init];
    callUpdate.remoteHandle = [[CXHandle alloc] initWithType:_handleType value:handle];
    callUpdate.supportsHolding = supportsHolding;
    callUpdate.supportsDTMF = supportsDTMF;
    callUpdate.supportsGrouping = supportsGrouping;
    callUpdate.supportsUngrouping = supportsUngrouping;
    callUpdate.hasVideo = hasVideo;
    callUpdate.localizedCallerName = localizedCallerName;

    [RNCallKeep initCallKitProvider];
    [sharedProvider reportNewIncomingCallWithUUID:uuid update:callUpdate completion:^(NSError * _Nullable error) {
        RNCallKeep *callKeep = [RNCallKeep allocWithZone: nil];
        [callKeep sendEventWithNameWrapper:RNCallKeepDidDisplayIncomingCall body:@{
            @"error": error && error.localizedDescription ? error.localizedDescription : @"",
            @"errorCode": error ? [callKeep getIncomingCallErrorCode:error] : @"",
            @"callUUID": uuidString,
            @"handle": handle,
            @"localizedCallerName": localizedCallerName ? localizedCallerName : @"",
            @"hasVideo": hasVideo ? @"1" : @"0",
            @"supportsHolding": supportsHolding ? @"1" : @"0",
            @"supportsDTMF": supportsDTMF ? @"1" : @"0",
            @"supportsGrouping": supportsGrouping ? @"1" : @"0",
            @"supportsUngrouping": supportsUngrouping ? @"1" : @"0",
            @"fromPushKit": fromPushKit ? @"1" : @"0",
            @"payload": payload ? payload : @"",
        }];
        if (error == nil) {
            // Workaround per https://forums.developer.apple.com/message/169511
            if ([callKeep lessThanIos10_2]) {
                [callKeep configureAudioSession];
            }
        }
        if (completion != nil) {
            completion();
        }
    }];
}

- (NSString *)getIncomingCallErrorCode:(NSError *)error {
    if ([error code] == CXErrorCodeIncomingCallErrorUnentitled) {
        return @"Unentitled";
    } else if ([error code] == CXErrorCodeIncomingCallErrorCallUUIDAlreadyExists) {
        return @"CallUUIDAlreadyExists";
    } else if ([error code] == CXErrorCodeIncomingCallErrorFilteredByDoNotDisturb) {
        return @"FilteredByDoNotDisturb";
    } else if ([error code] == CXErrorCodeIncomingCallErrorFilteredByBlockList) {
        return @"FilteredByBlockList";
    } else {
        return @"Unknown";
    }
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

+ (NSSet *) getSupportedHandleTypes:(id) handleType {
    if(handleType){
        if([handleType isKindOfClass:[NSArray class]]) {
            NSSet *types = [NSSet set];

            for (NSString* type in handleType) {
                types = [types setByAddingObject:[NSNumber numberWithInteger:[RNCallKeep getHandleType:type]]];
            }

            return types;
        } else {
            int _handleType = [RNCallKeep getHandleType:handleType];

            return [NSSet setWithObjects:[NSNumber numberWithInteger:_handleType], nil];
        }
    } else {
        return [NSSet setWithObjects:[NSNumber numberWithInteger:CXHandleTypePhoneNumber], nil];
    }
}

+ (int)getHandleType:(NSString *)handleType
{
    if ([handleType isEqualToString:@"generic"]) {
        return CXHandleTypeGeneric;
    } else if ([handleType isEqualToString:@"number"]) {
        return CXHandleTypePhoneNumber;
    } else if ([handleType isEqualToString:@"phone"]) {
        return CXHandleTypePhoneNumber;
    } else if ([handleType isEqualToString:@"email"]) {
        return CXHandleTypeEmailAddress;
    } else {
        return CXHandleTypeGeneric;
    }
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
    providerConfiguration.supportedHandleTypes = [RNCallKeep getSupportedHandleTypes:settings[@"handleType"]];

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
    if (@available(iOS 11.0, *)) {
        if (settings[@"includesCallsInRecents"]) {
            providerConfiguration.includesCallsInRecents = [settings[@"includesCallsInRecents"] boolValue];
        }
    }
    return providerConfiguration;
}

- (void)configureAudioSession
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][configureAudioSession] Activating audio session");
#endif

    NSUInteger categoryOptions = AVAudioSessionCategoryOptionAllowBluetooth | AVAudioSessionCategoryOptionAllowBluetoothA2DP;
    NSString *mode = AVAudioSessionModeDefault;

    NSDictionary *settings = [RNCallKeep getSettings];
    if (settings && settings[@"audioSession"]) {
        if (settings[@"audioSession"][@"categoryOptions"]) {
            categoryOptions = [settings[@"audioSession"][@"categoryOptions"] integerValue];
        }

        if (settings[@"audioSession"][@"mode"]) {
            mode = settings[@"audioSession"][@"mode"];
        }
    }

    AVAudioSession* audioSession = [AVAudioSession sharedInstance];
    [audioSession setCategory:AVAudioSessionCategoryPlayAndRecord withOptions:categoryOptions error:nil];

    [audioSession setMode:mode error:nil];

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

    // HACK TO AVOID XCODE 10 COMPILE CRASH
    // REMOVE ON NEXT MAJOR RELEASE OF RNCALLKIT
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 130000
    //XCode 11
    // iOS 13 returns an INStartCallIntent userActivity type
    if (@available(iOS 13, *)) {
        INStartCallIntent *intent = (INStartCallIntent*)interaction.intent;
        // callCapability is not available on iOS > 13.2, but it is in 13.1 weirdly...
        if ([intent respondsToSelector:@selector(callCapability)]) {
            isAudioCall = intent.callCapability == INCallCapabilityAudioCall;
            isVideoCall = intent.callCapability == INCallCapabilityVideoCall;
        } else {
            isAudioCall = [userActivity.activityType isEqualToString:INStartAudioCallIntentIdentifier];
            isVideoCall = [userActivity.activityType isEqualToString:INStartVideoCallIntentIdentifier];
        }
    } else {
#endif
        // XCode 10 and below
        isAudioCall = [userActivity.activityType isEqualToString:INStartAudioCallIntentIdentifier];
        isVideoCall = [userActivity.activityType isEqualToString:INStartVideoCallIntentIdentifier];
        // HACK TO AVOID XCODE 10 COMPILE CRASH
        // REMOVE ON NEXT MAJOR RELEASE OF RNCALLKIT
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
        [callKeep sendEventWithNameWrapper:RNCallKeepDidReceiveStartCallAction body:userInfo];
        return YES;
    }
    return NO;
}

+ (BOOL)requiresMainQueueSetup
{
    return YES;
}

#pragma mark - CXProviderDelegate

- (void)providerDidReset:(CXProvider *)provider{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][providerDidReset]");
#endif
    //this means something big changed, so tell the JS. The JS should
    //probably respond by hanging up all calls.
    [self sendEventWithNameWrapper:RNCallKeepProviderReset body:nil];
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
    [self sendEventWithNameWrapper:RNCallKeepDidReceiveStartCallAction body:@{ @"callUUID": [action.callUUID.UUIDString lowercaseString], @"handle": action.handle.value }];
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
    [self sendEventWithNameWrapper:RNCallKeepPerformAnswerCallAction body:@{ @"callUUID": [action.callUUID.UUIDString lowercaseString] }];
    [action fulfill];
}

// Ending incoming call
- (void)provider:(CXProvider *)provider performEndCallAction:(CXEndCallAction *)action
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][CXProviderDelegate][provider:performEndCallAction]");
#endif
    [self sendEventWithNameWrapper:RNCallKeepPerformEndCallAction body:@{ @"callUUID": [action.callUUID.UUIDString lowercaseString] }];
    [action fulfill];
}

-(void)provider:(CXProvider *)provider performSetHeldCallAction:(CXSetHeldCallAction *)action
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][CXProviderDelegate][provider:performSetHeldCallAction]");
#endif

    [self sendEventWithNameWrapper:RNCallKeepDidToggleHoldAction body:@{ @"hold": @(action.onHold), @"callUUID": [action.callUUID.UUIDString lowercaseString] }];
    [action fulfill];
}

- (void)provider:(CXProvider *)provider performPlayDTMFCallAction:(CXPlayDTMFCallAction *)action {
#ifdef DEBUG
    NSLog(@"[RNCallKeep][CXProviderDelegate][provider:performPlayDTMFCallAction]");
#endif
    [self sendEventWithNameWrapper:RNCallKeepPerformPlayDTMFCallAction body:@{ @"digits": action.digits, @"callUUID": [action.callUUID.UUIDString lowercaseString] }];
    [action fulfill];
}

-(void)provider:(CXProvider *)provider performSetMutedCallAction:(CXSetMutedCallAction *)action
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][CXProviderDelegate][provider:performSetMutedCallAction]");
#endif

    [self sendEventWithNameWrapper:RNCallKeepDidPerformSetMutedCallAction body:@{ @"muted": @(action.muted), @"callUUID": [action.callUUID.UUIDString lowercaseString] }];
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
    [self sendEventWithNameWrapper:RNCallKeepDidActivateAudioSession body:nil];
}

- (void)provider:(CXProvider *)provider didDeactivateAudioSession:(AVAudioSession *)audioSession
{
#ifdef DEBUG
    NSLog(@"[RNCallKeep][CXProviderDelegate][provider:didDeactivateAudioSession]");
#endif
    [self sendEventWithNameWrapper:RNCallKeepDidDeactivateAudioSession body:nil];
}

@end
