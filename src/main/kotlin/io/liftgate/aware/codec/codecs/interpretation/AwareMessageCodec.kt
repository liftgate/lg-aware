package io.liftgate.aware.codec.codecs.interpretation

import io.liftgate.aware.codec.codecs.JsonRedisCodec
import io.liftgate.aware.message.AwareMessage

/**
 * A default implementation for [JsonRedisCodec]
 * providing an [AwareMessage] value type.
 *
 * @author GrowlyX
 * @since 3/7/2022
 */
object AwareMessageCodec : JsonRedisCodec<AwareMessage>(AwareMessage::class)
{
    override fun getPacketId(v: AwareMessage) = v.packet
}
