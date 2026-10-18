package io.wazo.callkeep

import android.content.Context
import android.os.ParcelFileDescriptor
import android.os.Process
import android.os.UserManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.BridgeReactContext
import com.facebook.react.bridge.PromiseImpl
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import io.wazo.callkeep.RNCallKeepModule
import io.wazo.callkeep.VoiceConnectionService
import java.util.UUID
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Uses the real Telecom service. JavaScript doesn't run, so events stay queued and are read with
 * getInitialEvents. The host declares VoiceConnectionService, RECORD_AUDIO, CALL_PHONE,
 * READ_PHONE_STATE and READ_PHONE_NUMBERS, granted when installed (`adb install -g`): unlike
 * CallKeepSelfManagedTest's host, which must not grant READ_PHONE_NUMBERS. */
@RunWith(AndroidJUnit4::class)
class CallKeepStartCallFailedTest {
  private val instrumentation = InstrumentationRegistry.getInstrumentation()
  private lateinit var module: RNCallKeepModule
  private val calls = mutableListOf<String>()

  @After fun tearDown() {
    if (!::module.isInitialized) return
    instrumentation.runOnMainSync {
      calls.forEach { VoiceConnectionService.getConnection(it)?.onDisconnect() }
      module.clearInitialEvents()
    }
    // Leaves no managed phone account for the tests that run in self-managed mode.
    shell("telecom unregister-phone-account ${account()}")
  }

  /** Telecom allows one managed outgoing call at a time. In self-managed mode, it asks the user
   * whether to end the first call instead, and sends nothing to the app when they cancel. */
  @Test fun aSecondCallWhileTheFirstIsDialingIsRefused() {
    // Android 16 doesn't turn a self-managed phone account, left by another test, into a managed one.
    shell("telecom unregister-phone-account ${account()}")
    instrumentation.runOnMainSync {
      module = RNCallKeepModule.getInstance(BridgeReactContext(instrumentation.targetContext), true)
      module.setup(Arguments.createMap().apply { putBoolean("selfManaged", false) })
      module.clearInitialEvents()
    }
    // A phone account that isn't self-managed is disabled until the user enables it.
    shell("telecom set-phone-account-enabled ${account()}")

    val first = startCall("5551001", "First")
    waitFor("the first call is dialing") { VoiceConnectionService.getConnection(first) != null }

    // Telecom's request has the number, not startCall's extras: CallKeep finds the call by number.
    val second = startCall("+33 6 12 34 56 78", "Second")
    val event = startCallFailed(second)
    assertEquals("+33 6 12 34 56 78", event.getString("handle"))
    assertEquals("Second", event.getString("name"))
    instrumentation.runOnMainSync {
      assertNull(VoiceConnectionService.getConnection(second))
      assertNotNull("the first call goes on", VoiceConnectionService.getConnection(first))
    }
    assertTrue("startCallFailed is not sent for the first call", events(first).isEmpty())
  }

  /** The phone account's component, id and user serial number, as `adb shell telecom` takes them. */
  private fun account(): String {
    val context = instrumentation.targetContext
    val id = context.applicationInfo.loadLabel(context.packageManager)
    val user = (context.getSystemService(Context.USER_SERVICE) as UserManager)
      .getSerialNumberForUser(Process.myUserHandle())
    return "${context.packageName}/${VoiceConnectionService::class.java.name} $id $user"
  }

  private fun shell(command: String) {
    val output = instrumentation.uiAutomation.executeShellCommand(command)
    ParcelFileDescriptor.AutoCloseInputStream(output).use { it.readBytes() }
  }

  private fun startCall(number: String, name: String): String {
    val uuid = UUID.randomUUID().toString().also { calls += it }
    instrumentation.runOnMainSync { module.startCall(uuid, number, name, false) }
    return uuid
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

  private fun events(uuid: String): List<ReadableMap> {
    var pending: ReadableArray? = null
    instrumentation.runOnMainSync {
      module.getInitialEvents(PromiseImpl({ args -> pending = args[0] as ReadableArray },
        { args -> fail(args.contentToString()) }))
    }
    val all = requireNotNull(pending)
    return (0 until all.size()).mapNotNull { all.getMap(it) }
      .filter { it.getString("name") == "RNCallKeepStartCallFailed" }
      .mapNotNull { it.getMap("data") }
      .filter { it.getString("callUUID") == uuid }
  }

  private fun startCallFailed(uuid: String): ReadableMap {
    val deadline = System.nanoTime() + 5_000_000_000L
    while (System.nanoTime() < deadline) {
      events(uuid).firstOrNull()?.let { return it }
      Thread.sleep(50)
    }
    fail("no startCallFailed for $uuid")
    throw AssertionError()
  }
}
