package ru.kazantsev.nsd.remote_process.exception

/**
 * Это исключение нужно выкидывать в функции обработки строки в методах типа DocProcessor.process...(),
 * для того, что бы ваша ошибка записалась без указания типа исключения. Но вы можете выкидывать и другие исключения, просто будет
 * отличаться текст ошибки в файле результата.
 */
open class PlannedException(message : String) : Exception(message)
