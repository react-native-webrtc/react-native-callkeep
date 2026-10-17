package io.wazo.callkeep

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.BridgeReactContext
import com.facebook.react.bridge.PromiseImpl
import io.wazo.callkeep.RNCallKeepModule
import io.wazo.callkeep.VoiceConnection
import io.wazo.callkeep.VoiceConnectionService
import java.util.UUID
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The methods that need Android 8 APIs (TelecomManager.isInManagedCall, Connection.setAudioRoute)
 * don't throw on older versions, and setAudioRoute settles its promise. Run it on Android 7 (API 24
 * or 25) and on a recent version. The connection is created directly, without Telecom. */
@RunWith(AndroidJUnit4::class)
class CallKeepAudioRouteTest {
  private val instrumentation = InstrumentationRegistry.getInstrumentation()
  private lateinit var module: RNCallKeepModule
  private val uuid = UUID.randomUUID().toString()

  @Before fun setUp() {
    instrumentation.runOnMainSync {
      val context = instrumentation.targetContext
      module = RNCallKeepModule.getInstance(BridgeReactContext(context), true)
      module.registerPhoneAccount(Arguments.createMap())
      VoiceConnectionService.currentConnections[uuid] = VoiceConnection(context, hashMapOf(Constants.EXTRA_CALL_UUID to uuid))
    }
  }

  @After fun tearDown() {
    VoiceConnectionService.currentConnections.remove(uuid)
  }

  @Test fun checkIsInManagedCall() {
    var inCall: Boolean? = null
    instrumentation.runOnMainSync { inCall = module.checkIsInManagedCall() }
    assertEquals(false, inCall)
  }

  @Test fun setAudioRoute() {
    val result = settle { module.setAudioRoute(uuid, "Speaker", it) }
    if (Build.VERSION.SDK_INT >= 26) {
      assertEquals("resolved true", result)
    } else {
      assertEquals("rejected: Setting the audio route needs Android 8 (API 26) or later", result)
    }
  }

  @Test fun setAudioRouteWithoutTheCall() {
    assertEquals("rejected: No call found with UUID unknown",
      settle { module.setAudioRoute("unknown", "Speaker", it) })
  }

  @Test fun toggleAudioRouteSpeaker() {
    instrumentation.runOnMainSync { module.toggleAudioRouteSpeaker(uuid, true) }
  }

  private fun settle(call: (PromiseImpl) -> Unit): String {
    var result = "never settled"
    instrumentation.runOnMainSync {
      call(PromiseImpl({ args -> result = "resolved ${args[0]}" },
        { args -> result = "rejected: ${(args[0] as com.facebook.react.bridge.ReadableMap).getString("message")}" }))
    }
    return result
  }
}
