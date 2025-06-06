package io.kestra.queue.fluvio

import io.kestra.core.exceptions.DeserializationException
import io.kestra.core.queues.WorkerTriggerResultQueueInterface
import io.kestra.core.runners.WorkerTriggerResult
import io.kestra.core.utils.Either
import java.io.IOException
import java.util.function.Consumer

/**
 * Fluvio implementation of WorkerTriggerResultQueueInterface
 *
 * This class wraps a FluvioQueue<WorkerTriggerResult> to provide the specialized
 * WorkerTriggerResultQueueInterface functionality while maintaining compatibility
 * with Kestra's worker trigger result processing system.
 */
class FluvioWorkerTriggerResultQueue(
    private val fluvioQueue: FluvioQueue<WorkerTriggerResult>
) : WorkerTriggerResultQueueInterface {

    override fun receive(
        consumerGroup: String?,
        queueType: Class<*>?,
        consumer: Consumer<Either<WorkerTriggerResult, DeserializationException>>
    ): Runnable {
        return fluvioQueue.receive(consumerGroup, queueType, consumer)
    }

    override fun pause() {
        fluvioQueue.pause()
    }

    override fun resume() {
        fluvioQueue.resume()
    }

    @Throws(IOException::class)
    override fun close() {
        fluvioQueue.close()
    }
}
