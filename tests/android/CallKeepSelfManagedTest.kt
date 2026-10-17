package io.wazo.callkeep

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.Connection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.BridgeReactContext
import io.wazo.callkeep.RNCallKeepModule
import io.wazo.callkeep.VoiceConnectionService
import java.util.UUID
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Uses the real Telecom service. The host declares VoiceConnectionService and
 * MANAGE_OWN_CALLS but must not grant READ_PHONE_NUMBERS to the application. */
@RunWith(AndroidJUnit4::class)
@Suppress("DEPRECATION")
class CallKeepSelfManagedTest {
  @Test fun incomingConnectionNeedsNoPhoneNumberPermission() {
    assumeTrue(Build.VERSION.SDK_INT >= 30)
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val context = instrumentation.targetContext
    assertEquals(PackageManager.PERMISSION_DENIED,
      context.checkSelfPermission(Manifest.permission.READ_PHONE_NUMBERS))
    val uuid = UUID.randomUUID().toString()
    var connection: Connection? = null
    try {
      instrumentation.runOnMainSync {
        val module = RNCallKeepModule.getInstance(BridgeReactContext(context), true)
        module.clearInitialEvents()
        module.setup(Arguments.createMap().apply { putBoolean("selfManaged", true) })
        module.displayIncomingCall(uuid, "native-test", "CallKeep test", false)
      }
      val deadline = System.nanoTime() + 5_000_000_000L
      while (connection == null && System.nanoTime() < deadline) {
        instrumentation.runOnMainSync { connection = VoiceConnectionService.getConnection(uuid) }
        if (connection == null) Thread.sleep(50)
      }
      assertNotNull("Telecom did not create the incoming connection", connection)
      instrumentation.runOnMainSync {
        assertEquals(Connection.PROPERTY_SELF_MANAGED,
          connection!!.connectionProperties and Connection.PROPERTY_SELF_MANAGED)
      }
    } finally {
      instrumentation.runOnMainSync { VoiceConnectionService.getConnection(uuid)?.onReject() }
    }
  }
}
