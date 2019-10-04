package ru.experiment.retry

import org.junit.jupiter.api.extension.*
import kotlin.reflect.KClass

class RetryTestContext(private val currentRetry: Int,
                       private val totalRetry: Int,
                       private val exceptions: List<KClass<out Throwable>>,
                       private val displayName: String) : TestTemplateInvocationContext {
    override fun getDisplayName(invocationIndex: Int): String {
        return this.displayName
    }

    override fun getAdditionalExtensions(): MutableList<Extension> {
        return mutableListOf(DefaultRetryInfo(currentRetry, totalRetry, exceptions), DefaultExecutionCondition(currentRetry, totalRetry))
    }
}

class DefaultRetryInfo(private val currentRetry: Int, private val totalRetry: Int, private val errors: List<KClass<out Throwable>>) : ParameterResolver {
    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext) = (parameterContext.parameter.type == RetryInfo::class.java)

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext) = MyDefaultRepetitionInfo()

    inner class MyDefaultRepetitionInfo : RetryInfo {
        override val current: Int
            get() = currentRetry
        override val total: Int
            get() = totalRetry
        override val exceptions: List<KClass<out Throwable>>
            get() = errors

        override fun toString(): String {
            return StringBuilder(this::class.simpleName)
                    .append("currentRepetition", currentRetry)
                    .append("totalRepetitions", totalRetry)
                    .toString()
        }
    }
}

class DefaultExecutionCondition(private val currentRetry: Int, private val totalRetry: Int) : ExecutionCondition {
    override fun evaluateExecutionCondition(context: ExtensionContext?): ConditionEvaluationResult {
        return when {
            totalRetry < 1 -> ConditionEvaluationResult.disabled("Count of retry must be more then 0")
            currentRetry > totalRetry -> ConditionEvaluationResult.disabled("Current retry is great then total retry")
            else -> ConditionEvaluationResult.enabled("Retry the test")
        }
    }
}