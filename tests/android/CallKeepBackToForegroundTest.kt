package io.wazo.callkeep

import android.app.Activity
import android.app.ActivityManager
import android.app.KeyguardManager
import android.content.Intent
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.facebook.react.bridge.BridgeReactContext
import io.wazo.callkeep.RNCallKeepModule
import org.junit.After
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** backToForeground on a locked device: the app comes to the front and the lock screen is dismissed,
 * both before the app has an activity (a headless task, the process having been started by the
 * runner) and with its activity in the background. Needs a lock screen without credentials, e.g. an
 * emulator after `adb shell locksettings set-disabled false`, and a host whose JavaScript loads
 * without Metro (`debuggableVariants = []` in its `react` block). The activity is not destroyed:
 * when it is, CallKeep ends the calls and kills the process. */
@RunWith(AndroidJUnit4::class)
class CallKeepBackToForegroundTest {
  private val instrumentation = InstrumentationRegistry.getInstrumentation()
  private val context = instrumentation.targetContext
  private val activityManager = context.getSystemService(ActivityManager::class.java)
  private val keyguardManager = context.getSystemService(KeyguardManager::class.java)

  @After fun unlock() {
    shell("input keyevent KEYCODE_WAKEUP")
    shell("wm dismiss-keyguard")
  }

  @Test fun showsTheAppAndAsksToUnlock() {
    assumeTrue("needs a lock screen without credentials", !keyguardManager.isDeviceSecure)
    assertNull("the runner starts the process without an activity", activityIn(Stage.CREATED))

    lock()
    instrumentation.runOnMainSync {
      RNCallKeepModule.getInstance(BridgeReactContext(context), true).backToForeground()
    }
    waitFor("without an activity: the app is shown, unlocked") { isShownUnlocked() }

    context.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
      .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    waitFor("the app is in the background") { activityIn(Stage.STOPPED) != null }
    lock()
    instrumentation.runOnMainSync {
      // The module now has React's context, whose current activity is the app's.
      val module = RNCallKeepModule.getInstance(null, false)
      assertNotNull(module.currentReactActivity)
      module.backToForeground()
    }
    waitFor("with an activity: the app is shown, unlocked") { isShownUnlocked() }
    assertEquals(1, activityManager.appTasks.size)
  }

  private fun lock() {
    shell("input keyevent KEYCODE_SLEEP")
    shell("input keyevent KEYCODE_WAKEUP")
    assumeTrue("the device has no lock screen", eventually { keyguardManager.isKeyguardLocked })
  }

  private fun isShownUnlocked() = activityIn(Stage.RESUMED) != null && !keyguardManager.isKeyguardLocked

  private fun activityIn(stage: Stage): Activity? {
    var activity: Activity? = null
    instrumentation.runOnMainSync {
      activity = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(stage).firstOrNull()
    }
    return activity
  }

  private fun shell(command: String) {
    val output = instrumentation.uiAutomation.executeShellCommand(command)
    ParcelFileDescriptor.AutoCloseInputStream(output).use { it.readBytes() }
  }

  private fun eventually(condition: () -> Boolean): Boolean {
    val deadline = System.nanoTime() + 10_000_000_000L
    while (System.nanoTime() < deadline) {
      if (condition()) return true
      Thread.sleep(100)
    }
    return false
  }

  private fun waitFor(what: String, condition: () -> Boolean) {
    if (!eventually(condition)) fail("timed out: $what")
  }
}
