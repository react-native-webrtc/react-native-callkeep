package io.wazo.callkeep

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.BridgeReactContext
import com.facebook.react.bridge.WritableMap
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A call's lifetime and the `foregroundService.foregroundServiceType` and `endCallsWhenReactNativeStops`
 * options, through Telecom.
 * Same host setup as CallKeepForegroundServiceTest. The host manifest must declare
 * `phoneCall|microphone` on VoiceConnectionService: with `phoneCall` alone, the service starts with
 * `phoneCall` without the option too, and the type test would prove nothing, so it asserts this.
 * Android 8+, and 10+ for the service type.
 */
@RunWith(AndroidJUnit4::class)
@Suppress("DEPRECATION")
class CallKeepCallLifetimeOptionsTest {
  private val instrumentation = InstrumentationRegistry.getInstrumentation()
  private val context: Context get() = instrumentation.targetContext

  private fun isForeground(): Boolean =
    context.getSystemService(ActivityManager::class.java).getRunningServices(Int.MAX_VALUE)
      .any { it.service.className == VoiceConnectionService::class.java.name && it.foreground }

  // React Native calls module methods on its native-modules thread, which has a looper.
  private val moduleThread = HandlerThread("mqt_native_modules").apply { start() }

  private fun onModuleThread(work: () -> Unit) {
    val done = CountDownLatch(1)
    Handler(moduleThread.looper).post { work(); done.countDown() }
    assertTrue("module call did not return", done.await(3, TimeUnit.SECONDS))
  }

  private fun waitFor(condition: () -> Boolean): Boolean {
    val deadline = System.currentTimeMillis() + 5000
    while (System.currentTimeMillis() < deadline) {
      if (condition()) return true
      Thread.sleep(100)
    }
    return condition()
  }

  private fun selfManagedOptions(foregroundServiceType: Int? = null, endCallsWhenReactNativeStops: Boolean? = null): WritableMap =
    Arguments.createMap().apply {
      putBoolean("selfManaged", true)
      endCallsWhenReactNativeStops?.let { putBoolean("endCallsWhenReactNativeStops", it) }
      putMap("foregroundService", Arguments.createMap().apply {
        putString("channelId", "io.wazo.callkeep.test")
        putString("channelName", "CallKeep test")
        putString("notificationTitle", "Call in progress")
        foregroundServiceType?.let { putInt("foregroundServiceType", it) }
      })
    }

  /** A call can ring with the `phoneCall` type alone, although the manifest also declares `microphone`. */
  @Test fun theForegroundServiceStartsWithTheGivenType() {
    assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
    val bothTypes = ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
    val declared = context.packageManager
      .getServiceInfo(ComponentName(context, VoiceConnectionService::class.java), 0).foregroundServiceType
    assertEquals("the host must declare phoneCall|microphone on VoiceConnectionService", bothTypes, declared and bothTypes)
    val module = RNCallKeepModule.getInstance(BridgeReactContext(context), true)
    onModuleThread {
      module.setup(selfManagedOptions(foregroundServiceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL))
    }
    val uuid = UUID.randomUUID().toString()
    try {
      onModuleThread { module.displayIncomingCall(uuid, "5551005", "Phone call type", false, null) }
      assertTrue("call not created", waitFor { VoiceConnectionService.currentConnections.containsKey(uuid) })
      assertTrue("no foreground service", waitFor { isForeground() })
      assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL,
        VoiceConnectionService.currentConnectionService!!.foregroundServiceType)
    } finally {
      onModuleThread { module.endCall(uuid) }
      waitFor { !isForeground() }
      moduleThread.quitSafely()
    }
  }

  /** A call that rings, then React Native's lifecycle: [react] runs on the module, [check] after. */
  private fun withACall(options: WritableMap, react: (RNCallKeepModule) -> Unit, check: (String) -> Unit) {
    assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
    val module = RNCallKeepModule.getInstance(BridgeReactContext(context), true)
    onModuleThread { module.setup(options) }
    val uuid = UUID.randomUUID().toString()
    try {
      onModuleThread { module.displayIncomingCall(uuid, "5551006", "Lifetime", false, null) }
      assertTrue("call not created", waitFor { VoiceConnectionService.currentConnections.containsKey(uuid) })
      react(module)
      check(uuid)
    } finally {
      onModuleThread { module.endCall(uuid) }
      waitFor { !isForeground() }
      moduleThread.quitSafely()
    }
  }

  /**
   * The activity is destroyed (a configuration change, Back, the app swiped away from Recents): the
   * call goes on, and so does the process. Before, CallKeep ended the calls and killed the process,
   * which the instrumentation reports as a crash.
   */
  @Test fun theCallOutlivesTheActivity() = withACall(selfManagedOptions(), { module ->
    // React Native calls this on the main thread.
    instrumentation.runOnMainSync { module.onHostDestroy() }
    Thread.sleep(500)
  }) { uuid ->
    assertTrue("the call ended with the activity", VoiceConnectionService.currentConnections.containsKey(uuid))
  }

  /** React Native's instance stops (a reload): the JavaScript that handles the call is gone, so it ends. */
  @Test fun theCallEndsWhenReactNativeStops() = withACall(selfManagedOptions(), { module ->
    // React Native invalidates the modules off the main thread.
    onModuleThread { module.invalidate() }
  }) { uuid ->
    assertTrue("the call outlived React Native", waitFor { !VoiceConnectionService.currentConnections.containsKey(uuid) })
  }

  /** With `endCallsWhenReactNativeStops: false`, native code owns the calls: they outlive React Native. */
  @Test fun nativeCallsOutliveReactNative() = withACall(selfManagedOptions(endCallsWhenReactNativeStops = false), { module ->
    onModuleThread { module.invalidate() }
    Thread.sleep(500)
  }) { uuid ->
    assertTrue("the call ended with React Native", VoiceConnectionService.currentConnections.containsKey(uuid))
  }
}
