package ru.kazantsev.nsd.remote_process.exception

/**
 * Выкидывается методами проверки ответа jsonRpc.
 */
class JsonRpcErrorResultException(message: String) : RuntimeException(message)
