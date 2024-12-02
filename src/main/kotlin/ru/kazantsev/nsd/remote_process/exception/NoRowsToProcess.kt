package ru.kazantsev.nsd.remote_process.exception

import ru.kazantsev.nsd.remote_process.DocProcessor

/**
 * Выкидывается при попытке прочитать ячейку с ошибкой
 */
class NoRowsToProcess(val docProcessor: DocProcessor) : RuntimeException() {
    override val message: String
        get() {
            return "Не найдено строк к обработке на листе ${docProcessor.sheet.sheetName} документа ${docProcessor.file.name}"
        }
}
