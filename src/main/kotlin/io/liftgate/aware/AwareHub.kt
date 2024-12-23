package io.liftgate.aware

import com.google.gson.Gson
import io.liftgate.aware.thread.AwareThreadContext
import io.liftgate.aware.uri.WrappedAwareUri
import io.lettuce.core.ClientOptions
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.TimeoutOptions
import io.lettuce.core.resource.DefaultClientResources
import java.time.Duration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.logging.Level

/**
 * @author GrowlyX
 * @since 3/7/2022
 */
object AwareHub
{
    internal val scheduler = Executors
        .newSingleThreadScheduledExecutor()

    private lateinit var wrappedUri: WrappedAwareUri
    private var client: RedisClient? = null

    /**
     * When in master/replica configuration of Redis servers, PUBLISH
     * messages from clients connected to replica instances are not directly
     * propagated to clients connect to the master instance.
     *
     * As a temporary fix, we will set a custom redis instance URI (typically the
     * master), to redirect PUBLISH messages to.
     */
    var publishUri: RedisURI? = null

    // TODO: 3/7/2022 allow for multiple
    //  serialization providers
    lateinit var gson: () -> Gson

    fun configure(
        wrappedUri: WrappedAwareUri,
        publishUri: String,
        provider: () -> Gson
    )
    {
        this.wrappedUri = wrappedUri
        this.publishUri = RedisURI.create(publishUri)
        this.gson = provider
    }

    fun configure(
        wrappedUri: WrappedAwareUri,
        provider: () -> Gson
    )
    {
        this.wrappedUri = wrappedUri
        this.gson = provider
    }

    fun client(): RedisClient
    {
        if (client == null)
        {
            client = RedisClient.create(
                DefaultClientResources.builder()
                    .ioThreadPoolSize(4)
                    .computationThreadPoolSize(4)
                    .build(),
                RedisURI.create(this.wrappedUri.build())
            )
            client!!.options = ClientOptions.builder()
                .timeoutOptions(
                    TimeoutOptions.builder()
                        .timeoutCommands(false)
                        .fixedTimeout(
                            Duration.ofSeconds(0L)
                        )
                        .build()
                )
                .autoReconnect(true)
                .build()
        }

        return client!!
    }

    fun close()
    {
        client?.shutdown()
        client = null

        outboundReqPool.shutdownNow()
    }

    @JvmStatic
    val outboundReqPool: ExecutorService = Executors.newCachedThreadPool()

    fun <T : Any> publish(
        aware: io.liftgate.aware.Aware<T>,
        message: T,
        context: AwareThreadContext =
            AwareThreadContext.ASYNC,
        channel: String = aware.channel,
    )
    {
        if (
            context == AwareThreadContext.SYNC
        )
        {
            try
            {
                aware.publishConnection.sync()
                    .publish(channel, message)
            } catch (exception: Exception)
            {
                aware.logger.log(Level.WARNING, exception) {
                    "Something went wrong while trying to distribute a message."
                }
            }
            return
        }

        outboundReqPool.submit {
            aware.publishConnection.sync()
                .publish(channel, message)
        }
    }
}
