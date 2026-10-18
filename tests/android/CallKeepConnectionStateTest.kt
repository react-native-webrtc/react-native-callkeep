package io.wazo.callkeep

import android.content.Context
import android.os.ParcelFileDescriptor
import android.os.Process
import android.os.UserManager
import android.telecom.Connection
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.BridgeReactContext
import io.wazo.callkeep.RNCallKeepModule
import io.wazo.callkeep.VoiceConnectionService
import java.util.UUID
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Uses the real Telecom service. The host declares VoiceConnectionService, RECORD_AUDIO,
 * CALL_PHONE, READ_PHONE_STATE and READ_PHONE_NUMBERS, granted when installed (`adb install -g`),
 * as for CallKeepStartCallFailedTest. */
@RunWith(AndroidJUnit4::class)
class CallKeepConnectionStateTest {
  private val instrumentation = InstrumentationRegistry.getInstrumentation()
  private lateinit var module: RNCallKeepModule
  private val calls = mutableListOf<String>()

  @After fun tearDown() {
    if (!::module.isInitialized) return
    instrumentation.runOnMainSync {
      calls.forEach { VoiceConnectionService.getConnection(it)?.onDisconnect() }
      module.clearInitialEvents()
    }
    shell("telecom unregister-phone-account ${account()}")
  }

  @Test fun anOutgoingSelfManagedCallIsDialing() = outgoingCallIsDialing(selfManaged = true)

  @Test fun anOutgoingManagedCallIsDialing() = outgoingCallIsDialing(selfManaged = false)

  @Test fun anIncomingSelfManagedCallIsRinging() = incomingCallIsRinging(selfManaged = true)

  @Test fun anIncomingManagedCallIsRinging() = incomingCallIsRinging(selfManaged = false)

  private fun outgoingCallIsDialing(selfManaged: Boolean) {
    setUp(selfManaged)
    val uuid = UUID.randomUUID().toString().also { calls += it }
    instrumentation.runOnMainSync { module.startCall(uuid, "5551001", "Outgoing", false) }

    waitFor("Telecom shows the call dialing") { telecomState() == "DIALING" }
    assertEquals(Connection.STATE_DIALING, stateOf(uuid))

    instrumentation.runOnMainSync { module.setCurrentCallActive(uuid) }
    assertEquals(Connection.STATE_ACTIVE, stateOf(uuid))
    waitFor("Telecom shows the call active") { telecomState() == "ACTIVE" }
  }

  private fun incomingCallIsRinging(selfManaged: Boolean) {
    setUp(selfManaged)
    val uuid = UUID.randomUUID().toString().also { calls += it }
    instrumentation.runOnMainSync { module.displayIncomingCall(uuid, "5551002", "Incoming", false) }

    waitFor("Telecom shows the call ringing") { telecomState() == "RINGING" }
    assertEquals(Connection.STATE_RINGING, stateOf(uuid))

    instrumentation.runOnMainSync {
      module.answerIncomingCall(uuid)
      module.setCurrentCallActive(uuid)
    }
    assertEquals(Connection.STATE_ACTIVE, stateOf(uuid))
    waitFor("Telecom shows the call active") { telecomState() == "ACTIVE" }
  }

  private fun setUp(selfManaged: Boolean) {
    // Android 16 doesn't turn a phone account, left by another test, into the other kind.
    shell("telecom unregister-phone-account ${account()}")
    instrumentation.runOnMainSync {
      module = RNCallKeepModule.getInstance(BridgeReactContext(instrumentation.targetContext), true)
      module.setup(Arguments.createMap().apply { putBoolean("selfManaged", selfManaged) })
      module.clearInitialEvents()
    }
    // A phone account that isn't self-managed is disabled until the user enables it.
    if (!selfManaged) shell("telecom set-phone-account-enabled ${account()}")
  }

  /** The connection's state once Telecom has created it. */
  private fun stateOf(uuid: String): Int {
    waitFor("Telecom creates the connection") { VoiceConnectionService.getConnection(uuid) != null }
    var state = -1
    instrumentation.runOnMainSync { state = VoiceConnectionService.getConnection(uuid)!!.state }
    return state
  }

  /** The state Telecom shows for the only call, read from `dumpsys telecom`. */
  private fun telecomState(): String? {
    val dump = shell("dumpsys telecom")
    val section = dump.substringAfter("mCalls:")
    // "[TC@1, DIALING, ..." up to Android 10, "[Call id=TC@1, state=DIALING, ..." later.
    val states = Regex("""\[(?:Call id=)?TC@\d+, (?:state=)?([A-Z_]+),""").findAll(section)
      .map { it.groupValues[1] }.toList()
    Log.i("CallKeepConnectionStateTest", "Telecom call states: $states")
    return states.firstOrNull()
  }

  /** The phone account's component, id and user serial number, as `adb shell telecom` takes them. */
  private fun account(): String {
    val context = instrumentation.targetContext
    val id = context.applicationInfo.loadLabel(context.packageManager)
    val user = (context.getSystemService(Context.USER_SERVICE) as UserManager)
      .getSerialNumberForUser(Process.myUserHandle())
    return "${context.packageName}/${VoiceConnectionService::class.java.name} $id $user"
  }

  private fun shell(command: String): String {
    val output = instrumentation.uiAutomation.executeShellCommand(command)
    return ParcelFileDescriptor.AutoCloseInputStream(output).use { String(it.readBytes()) }
  }

  private fun waitFor(what: String, condition: () -> Boolean) {
    val deadline = System.nanoTime() + 5_000_000_000L
    while (System.nanoTime() < deadline) {
      var met = false
      instrumentation.runOnMainSync { met = condition() }
      if (met) return
      Thread.sleep(50)
    }
    fail("timed out: $what")
  }
}
