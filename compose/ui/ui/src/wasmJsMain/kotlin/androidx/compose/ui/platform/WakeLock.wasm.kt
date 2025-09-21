package androidx.compose.ui.platform

import kotlinx.browser.window
import kotlinx.coroutines.await
import org.w3c.dom.events.Event
import kotlin.js.Promise
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal actual fun wakeLock(): WakeLock = WasmWakeLock()

private class WasmWakeLock : WakeLock {

    override var isAcquired: Boolean = false
        private set

    private val mutex = Mutex()

    private val w3cWakeLock: W3CWakeLock? = w3cWakeLock()

    private var w3cWakeLockSentinel: W3CWakeLockSentinel? = null

    private val onVisibilityChanged: (Event) -> Unit = {
        if (isAcquired) {
            tryRequest()
        }
    }

    private fun tryRequest(): Promise<*> {
        return if (
            w3cWakeLock != null &&
            w3cWakeLockSentinel?.released != false &&
            visibilityState() == "visible"
        ) {
            w3cWakeLock.request("screen")
                .then { w3cWakeLockSentinel = it; true.toJsBoolean() }
                .catch { false.toJsBoolean(); }
        } else {
            Promise.resolve( false.toJsBoolean())
        }
    }

    override suspend fun request() = mutex.withLock {
        if (!isAcquired && w3cWakeLock != null) {
            isAcquired = true
            tryRequest().await<JsBoolean>()
            window.addEventListener("visibilitychange", onVisibilityChanged)
        }
    }

    override suspend fun release() = mutex.withLock {
        if (isAcquired && w3cWakeLock != null) {
            isAcquired = false
            window.removeEventListener("visibilitychange", onVisibilityChanged)
            w3cWakeLockSentinel?.release()?.catch { null }?.await<JsBoolean>()
            w3cWakeLockSentinel = null
        }
    }
}

private fun w3cWakeLock() : W3CWakeLock? = js("navigator.wakeLock ? navigator.wakeLock : null")
private fun visibilityState() : String = js("document.visibilityState")

private external interface W3CWakeLockSentinel : JsAny {

    val released : Boolean
    fun release() : Promise<*>
}

private external interface W3CWakeLock {
    fun request(type : String) : Promise<W3CWakeLockSentinel>
}
