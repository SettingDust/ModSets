package settingdust.modsets.ingame

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.launch

/** https://github.com/Kotlin/kotlinx.coroutines/issues/2603#issuecomment-2065377205 */
class WaitingSharedFlow<T>() : Flow<T> {
    private val allChannels = mutableSetOf<Channels<T>>()

    override suspend fun collect(collector: FlowCollector<T>) {
        val channels = Channels<T>()
        synchronized(allChannels) { allChannels += channels }

        try {
            while (true) {
                collector.emit(channels.data.receive())
                channels.done.send(Unit)
            }
        } finally {
            synchronized(allChannels) { allChannels -= channels.also { it.close() } }
        }
    }

    suspend fun emit(value: T) = coroutineScope {
        synchronized(allChannels) {} // Ensuring a memory barrier with collectors.
        for (channels in allChannels) {
            launch {
                try {
                    channels.data.send(value)
                } catch (_: ClosedSendChannelException) {
                    return@launch
                }
                try {
                    channels.done.receive()
                } catch (_: ClosedReceiveChannelException) {
                }
            }
        }
    }

    private data class Channels<T>(
        val data: Channel<T> = Channel(),
        val done: Channel<Unit> = Channel()
    ) {
        fun close() {
            data.close()
            done.close()
        }
    }
}
