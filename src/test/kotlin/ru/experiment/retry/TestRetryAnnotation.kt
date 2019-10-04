package ru.experiment.retry

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.TestTemplate

class TestRetryAnnotation {
    @Tag("NotTestTemplate")
    @Retry(value = 2)
    fun test1(retryInfo: RetryInfo){
        println("Current retry = ${retryInfo.current} Total retry = ${retryInfo.total} Exceptions = ${retryInfo.exceptions}")
    }
    @Tag("WithTestTemplate")
    @Retry(value = 2)
    @TestTemplate
    fun test2(retryInfo: RetryInfo){
        println("Current retry = ${retryInfo.current} Total retry = ${retryInfo.total} Exceptions = ${retryInfo.exceptions}")
    }

}