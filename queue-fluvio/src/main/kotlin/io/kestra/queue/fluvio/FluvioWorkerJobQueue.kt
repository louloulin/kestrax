package io.kestra.queue.fluvio

import io.kestra.core.exceptions.DeserializationException
import io.kestra.core.queues.WorkerJobQueueInterface
import io.kestra.core.runners.WorkerJob
import io.kestra.core.utils.Either
import java.io.IOException
import java.util.function.Consumer

/**
 * Fluvio implementation of WorkerJobQueueInterface
 *
 * This class wraps a FluvioQueue<WorkerJob> to provide the specialized
 * WorkerJobQueueInterface functionality while maintaining compatibility
 * with Kestra's worker job processing system.
 */
class FluvioWorkerJobQueue(
    private val fluvioQueue: FluvioQueue<WorkerJob>
) : WorkerJobQueueInterface {

    override fun receive(
        consumerGroup: String?,
        queueType: Class<*>?,
        consumer: Consumer<Either<WorkerJob, DeserializationException>>
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
