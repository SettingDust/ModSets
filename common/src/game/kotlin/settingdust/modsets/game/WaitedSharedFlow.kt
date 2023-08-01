package settingdust.modsets.game

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

class WaitedSharedFlow<T>(private val context: CoroutineContext = Dispatchers.Default) {
    private val scope = CoroutineScope(context)

    private val _events = MutableSharedFlow<T>()

    suspend fun emit(event: T) = withContext(context) {
        _events.emit(event)
    }

    fun subscribe(block: (event: T) -> Unit) = _events
        .onEach { block(it) }
        .launchIn(scope)
}
