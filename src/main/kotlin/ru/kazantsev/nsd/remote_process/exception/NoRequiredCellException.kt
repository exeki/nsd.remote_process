package ru.kazantsev.nsd.remote_process.exception

import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.Row
import ru.kazantsev.nsd.remote_process.Utilities

/**
 * Выкидывается утилитарными методами если ячейка не существует
 */
class NoRequiredCellException(message: String) : RuntimeException(message) {
    companion object {
        fun getText(row: Row, columnIndex: Int): String {
            return "Отсутствует ячейка ${Utilities.getCellCoordinates(row, columnIndex)} " +
                    "(ячейка не инициализирована, вероятно отсутствует значение)"
        }
    }

    constructor() : this("Передана null ячейка.")
    constructor(row: Row, columnIndex: Int) : this(getText(row, columnIndex))
    constructor(cell: Cell) : this(cell.row, cell.columnIndex)

}
