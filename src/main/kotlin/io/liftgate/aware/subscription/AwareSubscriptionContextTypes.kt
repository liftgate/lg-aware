package io.liftgate.aware.subscription

import java.lang.reflect.Method

/**
 * Defines several [AwareSubscriptionContextType]
 * types.
 *
 * @author GrowlyX
 * @since 3/8/2022
 */
enum class AwareSubscriptionContextTypes(
    val contextType: AwareSubscriptionContextType<*>
)
{
    METHOD(object : AwareSubscriptionContextType<Method>
    {
        override fun <V : Any> launch(
            c: AwareSubscriptionContext<Method>, v: V
        )
        {
            c.context.invoke(c.caller, v)
        }
    }),

    LAMBDA(object : AwareSubscriptionContextType<(Any) -> Unit>
    {
        override fun <V : Any> launch(
            c: AwareSubscriptionContext<(Any) -> Unit>, v: V
        )
        {
            c.context.invoke(v)
        }
    });

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Any> asT(): AwareSubscriptionContextType<T>
    {
        return contextType as AwareSubscriptionContextType<T>
    }
}
