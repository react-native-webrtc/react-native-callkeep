package io.wazo.callkeep

import android.Manifest
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.BridgeReactContext
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Native call-state listener on Android 6-11 (PhoneStateListener). Same host setup as
 * CallKeepBootstrapTest; run it on an API 23-30 device or emulator.
 */
@RunWith(AndroidJUnit4::class)
class CallKeepNativeCallsListenerTest {
  @get:Rule val readPhoneState: GrantPermissionRule =
    GrantPermissionRule.grant(Manifest.permission.READ_PHONE_STATE)

  private fun module(): RNCallKeepModule {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    return RNCallKeepModule.getInstance(BridgeReactContext(context), true).apply {
      setup(Arguments.createMap().apply { putBoolean("selfManaged", true) })
    }
  }

  private fun runOn(thread: HandlerThread, work: () -> Unit): Boolean {
    val done = CountDownLatch(1)
    Handler(thread.looper).post { work(); done.countDown() }
    return done.await(3, TimeUnit.SECONDS)
  }

  /** displayIncomingCall and startCall call listenToNativeCallsState on React Native's module thread. */
  @Test fun listeningReturnsAndStoppingKeepsTheModuleThreadRunning() {
    assumeTrue(Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
    val module = module()
    val moduleThread = HandlerThread("mqt_native_modules").apply { start() }
    assertTrue("listenToNativeCallsState did not return", runOn(moduleThread) { module.listenToNativeCallsState() })
    assertTrue("stopListenToNativeCallsState did not return", runOn(moduleThread) { module.stopListenToNativeCallsState() })
    assertTrue("the module thread no longer runs messages", runOn(moduleThread) {})
    moduleThread.quitSafely()
  }

  /** Native code may call it from a thread without a looper, e.g. a push service thread. */
  @Test fun listeningFromAThreadWithoutLooperReturnsWithoutError() {
    assumeTrue(Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
    val module = module()
    var error: Throwable? = null
    val thread = Thread {
      try {
        module.listenToNativeCallsState()
        module.stopListenToNativeCallsState()
      } catch (e: Throwable) {
        error = e
      }
    }
    thread.start()
    thread.join(3000)
    assertFalse("listenToNativeCallsState did not return", thread.isAlive)
    assertNull("listenToNativeCallsState threw $error", error)
  }
}
