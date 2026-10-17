package io.wazo.callkeep

import android.os.Build
import android.telecom.Connection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.facebook.react.bridge.Arguments
import java.util.UUID
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Setup and an incoming call from native code, without a React context or a CallKeep module, then
 * Telecom's call focus, gained by the active call and gone with the last call. Same host setup as CallKeepSelfManagedTest. Android 9+ (the call focus).
 */
@RunWith(AndroidJUnit4::class)
class CallKeepNativeSetupTest {
  private val instrumentation = InstrumentationRegistry.getInstrumentation()
  private val context get() = instrumentation.targetContext

  private fun waitFor(condition: () -> Boolean): Boolean {
    val deadline = System.currentTimeMillis() + 5000
    while (System.currentTimeMillis() < deadline) {
      var met = false
      instrumentation.runOnMainSync { met = condition() }
      if (met) return true
      Thread.sleep(100)
    }
    return false
  }

  @Test fun aNativeIncomingCallGetsTheCallFocusUntilItEnds() {
    assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
    // Other fixtures may have created the module in this process: this one runs without it.
    val module = RNCallKeepModule.instance
    RNCallKeepModule.instance = null
    var gained = false
    val uuid = UUID.randomUUID().toString()
    try {
      instrumentation.runOnMainSync {
        VoiceConnectionService.nativeFocusListener = object : VoiceConnectionService.NativeFocusListener {
          override fun onFocusGained() { gained = true }
          override fun onFocusLost() { VoiceConnectionService.currentConnectionService?.connectionServiceFocusReleased() }
        }
        RNCallKeepModule.setupNative(context, Arguments.createMap().apply { putBoolean("selfManaged", true) })
        RNCallKeepModule.displayIncomingCall(context, uuid, "5551007", "Native call", false)
      }
      assertTrue("Telecom did not create the incoming connection", waitFor { VoiceConnectionService.getConnection(uuid) != null })
      instrumentation.runOnMainSync {
        assertNull("native setup created a module", RNCallKeepModule.instance)
        assertEquals(Connection.PROPERTY_SELF_MANAGED,
          VoiceConnectionService.getConnection(uuid)!!.connectionProperties and Connection.PROPERTY_SELF_MANAGED)
        VoiceConnectionService.getConnection(uuid)!!.onAnswer()
        VoiceConnectionService.setState(uuid, Connection.STATE_ACTIVE)
      }
      assertTrue("the active call did not get the call focus",
        waitFor { gained && VoiceConnectionService.hasConnectionServiceFocus() })
      instrumentation.runOnMainSync { VoiceConnectionService.getConnection(uuid)!!.onDisconnect() }
      assertTrue("the call focus outlived the last call",
        waitFor { VoiceConnectionService.getConnection(uuid) == null && !VoiceConnectionService.hasConnectionServiceFocus() })
    } finally {
      instrumentation.runOnMainSync {
        VoiceConnectionService.getConnection(uuid)?.onDisconnect()
        VoiceConnectionService.nativeFocusListener = null
      }
      RNCallKeepModule.instance = module
    }
  }
}
