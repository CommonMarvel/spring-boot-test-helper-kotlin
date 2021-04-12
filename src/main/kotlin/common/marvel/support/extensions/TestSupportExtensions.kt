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
import io.kotlintest.shouldThrow

val prettyPrinter = DefaultPrettyPrinter().apply {
    DefaultIndenter("  ", "\n").also {
        indentArraysWith(it)
        indentObjectsWith(it)
    }
}

inline fun errCodeMatches(errorCode: ErrorCode, block: () -> Unit) {
    val businessException = shouldThrow<BusinessException> { block() }
    if (businessException.errorCode != errorCode) {
        throw Failures.failure("Expected errorCode $errorCode but got ${businessException.errorCode}")
    }
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
