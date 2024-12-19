package io.liftgate.aware.test

import com.google.gson.GsonBuilder
import com.google.gson.LongSerializationPolicy
import io.liftgate.aware.Aware
import io.liftgate.aware.AwareBuilder
import io.liftgate.aware.AwareHub
import io.liftgate.aware.annotation.ExpiresIn
import io.liftgate.aware.annotation.Subscribe
import io.liftgate.aware.codec.codecs.JsonRedisCodec
import io.liftgate.aware.codec.codecs.interpretation.AwareMessageCodec
import io.liftgate.aware.conversation.ConversationContinuation
import io.liftgate.aware.conversation.ConversationFactoryBuilder
import io.liftgate.aware.conversation.messages.ConversationMessage
import io.liftgate.aware.conversation.messages.ConversationMessageResponse
import io.liftgate.aware.message.AwareMessage
import io.liftgate.aware.thread.AwareThreadContext
import io.liftgate.aware.uri.WrappedAwareUri
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.lang.Thread.sleep
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.random.Random

/**
 * @author GrowlyX
 * @since 3/7/2022
 */
object AwareTest
{
    @Test
    @Disabled
    fun onPerformanceTest()
    {
        val gson = GsonBuilder()
            .setLongSerializationPolicy(LongSerializationPolicy.STRING)
            .create()

        // this can be called on your platform initialization,
        // or any major "module", or in context of Minecraft, a primary plugin.
        AwareHub.configure(
            WrappedAwareUri()
        )
        {
            gson
        }

        val runs = 1000

        val aware = AwareBuilder
            .of<AwareMessage>("perf")
            .codec(AwareMessageCodec)
            .build()

        val connection = measured {
            aware.internal().connect()
        }

        val sync = connection.second.sync()

        val hSet = measured {
            for (i in 0..runs)
            {
                sync.hset("lettuce-test", "$i", "horse!")
            }
        }

        val hGet = measured {
            for (i in 0..runs)
            {
                sync.hget("lettuce-test", "$i")
            }
        }

        println("Lettuce (sync):")
        println("  Connection: ${connection.first}")

        println("  HSET: ${hSet.first}ms")
        println("    avg: ${hSet.first / runs}ms")

        println("  HGET: ${hGet.first}ms")
        println("    avg: ${hGet.first / runs}ms")
    }

    fun <T> measured(lambda: () -> T): Pair<Long, T>
    {
        val start = System.currentTimeMillis()
        val value = lambda.invoke()

        return Pair(
            System.currentTimeMillis() - start,
            value
        )
    }

    @Test
    @Disabled
    fun testGeneric()
    {
        val gson = GsonBuilder()
            .setLongSerializationPolicy(LongSerializationPolicy.STRING)
            .create()

        // this can be called on your platform initialization,
        // or any major "module", or in context of Minecraft, a primary plugin.
        AwareHub.configure(
            WrappedAwareUri()
        )
        {
            gson
        }

        val aware = AwareBuilder
            .of<AwareMessage>("twitter.com/growlygg")
            // You can do this:
            .codec(AwareMessageCodec)
            // Or you can do this:
            .codec(JsonRedisCodec.of { it.packet })
            .build()

        aware.listen(this)

        aware.listen(
            "test",
            ExpiresIn(30L, TimeUnit.SECONDS)
        )
        {
            val horse = retrieve<String>("horse")

            println("Lets go, hasn't been 30s? $horse")
        }

        aware.connect().thenRun {
            launchInfinitePublisher(aware)
        }

        val conversationFactory = ConversationFactoryBuilder
            .of<ConversationMessageImpl, ConversationResponseImpl>()
            .channel("big-monkey")
            .timeout(2L, TimeUnit.SECONDS) {
                println("Lmao no response dam")
            }
            .response {
                ConversationResponseImpl(
                    "on god", it.uniqueId
                )
            }
            .receive { message, response ->
                println("Original msg: ${message.message}")
                println("Response: ${response.message}")

                return@receive ConversationContinuation.END
            }
            .build()

        conversationFactory.configure()
            .thenRun {
                thread {
                    while (true)
                    {
                        conversationFactory.distribute(
                            ConversationMessageImpl("Heyyy${Random.nextFloat()}")
                        )

                        sleep(1000L)
                    }
                }
            }

        while (true)
        {
            sleep(Long.MAX_VALUE)
        }
    }

    class ConversationResponseImpl(
        val message: String, uniqueId: UUID
    ) : ConversationMessageResponse(uniqueId)

    class ConversationMessageImpl(
        val message: String
    ) : ConversationMessage()

    fun launchInfinitePublisher(
        aware: io.liftgate.aware.Aware<AwareMessage>
    )
    {
        thread {
            while (true)
            {
                AwareMessage.of(
                    "test", aware,
                    "horse" to "heyy-${Random.nextFloat()}"
                ).publish(
                    // supplying our own thread context
                    AwareThreadContext.SYNC
                )

                sleep(1000L)
            }
        }
    }

    @Subscribe("test")
    @ExpiresIn(30L, TimeUnit.SECONDS)
    fun onTestExpiresIn30Seconds(
        message: AwareMessage
    )
    {
        val horse = message.retrieve<String>("horse")

        println("Hey! It's not been 30 seconds. :) ($horse)")
    }
}
