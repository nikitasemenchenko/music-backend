package ru.magnum.data.database

data class DatabaseSettings(
    val jdbcUrl: String,
    val username: String,
    val password: String,
    val driverClassName: String,
    val maximumPoolSize: Int
)