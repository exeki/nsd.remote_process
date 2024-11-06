package ru.kazantsev.nsd.remote_process.exception

import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.Row
import ru.kazantsev.nsd.remote_process.Utilities

/**
 * Выкидывается при попытке прочитать ячейку с ошибкой
 */
class StringToBooleanConvertException(val row: Row, val columnIndex: Int) : RuntimeException() {
    constructor(cell: Cell) : this(cell.row, cell.columnIndex)

    override val message: String
        get() {
            return "Не удалось сопоставить строковое значение ячейки " +
                    "${Utilities.getCellCoordinates(row, columnIndex)} с картой значений."
        }
}
