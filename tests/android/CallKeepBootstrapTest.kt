package io.wazo.callkeep

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.BridgeReactContext
import com.facebook.react.bridge.JavaScriptModule
import com.facebook.react.bridge.PromiseImpl
import com.facebook.react.bridge.ReadableArray
import io.wazo.callkeep.RNCallKeepModule
import java.lang.reflect.Proxy
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Run on device, with the RN native libraries but without a bridge/runtime. */
@RunWith(AndroidJUnit4::class)
@Suppress("DEPRECATION")
class CallKeepBootstrapTest {
  private fun onMain(work: () -> Unit) =
    InstrumentationRegistry.getInstrumentation().runOnMainSync(work)

  @Test fun nativeSetupAndBufferedAnswerBeforeJavaScript() = onMain {
    val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    val reactContext = BridgeReactContext(context)
    assertFalse(reactContext.hasActiveReactInstance())
    val module = RNCallKeepModule.getInstance(reactContext, true)
    module.clearInitialEvents()
    module.setup(Arguments.createMap().apply { putBoolean("selfManaged", true) })
    module.sendEventToJS("RNCallKeepPerformAnswerCallAction", Arguments.createMap().apply {
      putString("callUUID", "b3b5837b-645e-43e4-b91b-67d5c28042f4")
    })
    var events: ReadableArray? = null
    module.getInitialEvents(PromiseImpl({ args -> events = args[0] as ReadableArray },
      { args -> fail(args.contentToString()) }))
    assertEquals(1, events?.size())
    assertEquals("RNCallKeepPerformAnswerCallAction", events?.getMap(0)?.getString("name"))
    assertFalse(reactContext.hasActiveReactInstance())
    module.clearInitialEvents()
  }

  private class EventContext(context: Context) : BridgeReactContext(context) {
    var ready = false
    val batches = mutableListOf<ReadableArray>()
    override fun hasActiveCatalystInstance() = ready
    override fun hasActiveReactInstance() = ready
    override fun <T : JavaScriptModule> getJSModule(type: Class<T>): T {
      check(ready) { "JavaScript has not started" }
      return type.cast(Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, method, args ->
        if (method.name == "emit" && args?.get(0) == "RNCallKeepDidLoadWithEvents")
          batches.add(args[1] as ReadableArray)
        null
      })!!
    }
  }

  @Test fun eventsAfterFirstSetupAreFlushedOnceWhenJavaScriptIsReady() = onMain {
    val context = EventContext(InstrumentationRegistry.getInstrumentation().targetContext)
    val module = RNCallKeepModule.getInstance(context, true)
    module.clearInitialEvents()
    module.setup(Arguments.createMap().apply { putBoolean("selfManaged", true) })
    module.sendEventToJS("RNCallKeepPerformAnswerCallAction", Arguments.createMap())
    module.sendEventToJS("RNCallKeepPerformEndCallAction", Arguments.createMap())
    context.ready = true
    module.registerEvents()
    module.startObserving() // setup also calls this twice.
    assertEquals(1, context.batches.size)
    val events = context.batches.single()
    assertEquals(2, events.size())
    assertEquals("RNCallKeepPerformAnswerCallAction", events.getMap(0)?.getString("name"))
    assertEquals("RNCallKeepPerformEndCallAction", events.getMap(1)?.getString("name"))
    module.clearInitialEvents()
  }

  @Test fun tryingToObserveBeforeJavaScriptPreservesPendingActions() = onMain {
    val context = EventContext(InstrumentationRegistry.getInstrumentation().targetContext)
    val module = RNCallKeepModule.getInstance(context, true)
    module.clearInitialEvents()
    module.sendEventToJS("RNCallKeepPerformEndCallAction", Arguments.createMap())
    module.startObserving()
    assertTrue(context.batches.isEmpty())
    context.ready = true
    module.registerEvents()
    assertEquals(1, context.batches.single().size())
    module.clearInitialEvents()
  }

  @Test fun readingPendingActionsTwiceReturnsIndependentCompleteSnapshots() = onMain {
    val context = EventContext(InstrumentationRegistry.getInstrumentation().targetContext)
    val module = RNCallKeepModule.getInstance(context, true)
    module.clearInitialEvents()
    fun pending(): ReadableArray {
      var result: ReadableArray? = null
      module.getInitialEvents(PromiseImpl({ args -> result = args[0] as ReadableArray },
        { args -> fail(args.contentToString()) }))
      return requireNotNull(result)
    }
    module.sendEventToJS("RNCallKeepPerformAnswerCallAction", Arguments.createMap().apply {
      putString("callUUID", "b3b5837b-645e-43e4-b91b-67d5c28042f4")
    })
    val first = pending()
    assertEquals(1, first.size())
    module.sendEventToJS("RNCallKeepDidActivateAudioSession", null)
    val second = pending()
    assertNotSame(first, second)
    assertEquals(1, first.size())
    assertEquals(2, second.size())
    assertEquals("b3b5837b-645e-43e4-b91b-67d5c28042f4",
      second.getMap(0)?.getMap("data")?.getString("callUUID"))
    assertEquals(0, second.getMap(1)?.getMap("data")?.toHashMap()?.size)
    module.clearInitialEvents()
    assertEquals(0, pending().size())
  }
}
