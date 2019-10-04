package ru.experiment.retry

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler
import org.junit.jupiter.api.extension.TestTemplateInvocationContext
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider
import org.junit.platform.commons.logging.LoggerFactory
import org.junit.platform.commons.util.AnnotationUtils
import org.junit.platform.commons.util.Preconditions
import org.opentest4j.TestAbortedException
import java.lang.reflect.Method
import java.util.*
import java.util.stream.Stream
import java.util.stream.StreamSupport

private val logger = LoggerFactory.getLogger(RetryTestExtension::class.java)

fun <T> Iterator<T>.asStream(characteristic: Int = Spliterator.ORDERED, parallel: Boolean = false): Stream<T> {
    val spliterator = Spliterators.spliteratorUnknownSize(this, characteristic)
    return StreamSupport.stream(spliterator, parallel)
}

class RetryTestExtension : TestTemplateInvocationContextProvider, TestExecutionExceptionHandler {
    //executionContext inforamtion
    private lateinit var methodName: String
    //Annotation information
    private val annotation = Retry::class.java
    private lateinit var retryTest: Retry
    private val annInfo: AnnotationInfo
        get() = AnnotationInfo(retryTest, methodName)
    private val displayName: String
        get() = retryTest.format(methodName, currentRetry, annInfo.total)
    //counters
    private var currentRetry = 1
    private var errorRetry = 0


    override fun supportsTestTemplate(context: ExtensionContext) = AnnotationUtils.isAnnotated(context.testMethod, annotation)

    override fun provideTestTemplateInvocationContexts(context: ExtensionContext): Stream<TestTemplateInvocationContext> {
        val testMethod = context.requiredTestMethod
        methodName = context.displayName
        retryTest = AnnotationUtils.findAnnotation(testMethod, annotation).get()
        return RetryTestContextIterator().asStream()
    }

    override fun handleTestExecutionException(context: ExtensionContext, throwable: Throwable) {
        val testMethod = context.requiredTestMethod
        if (testMethod.isAnnotationPresent(annotation)) {
            if (throwable::class in annInfo.exceptions.toList()) {
                errorRetry++
                logger.warn { "I've caught next error:\n ${throwable.message}" }
                if (currentRetry > annInfo.total) {
                    throw throwable
                } else {
                    throw TestAbortedException("Do not fail completely, but repeat the test", throwable)
                }
            } else {
                logger.error { "This error doesn't include in ${annInfo.exceptions}" }
                throw throwable
            }
        }
    }

    private fun totalRepetitions(retry: Retry, method: Method): Int {
        val repetitions = retry.value
        Preconditions.condition(repetitions > 0) {
            """Configuration error: @Retry on method [$method] must be declared with a positive "value"."""
        }
        return repetitions
    }

    inner class RetryTestContextIterator : Iterator<TestTemplateInvocationContext> {
        override fun hasNext(): Boolean {
            if (currentRetry != 1) Thread.sleep(annInfo.sleep)
            return (currentRetry - errorRetry == 1) && (currentRetry <= annInfo.total)
        }

        override fun next(): RetryTestContext {
            val context = RetryTestContext(currentRetry = currentRetry, totalRetry = annInfo.total, exceptions = annInfo.exceptions, displayName = displayName)
            currentRetry++
            return context
        }
    }

    class AnnotationInfo(private val annotation: Retry, private val methodName: String) {
        val total: Int
            get() {
                val repetitions = annotation.value
                Preconditions.condition(repetitions > 0) {
                    """Configuration error: @Retry on method [$methodName] must be declared with a positive "value"."""
                }
                return repetitions
            }
        val name = annotation.name
        val sleep = annotation.sleep
        val exceptions = annotation.exceptions.toList()
    }
}