package io.wazo.callkeep

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.telecom.Connection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.BridgeReactContext
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Foreground service with two calls, through Telecom. Same host setup as CallKeepBootstrapTest;
 * the host manifest must declare VoiceConnectionService and MANAGE_OWN_CALLS. Android 8+.
 */
@RunWith(AndroidJUnit4::class)
@Suppress("DEPRECATION")
class CallKeepForegroundServiceTest {
  private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

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

  /** The first call is answered, a second one comes in and is ended: the first call keeps its foreground service. */
  @Test fun endingTheSecondCallKeepsTheForegroundServiceOfTheFirst() {
    assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
    val module = RNCallKeepModule.getInstance(BridgeReactContext(context), true)
    onModuleThread {
      module.setup(Arguments.createMap().apply {
        putBoolean("selfManaged", true)
        putMap("foregroundService", Arguments.createMap().apply {
          putString("channelId", "io.wazo.callkeep.test")
          putString("channelName", "CallKeep test")
          putString("notificationTitle", "Call in progress")
        })
      })
    }
    val first = UUID.randomUUID().toString()
    val second = UUID.randomUUID().toString()

    onModuleThread { module.displayIncomingCall(first, "5551001", "First", false, null) }
    assertTrue("first call not created", waitFor { VoiceConnectionService.currentConnections.containsKey(first) })
    onModuleThread {
      module.answerIncomingCall(first)
      module.setCurrentCallActive(first)
    }
    assertTrue("first call not active",
      waitFor { VoiceConnectionService.currentConnections[first]?.state == Connection.STATE_ACTIVE })
    assertTrue("no foreground service during the first call", waitFor { isForeground() })

    onModuleThread { module.displayIncomingCall(second, "5551002", "Second", false, null) }
    assertTrue("second call not created", waitFor { VoiceConnectionService.currentConnections.containsKey(second) })
    onModuleThread { module.endCall(second) }
    assertTrue("second call not removed", waitFor { !VoiceConnectionService.currentConnections.containsKey(second) })
    Thread.sleep(500)
    assertTrue("foreground service stopped while the first call is still active", isForeground())

    onModuleThread { module.endCall(first) }
    assertTrue("foreground service still running after the last call", waitFor { !isForeground() })
    moduleThread.quitSafely()
  }
}
