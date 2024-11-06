package ru.kazantsev.nsd.remote_process.exception

import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.Row
import ru.kazantsev.nsd.remote_process.Utilities

/**
 * Выкидывается если у читаемой утилитарными методами ячейки отсутствует тип
 */
class NoCellTypeException (val row: Row, val columnIndex: Int) : RuntimeException(){
    constructor(cell: Cell) : this(cell.row, cell.columnIndex)
    override val message: String
        get() {
            return "У ячейки ${Utilities.getCellCoordinates(row, columnIndex)} нет типа"
        }
}

