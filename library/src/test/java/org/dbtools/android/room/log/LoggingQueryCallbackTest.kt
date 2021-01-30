package org.dbtools.android.room.log

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class LoggingQueryCallbackTest {

    private val queryContext = "Test"
    private val loggingQueryCallback = LoggingQueryCallback(queryContext)

    private fun formatOutput(sql: String) = "$queryContext Query: [$sql]"

    @Test
    fun testSimpleQuery() {
        val bindingArgs = listOf<Any>()
        val sqlQuery = "SELECT * FROM Individual"

        loggingQueryCallback.onQuery(sqlQuery, bindingArgs)
        Assertions.assertEquals(formatOutput("SELECT * FROM Individual"), loggingQueryCallback.lastLog)
    }

    @Test
    fun testSingleIntArgQuery() {
        val bindingArgs = listOf<Any>(1234)
        val sqlQuery = "SELECT * FROM Individual WHERE id = ?"

        loggingQueryCallback.onQuery(sqlQuery, bindingArgs)
        Assertions.assertEquals(formatOutput("SELECT * FROM Individual WHERE id = 1234"), loggingQueryCallback.lastLog)
    }

    @Test
    fun testSingleFloatArgQuery() {
        val bindingArgs = listOf<Any>(10.5f)
        val sqlQuery = "SELECT * FROM Individual WHERE cost > ?"

        loggingQueryCallback.onQuery(sqlQuery, bindingArgs)
        Assertions.assertEquals(formatOutput("SELECT * FROM Individual WHERE cost > 10.5"), loggingQueryCallback.lastLog)
    }

    @Test
    fun testSingleStringArgQuery() {
        val bindingArgs = listOf<Any>("1234")
        val sqlQuery = "SELECT * FROM Individual WHERE id = ?"

        loggingQueryCallback.onQuery(sqlQuery, bindingArgs)
        Assertions.assertEquals(formatOutput("SELECT * FROM Individual WHERE id = '1234'"), loggingQueryCallback.lastLog)
    }

    @Test
    fun testSingleBooleanArgQuery() {
        val bindingArgs = listOf<Any>(true)
        val sqlQuery = "SELECT * FROM Individual WHERE enabled = ?"

        loggingQueryCallback.onQuery(sqlQuery, bindingArgs)
        Assertions.assertEquals(formatOutput("SELECT * FROM Individual WHERE enabled = 1"), loggingQueryCallback.lastLog)
    }

    @Test
    fun testSingleBlobArgQuery() {
        val bindingArgs = listOf<Any>(ByteArray(1))
        val sqlQuery = "SELECT * FROM Individual WHERE data = ?"

        loggingQueryCallback.onQuery(sqlQuery, bindingArgs)
        Assertions.assertEquals(formatOutput("SELECT * FROM Individual WHERE data = BLOB"), loggingQueryCallback.lastLog)
    }

    @Test
    fun testMultipleArgsQuery() {
        val bindingArgs = listOf<Any>("1234", 10)
        val sqlQuery = "SELECT * FROM Individual WHERE id = ? LIMIT ?"

        loggingQueryCallback.onQuery(sqlQuery, bindingArgs)
        Assertions.assertEquals(formatOutput("SELECT * FROM Individual WHERE id = '1234' LIMIT 10"), loggingQueryCallback.lastLog)
    }

    @Test
    fun testEdgeCaseQuery1() {
        // this tests a failure when trying to use String.format

        val bindingArgs = listOf<Any>(10)
        val sqlQuery = "SELECT * FROM Individual WHERE name LIKE '%' AND age > ?"

        loggingQueryCallback.onQuery(sqlQuery, bindingArgs)
        Assertions.assertEquals(formatOutput("SELECT * FROM Individual WHERE name LIKE '%' AND age > 10"), loggingQueryCallback.lastLog)
    }
}