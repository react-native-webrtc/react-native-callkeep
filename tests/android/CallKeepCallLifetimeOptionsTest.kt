package io.wazo.callkeep

import android.app.ActivityManager
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
 * The `foregroundService.foregroundServiceType` and `endCallsOnHostDestroy` options, through Telecom.
 * Same host setup as CallKeepForegroundServiceTest; the host manifest declares the `phoneCall` type
 * on VoiceConnectionService. Android 8+, and 10+ for the service type.
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

  private fun selfManagedOptions(foregroundServiceType: Int? = null, endCallsOnHostDestroy: Boolean? = null): WritableMap =
    Arguments.createMap().apply {
      putBoolean("selfManaged", true)
      endCallsOnHostDestroy?.let { putBoolean("endCallsOnHostDestroy", it) }
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

  /** With `endCallsOnHostDestroy: false`, destroying the activity leaves the call and the process. */
  @Test fun theCallOutlivesTheActivityWhenAsked() {
    assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
    val module = RNCallKeepModule.getInstance(BridgeReactContext(context), true)
    onModuleThread { module.setup(selfManagedOptions(endCallsOnHostDestroy = false)) }
    val uuid = UUID.randomUUID().toString()
    try {
      onModuleThread { module.displayIncomingCall(uuid, "5551006", "Outlives the activity", false, null) }
      assertTrue("call not created", waitFor { VoiceConnectionService.currentConnections.containsKey(uuid) })
      // React Native calls this on the main thread. Without the option, it ends the calls and kills
      // the process, which the instrumentation reports as a crash.
      instrumentation.runOnMainSync { module.onHostDestroy() }
      Thread.sleep(500)
      assertTrue("the call ended with the activity", VoiceConnectionService.currentConnections.containsKey(uuid))
    } finally {
      onModuleThread { module.endCall(uuid) }
      waitFor { !isForeground() }
      moduleThread.quitSafely()
    }
  }
}
