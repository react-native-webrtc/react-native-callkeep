package io.wazo.callkeep

import android.os.Build
import android.os.Bundle
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
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Uses the real Telecom service, in self-managed mode. JavaScript doesn't run, so events stay
 * queued and are read with getInitialEvents. The host declares VoiceConnectionService. */
@RunWith(AndroidJUnit4::class)
@Suppress("DEPRECATION")
class CallKeepDidDisplayIncomingCallTest {
  private val instrumentation = InstrumentationRegistry.getInstrumentation()
  private lateinit var module: RNCallKeepModule
  private val calls = mutableListOf<String>()

  @Before fun setUp() {
    assumeTrue(Build.VERSION.SDK_INT >= 26)
    instrumentation.runOnMainSync {
      module = RNCallKeepModule.getInstance(BridgeReactContext(instrumentation.targetContext), true)
      module.setup(Arguments.createMap().apply { putBoolean("selfManaged", true) })
      module.registerPhoneAccount(Arguments.createMap().apply { putBoolean("selfManaged", true) })
      module.clearInitialEvents()
    }
  }

  @After fun tearDown() {
    if (!::module.isInitialized) return // skipped before Android 8
    instrumentation.runOnMainSync {
      calls.forEach { VoiceConnectionService.getConnection(it)?.onReject() }
      module.clearInitialEvents()
    }
  }

  @Test fun displayIncomingCallSendsTheEventOnceTelecomCreatedTheCall() {
    val uuid = newCall()
    instrumentation.runOnMainSync { module.displayIncomingCall(uuid, "+33612345678", "Alice", true) }
    val event = didDisplayIncomingCall(uuid)
    instrumentation.runOnMainSync { assertNotNull(VoiceConnectionService.getConnection(uuid)) }
    assertEquals("+33612345678", event.getString("handle"))
    assertEquals("Alice", event.getString("localizedCallerName"))
    assertEquals("Alice", event.getString("name"))
    assertEquals("1", event.getString("hasVideo"))
    assertEquals("0", event.getString("fromPushKit"))
    assertFalse(event.hasKey("payload"))
  }

  @Test fun reportNewIncomingCallSendsItsPayload() {
    val uuid = newCall()
    instrumentation.runOnMainSync {
      module.reportNewIncomingCall(uuid, "5551234", "Bob", false, """{"room":"42"}""")
    }
    val event = didDisplayIncomingCall(uuid)
    assertEquals("5551234", event.getString("handle"))
    assertEquals("Bob", event.getString("localizedCallerName"))
    assertEquals("0", event.getString("hasVideo"))
    assertEquals("""{"room":"42"}""", event.getString("payload"))
    assertEquals("the event is sent once", 1, events(uuid).size)
  }

  @Test fun nativeDisplayIncomingCallSendsItsBundlePayload() {
    val uuid = newCall()
    instrumentation.runOnMainSync {
      module.displayIncomingCall(uuid, "5551234", "Carol", false, Bundle().apply { putString("room", "42") })
    }
    assertEquals("42", didDisplayIncomingCall(uuid).getMap("payload")?.getString("room"))
  }

  private fun newCall() = UUID.randomUUID().toString().also { calls += it }

  private fun events(uuid: String): List<ReadableMap> {
    var pending: ReadableArray? = null
    instrumentation.runOnMainSync {
      module.getInitialEvents(PromiseImpl({ args -> pending = args[0] as ReadableArray },
        { args -> fail(args.contentToString()) }))
    }
    val all = requireNotNull(pending)
    return (0 until all.size()).mapNotNull { all.getMap(it) }
      .filter { it.getString("name") == "RNCallKeepDidDisplayIncomingCall" }
      .mapNotNull { it.getMap("data") }
      .filter { it.getString("callUUID") == uuid }
  }

  private fun didDisplayIncomingCall(uuid: String): ReadableMap {
    val deadline = System.nanoTime() + 5_000_000_000L
    while (System.nanoTime() < deadline) {
      events(uuid).firstOrNull()?.let { return it }
      Thread.sleep(50)
    }
    fail("no didDisplayIncomingCall for $uuid")
    throw AssertionError()
  }
}
