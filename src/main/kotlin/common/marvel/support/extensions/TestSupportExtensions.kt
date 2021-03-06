package common.marvel.support.extensions

import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.karumi.kotlinsnapshot.KotlinSnapshot
import common.marvel.helper.exception.BusinessException
import common.marvel.helper.exception.ErrorCode
import common.marvel.helper.extension.jsonStringToMap
import common.marvel.helper.extension.toJsonString
import common.marvel.helper.global.Global
import io.kotlintest.Failures
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import java.lang.reflect.InvocationTargetException

val prettyPrinter = DefaultPrettyPrinter().apply {
    DefaultIndenter("  ", "\n").also {
        indentArraysWith(it)
        indentObjectsWith(it)
    }
}

inline fun errCodeMatches(errorCode: ErrorCode, block: () -> Unit) {
    val expectedExceptionClass = BusinessException::class
    val exception = shouldThrow<Exception> { block() }
    val actualErrorCode = when {
        exception is BusinessException -> exception.errorCode
        exception is InvocationTargetException && exception.cause is BusinessException -> (exception.cause as BusinessException).errorCode // Get errorCode of BusinessException thrown by invoked Method
        else -> throw Failures.failure("Expected exception ${expectedExceptionClass.qualifiedName} but a ${exception::class.simpleName} was thrown instead.", exception)
    }
    actualErrorCode shouldBe errorCode
}

fun Any?.toMatchSnapshot(
    snapshotName: String? = null,
    expectAny: Any? = null
) {
    val kotlinSnapshot = KotlinSnapshot(testClassAsDirectory = true, snapshotsFolder = "src/test")
    if (this is String) {
        kotlinSnapshot.matchWithSnapshot(snapshotName = snapshotName, value = this)
        return
    }

    val printer = Global.objectMapper
        .setDefaultPrettyPrinter(prettyPrinter)
        .writerWithDefaultPrettyPrinter()

    val result = if (this is Collection<*>) {
        val content = this.joinToString("\n") {
            val valueMap = it
                .toJsonString()
                .jsonStringToMap(String::class.java, Any::class.java)
                .toSortedMap()

            printer.writeValueAsString(valueMap)
        }
        "[\n$content\n]"
    } else {
        val expectAnyValue = expectAny?.toJsonString()?.jsonStringToMap() ?: HashMap()
        val valueMap = this.toJsonString().jsonStringToMap(String::class.java, Any::class.java).toMutableMap()
        expectAnyValue.asSequence()
            .distinct()
            .forEach {
                valueMap[it.key] = it.value
            }
        printer.writeValueAsString(valueMap)
    }

    kotlinSnapshot.matchWithSnapshot(snapshotName = snapshotName, value = result)
}
