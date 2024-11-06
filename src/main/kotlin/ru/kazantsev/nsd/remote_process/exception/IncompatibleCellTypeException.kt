package ru.kazantsev.nsd.remote_process.exception

import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.Row
import ru.kazantsev.nsd.remote_process.Utilities

/**
 * Выкидывается при взаимодействии утилитарных методов с ячейками, содержащими недопустимые методы
 */
class IncompatibleCellTypeException(val row: Row, val columnIndex: Int) : RuntimeException() {
    constructor(cell: Cell) : this(cell.row, cell.columnIndex)
    override val message: String
        get() {
            return "Ячейка ${Utilities.getCellCoordinates(row, columnIndex)} содержит недопустимый тип: ${
                row.getCell(
                    columnIndex
                )
            }"
        }
}
