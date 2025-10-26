package settingdust.mod_sets.util

import org.apache.logging.log4j.Logger
import java.util.*

object ServiceLoaderUtil {
    lateinit var defaultLogger: Logger

    inline fun <reified T> load() = ServiceLoader.load(T::class.java)!!

    fun <T> findService(clazz: Class<T>, serviceLoader: ServiceLoader<T>, logger: Logger = defaultLogger) =
        findServices(clazz, serviceLoader, logger).first()

    inline fun <reified T> findService(serviceLoader: ServiceLoader<T> = load(), logger: Logger = defaultLogger) =
        findService(T::class.java, serviceLoader, logger)

    fun <T> findServices(
        clazz: Class<T>,
        serviceLoader: ServiceLoader<T>,
        logger: Logger = defaultLogger,
        required: Boolean = true
    ) = sequence {
        val prefix = String.format("[%s] ", logger.name)
        val iterator = serviceLoader.stream().iterator()
        val errors = mutableListOf<Throwable>()
        var current = findNext(iterator, errors)
        var found = false
        while (current.isSuccess) {
            val providerName: String = current.getOrThrow().type().getName()

            logger.debug("${prefix}Loading $providerName")

            try {
                yield(current.getOrThrow().get())
                found = true
            } catch (t: Throwable) {
                val e = IllegalStateException("${prefix}Loading $providerName failed", t)
                errors.add(e)
                logger.debug(e)
            }

            current = findNext<T>(iterator, errors)
        }
        if (found || !required) return@sequence
        val exception = IllegalStateException("Load service of $clazz failed")
        if (errors.isEmpty()) {
            exception.addSuppressed(NoSuchElementException("Can't find service for $clazz"))
        }
        errors.forEach(exception::addSuppressed)
        throw exception
    }

    inline fun <reified T> findServices(
        serviceLoader: ServiceLoader<T> = load(),
        logger: Logger = defaultLogger,
        required: Boolean = true
    ) = findServices(T::class.java, serviceLoader, logger, required)

    fun <T> loadServices(
        clazz: Class<T>,
        serviceLoader: ServiceLoader<T>,
        logger: Logger = defaultLogger,
        required: Boolean = true
    ) {
        findServices(clazz, serviceLoader, logger, required).forEach { _ -> }
    }

    inline fun <reified T> loadServices(
        serviceLoader: ServiceLoader<T> = load(),
        logger: Logger = defaultLogger,
        required: Boolean = true
    ) =
        findServices(T::class.java, serviceLoader, logger, required).count()

    private fun <T> findNext(
        iterator: Iterator<ServiceLoader.Provider<T>>,
        errors: MutableList<Throwable>
    ): Result<ServiceLoader.Provider<T>> {
        var current: ServiceLoader.Provider<T>? = null
        do {
            try {
                current = iterator.next()
            } catch (e: NoSuchElementException) {
                return Result.failure(e)
            } catch (t: Throwable) {
                errors.add(t)
            }
        } while (current == null)
        return Result.success(current)
    }
}