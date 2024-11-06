package ru.kazantsev.nsd.remote_process.exception

import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.Row
import ru.kazantsev.nsd.remote_process.Utilities

class NoRequiredCellValueException(val row: Row, val columnIndex: Int) : RuntimeException() {
    constructor(cell: Cell) : this(cell.row, cell.columnIndex)
    override val message: String
        get() {
            return "Отсутствует значение в ячейке ${Utilities.getCellCoordinates(row, columnIndex)}"
        }
}
