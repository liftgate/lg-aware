package io.liftgate.aware.subscription

/**
 * Handles the internal invocation
 * of context [C] when given a message `V`.
 *
 * @author GrowlyX
 * @since 3/8/2022
 */
interface AwareSubscriptionContextType<C>
{
    fun <V : Any> launch(
        c: AwareSubscriptionContext<C>, v: V
    )

    /**
     * Wonky solution to generic-related issues.
     */
    @Suppress("UNCHECKED_CAST")
    fun <V : Any> launchCasted(c: Any, v: V)
    {
        launch(c as AwareSubscriptionContext<C>, v)
    }
}
