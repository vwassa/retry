package ru.experiment.retry

import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.platform.commons.util.Preconditions
import kotlin.reflect.KClass

const val CURRENT_REPETITION_PLACEHOLDER = "{currentRepetition}"
const val TOTAL_REPETITIONS_PLACEHOLDER = "{totalRepetitions}"
const val DISPLAY_NAME_PLACEHOLDER = "{displayName}"
const val SHORT_DISPLAY_NAME = "retry: $CURRENT_REPETITION_PLACEHOLDER of $TOTAL_REPETITIONS_PLACEHOLDER"
const val LONG_DISPLAY_NAME = "$DISPLAY_NAME_PLACEHOLDER :: $SHORT_DISPLAY_NAME"

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ExtendWith(RetryTestExtension::class)
@TestTemplate
annotation class Retry(val value: Int = 1,
                       val exceptions: Array<KClass<out Throwable>> = [Throwable::class],
                       val sleep: Long = 0,
                       val name: String = SHORT_DISPLAY_NAME
)

fun Retry.format(methodName: String, currentRetry: Int, totalRetry: Int): String {
    val pattern = Preconditions.notBlank(this.name.trim()) {
        "Configuration error: @Retry on method $methodName must be declared with a non-empty name."
    }
    return pattern
            .replace(DISPLAY_NAME_PLACEHOLDER, methodName)
            .replace(CURRENT_REPETITION_PLACEHOLDER, currentRetry.toString())
            .replace(TOTAL_REPETITIONS_PLACEHOLDER, totalRetry.toString())
}

interface RetryInfo {
    val current: Int
    val total: Int
    val exceptions: List<KClass<out Throwable>>
}