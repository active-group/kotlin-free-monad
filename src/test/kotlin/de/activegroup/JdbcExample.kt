package de.activegroup

import java.util.stream.Collectors

class JdbcTemplate {
    fun execute(sql: String): Unit = TODO()
    fun batchUpdate(sql: String, batchArgs: List<Array<Any>>)
            : Array<Int> = TODO()
    fun <T> query(sql: String, args: Array<Any>, rowMapper: (Row, Int) -> T)
            : List<T> = TODO()
}

typealias JdbcComputation<A> = Reader<JdbcTemplate, A>

class Row {
    fun getLong(columnName: String): Long = TODO()
    fun getString(columnName: String): String = TODO()

}

class JdbcDsl : ReaderDsl<JdbcTemplate>()  {
    suspend fun execute(sql: String): Unit =
        ask().execute(sql)
    suspend fun batchUpdate(sql: String, batchArgs: List<Array<Any>>)
            : Array<Int> = ask().batchUpdate(sql, batchArgs)
    suspend fun <T> query(sql: String, args: Array<Any>, rowMapper: (Row, Int) -> T)
            : List<T> = ask().query(sql, args, rowMapper)
}

val Jdbc = JdbcDsl()

fun  <A> jdbc(block: suspend JdbcDsl.() -> A): JdbcComputation<A> =
    Reader.reader { Jdbc.block() }

class Log {
    fun info(message: String): Unit = TODO()
}

val log = Log()

data class Customer(val id: Long, val firstName: String, val lastName: String)
val computation: JdbcComputation<Unit> =
    jdbc {
        execute("DROP TABLE customers IF EXISTS")
        execute(
            "CREATE TABLE customers(" +
                    "id SERIAL, first_name VARCHAR(255), last_name VARCHAR(255))"
        )
        val splitUpNames = listOf("John Woo", "Jeff Dean", "Josh Bloch", "Josh Long").stream()
            .map { name -> name.split(" ") }
            .collect(Collectors.toList())
            .map { it.toTypedArray<Any>() }
        batchUpdate("INSERT INTO customers(first_name, last_name) VALUES (?,?)", splitUpNames.toList())
        val customers = query(
                "SELECT id, first_name, last_name FROM customers WHERE first_name = ?",
                arrayOf("Josh"))
            { rs, rowNum -> Customer(rs.getLong("id"), rs.getString("first_name"), rs.getString("last_name")) }
        customers.forEach { customer -> log.info(customer.toString()) }
        pure(Unit)
    }

