package ru.kazantsev.nsd.remote_process

import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import ru.kazantsev.nsd.json_rpc_connector.RpcResponseDto
import ru.kazantsev.nsd.remote_process.exception.*
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Содержит различные утилитарные методы
 */
@Suppress("unused")
class Utilities {
    companion object {

        //Константы:

        /**
         * Мапа сопоставления строк к булевым по умолчанию
         */
        @JvmStatic
        val DEFAULT_STRING_TO_BOOLEAN_CONVERT_MAP: MutableMap<String, Boolean> =
            mutableMapOf("да" to true, "нет" to false)

        /**
         * DateFormat для парсинга даты по умолчанию
         */
        @JvmStatic
        val DEFAULT_DATE_FORMAT: DateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss.ssss")

        //Параметры:

        /**
         * DateFormat, который будет парсить дату из строки (если в метод не был передан другой)
         */
        @JvmStatic
        var dateFormat: DateFormat = DEFAULT_DATE_FORMAT

        /**
         * Мапа сопоставления строк к булевым
         */
        @JvmStatic
        var stringToBooleanConvertMap: MutableMap<String, Boolean> = DEFAULT_STRING_TO_BOOLEAN_CONVERT_MAP

        //Общие утилитарные методы:

        /**
         * Проверить ответ jsonRpc на предмет ошибки
         * @param response проверяемый ответ
         * @param notNull признак, активирующий проверку на заполненность результата в ответе
         * @throws JsonRpcErrorResultException если в ответе ошибка или пустой результат
         */
        @JvmStatic
        fun checkRpcResponse(response: RpcResponseDto, notNull: Boolean = false): RpcResponseDto {
            if (response.error != null) throw JsonRpcErrorResultException(
                "При запросе через json rpc вернулась ошибка: " +
                        "код: ${response.error.code} " +
                        "сообщение: ${response.error.message}"
            )
            if (notNull && response.result == null) throw JsonRpcErrorResultException("При запросе через json rpc вернулся пустой ответ.")
            return response
        }

        /**
         * Получить строковое представление координат ячейки
         * @param cell ячейка
         * @return строковое представление координат ячейки
         */
        @JvmStatic
        fun getCellCoordinates(cell: Cell): String {
            return "${cell.rowIndex}:${cell.columnIndex}"
        }

        /**
         * Получить строковое представление координат ячейки
         * @param row строка
         * @param columnIndex индекс столбца
         * @return строковое представление координат ячейки
         */
        @JvmStatic
        fun getCellCoordinates(row: Row, columnIndex: Int): String {
            return "${row.rowNum}:${columnIndex}"
        }

        /**
         * Получить ячейку или выкинуть исключение
         * @param row строка, из которой получить
         * @param columnIndex индекс столбца
         * @return ячейка
         * @throws NoRequiredCellException если ячейка не существует
         */
        @JvmStatic
        fun getCellOrThrow(row: Row, columnIndex: Int): Cell {
            val cell = row.getCell(columnIndex)
            if (cell == null) throw NoRequiredCellException(row, columnIndex)
            else return cell
        }

        /**
         * Проверить ячейку на существование
         * @param cell проверяемая ячейка
         * @return та же самая ячейка
         * @throws NoRequiredCellException если не существует
         */
        @JvmStatic
        fun throwIfCellIsNull(cell: Cell?): Cell {
            if (cell == null) throw NoRequiredCellException()
            else return cell
        }

        /**
         * Получить или создать ячейку
         * @param row строка, из которой получить или создать
         * @param index индекс столбца
         * @return ячейка
         */
        @JvmStatic
        fun getOrCreateCell(row: Row, index: Int): Cell {
            val value = row.getCell(index)
            return value ?: row.createCell(index)
        }


        //ПОЛУЧЕНИЕ ЗНАЧЕНИЙ ЯЧЕЕК:
        //Не типизированное:

        /**
         * Получить не типизированное значение ячейки
         * @param row строка
         * @param columnIndex индекс столбца
         * @return значение ячейки
         * @throws NoCellTypeException если у ячейки нет типа
         * @throws ErrorCellException если ячейка содержит ошибку
         */
        @JvmStatic
        fun getCellValue(row: Row, columnIndex: Int): Optional<Any> {
            return getCellValue(row.getCell(columnIndex))
        }

        /**
         * Получить не типизированное значение ячейки
         * @param cell ячейка
         * @return значение ячейки
         * @throws NoCellTypeException если у ячейки нет типа
         * @throws ErrorCellException если ячейка содержит ошибку
         */
        @JvmStatic
        fun getCellValue(cell: Cell?): Optional<Any> {
            if (cell == null) return Optional.empty()
            return when (cell.cellType!!) {
                CellType.STRING -> {
                    val value = cell.stringCellValue
                    if (value.isNullOrBlank()) return Optional.empty()
                    return Optional.of(cell.stringCellValue)
                }

                CellType.BLANK -> Optional.empty()
                CellType.ERROR -> throw ErrorCellException(cell)
                CellType.BOOLEAN -> Optional.ofNullable(cell.booleanCellValue)
                CellType.NUMERIC -> Optional.ofNullable(cell.numericCellValue)
                CellType.FORMULA -> Optional.ofNullable(cell.cellFormula)
                CellType._NONE -> throw NoCellTypeException(cell)
            }
        }

        /**
         * Получить не типизированное значение ячейки или выкинуть исключение
         * @param row строка
         * @param columnIndex индекс столбца
         * @return значение ячейки
         * @throws NoRequiredCellException если ячейки не существует
         * @throws NoRequiredCellValueException если нет значения
         * @throws NoCellTypeException если у ячейки нет типа
         * @throws ErrorCellException если ячейка содержит исключение
         */
        @JvmStatic
        fun getCellValueElseThrow(row: Row, columnIndex: Int): Any {
            return getCellValueElseThrow(getCellOrThrow(row, columnIndex))
        }

        /**
         * Получить не типизированное значение ячейки или выкинуть исключение
         * @param cell ячейка
         * @return значение ячейки
         * @throws NoRequiredCellException если ячейки не существует
         * @throws NoRequiredCellValueException если нет значения
         * @throws NoCellTypeException если у ячейки нет типа
         * @throws ErrorCellException если ячейка содержит исключение
         */
        @JvmStatic
        fun getCellValueElseThrow(cell: Cell?): Any {
            return getCellValue(throwIfCellIsNull(cell)).orElseThrow { NoRequiredCellValueException(cell!!) }
        }

        //Получение даты:

        /**
         * Получить значение ячейки в виде даты
         * @param row строка
         * @param columnIndex индекс столбца
         * @param dateFormat формат даты, если нужно парсить строку
         * @return значение ячейки в виде даты
         * @throws ErrorCellException если в ячейке ошибка
         * @throws IncompatibleCellTypeException если ячейка содержит неподходящий тип данных
         */
        @JvmStatic
        fun getCellValueAsDate(row: Row, columnIndex: Int, dateFormat: DateFormat?): Optional<Date> {
            return getCellValueAsDate(row.getCell(columnIndex), dateFormat)
        }

        @JvmStatic
        fun getCellValueAsDate(row: Row, columnIndex: Int): Optional<Date> {
            return getCellValueAsDate(row, columnIndex, dateFormat)
        }

        /**
         * Получить значение ячейки в виде даты
         * @param cell ячейка
         * @param dateFormat формат даты, если нужно парсить строку
         * @return значение ячейки в виде даты
         * @throws ErrorCellException если в ячейке ошибка
         * @throws IncompatibleCellTypeException если ячейка содержит неподходящий тип данных
         */
        @JvmStatic
        fun getCellValueAsDate(cell: Cell?, dateFormat: DateFormat?): Optional<Date> {
            if (cell == null) return Optional.empty()
            return when (cell.cellType) {
                CellType.STRING -> {
                    val value = cell.stringCellValue
                    if (value.isNullOrBlank()) return Optional.empty()
                    return Optional.ofNullable((dateFormat ?: Companion.dateFormat).parse(value))
                }

                CellType.BLANK -> Optional.empty()
                CellType.ERROR -> throw ErrorCellException(cell)
                CellType.NUMERIC -> Optional.ofNullable(cell.dateCellValue)
                else -> throw IncompatibleCellTypeException(cell)
            }
        }

        @JvmStatic
        fun getCellValueAsDate(cell: Cell?): Optional<Date> {
            return getCellValueAsDate(cell, dateFormat)
        }

        /**
         * Получить значение ячейки в виде даты или выкинуть исключение
         * @param row строка
         * @param columnIndex индекс столбца
         * @param dateFormat формат даты, если нужно парсить строку
         * @return значение ячейки в виде даты
         * @throws ErrorCellException если в ячейке ошибка
         * @throws IncompatibleCellTypeException если ячейка содержит неподходящий тип данных
         * @throws NoRequiredCellException если ячейки не существует
         * @throws NoRequiredCellValueException если нет значения
         */
        @JvmStatic
        fun getCellValueAsDateElseThrow(row: Row, columnIndex: Int, dateFormat: DateFormat?): Date {
            return getCellValueAsDateElseThrow(getCellOrThrow(row, columnIndex), dateFormat)
        }

        @JvmStatic
        fun getCellValueAsDateElseThrow(row: Row, columnIndex: Int): Date {
            return getCellValueAsDateElseThrow(row, columnIndex, dateFormat)
        }

        /**
         * Получить значение ячейки в виде даты или выкинуть исключение
         * @param cell ячейка
         * @param dateFormat формат даты, если нужно парсить строку
         * @return значение ячейки в виде даты
         * @throws ErrorCellException если в ячейке ошибка
         * @throws IncompatibleCellTypeException если ячейка содержит неподходящий тип данных
         * @throws NoRequiredCellException если ячейки не существует
         * @throws NoRequiredCellValueException если нет значения
         */
        @JvmStatic
        fun getCellValueAsDateElseThrow(cell: Cell?, dateFormat: DateFormat?): Date {
            throwIfCellIsNull(cell)
            return getCellValueAsDate(cell, dateFormat).orElseThrow { NoRequiredCellValueException(cell!!) }
        }

        @JvmStatic
        fun getCellValueAsDateElseThrow(cell: Cell?): Date {
            return getCellValueAsDateElseThrow(cell, dateFormat)
        }

        //Получение чисел:

        /**
         * Получить значение ячейки как число
         * @param row строка
         * @param columnIndex индекс столбца
         * @return значение ячейки как число
         * @throws ErrorCellException если в ячейке ошибка
         * @throws IncompatibleCellTypeException если ячейка содержит неподходящий тип данных
         */
        @JvmStatic
        fun getCellValueAsNumeric(row: Row, columnIndex: Int): Optional<Double> {
            return getCellValueAsNumeric(row.getCell(columnIndex))
        }

        /**
         * Получить значение ячейки как число
         * @param cell ячейка
         * @return значение ячейки как число
         * @throws ErrorCellException если в ячейке ошибка
         * @throws IncompatibleCellTypeException если ячейка содержит неподходящий тип данных
         */
        @JvmStatic
        fun getCellValueAsNumeric(cell: Cell?): Optional<Double> {
            if (cell == null) return Optional.empty()
            return when (cell.cellType) {
                CellType.STRING -> {
                    val value = cell.stringCellValue
                    if (value.isNullOrBlank()) return Optional.empty()
                    return Optional.ofNullable(value.toDouble())
                }

                CellType.BLANK -> Optional.empty()
                CellType.ERROR -> throw ErrorCellException(cell)
                CellType.NUMERIC -> Optional.ofNullable(cell.numericCellValue)
                else -> throw IncompatibleCellTypeException(cell)
            }
        }

        /**
         * Получить значение ячейки как число или выкинуть исключение
         * @param row строка
         * @param columnIndex индекс столбца
         * @return значение ячейки как число
         * @throws ErrorCellException если в ячейке ошибка
         * @throws IncompatibleCellTypeException если ячейка содержит неподходящий тип данных
         * @throws NoRequiredCellException если ячейки не существует
         * @throws NoRequiredCellValueException если нет значения
         */
        @JvmStatic
        fun getCellValueAsNumericElseThrow(row: Row, columnIndex: Int): Double {
            return getCellValueAsNumericElseThrow(getCellOrThrow(row, columnIndex))
        }

        /**
         * Получить значение ячейки как число или выкинуть исключение
         * @param cell ячейка
         * @return значение ячейки как число
         * @throws ErrorCellException если в ячейке ошибка
         * @throws IncompatibleCellTypeException если ячейка содержит неподходящий тип данных
         * @throws NoRequiredCellException если ячейки не существует
         * @throws NoRequiredCellValueException если нет значения
         */
        @JvmStatic
        fun getCellValueAsNumericElseThrow(cell: Cell?): Double {
            throwIfCellIsNull(cell)
            return getCellValueAsNumeric(cell).orElseThrow { NoRequiredCellValueException(cell!!) }
        }

        //Получение логического:

        /**
         * Получить значение ячейки как булево
         * @param row строка
         * @param columnIndex индекс столбца
         * @param stringConvertMap мапа для сопоставления строк с булевым при парсинге строк
         * @return значение ячейки как булево
         * @throws ErrorCellException если в ячейке ошибка
         * @throws IncompatibleCellTypeException если ячейка содержит неподходящий тип данных
         * @throws StringToBooleanConvertException если не удалось подобрать значение при парсинге строки
         */
        @JvmStatic
        fun getCellValueAsBoolean(
            row: Row,
            columnIndex: Int,
            stringConvertMap: Map<String, Boolean>?
        ): Optional<Boolean> {
            return getCellValueAsBoolean(row.getCell(columnIndex), stringConvertMap)
        }

        @JvmStatic
        fun getCellValueAsBoolean(row: Row, columnIndex: Int): Optional<Boolean> {
            return getCellValueAsBoolean(row, columnIndex, stringToBooleanConvertMap)
        }

        /**
         * Получить значение ячейки как булево
         * @param cell ячейка
         * @param stringConvertMap мапа для сопоставления строк с булевым при парсинге строк
         * @return значение ячейки как булево
         * @throws ErrorCellException если в ячейке ошибка
         * @throws IncompatibleCellTypeException если ячейка содержит неподходящий тип данных
         * @throws StringToBooleanConvertException если не удалось подобрать значение при парсинге строки
         */
        @JvmStatic
        fun getCellValueAsBoolean(
            cell: Cell?,
            stringConvertMap: Map<String, Boolean>?
        ): Optional<Boolean> {
            if (cell == null) return Optional.empty()
            return when (cell.cellType) {
                CellType.BOOLEAN -> Optional.ofNullable(cell.booleanCellValue)
                CellType.STRING -> {
                    val value = cell.stringCellValue?.lowercase()
                    if (value.isNullOrBlank()) return Optional.empty()
                    val map = stringConvertMap ?: DEFAULT_STRING_TO_BOOLEAN_CONVERT_MAP
                    val converted = map[value] ?: throw StringToBooleanConvertException(cell)
                    return Optional.of(converted)
                }

                CellType.BLANK -> Optional.empty()
                CellType.ERROR -> throw ErrorCellException(cell)
                CellType.NUMERIC -> {
                    val value = cell.numericCellValue
                    return Optional.of(value > 0)
                }

                else -> throw IncompatibleCellTypeException(cell)
            }
        }

        @JvmStatic
        fun getCellValueAsBoolean(cell: Cell?): Optional<Boolean> {
            return getCellValueAsBoolean(cell, stringToBooleanConvertMap)
        }

        /**
         * Получить значение ячейки как булево или выкинуть исключение
         * @param row строка
         * @param columnIndex индекс столбца
         * @param stringConvertMap мапа для сопоставления строк с булевым при парсинге строк
         * @return значение ячейки как булево
         * @throws ErrorCellException если в ячейке ошибка
         * @throws IncompatibleCellTypeException если ячейка содержит неподходящий тип данных
         * @throws StringToBooleanConvertException если не удалось подобрать значение при парсинге строки
         * @throws NoRequiredCellException если ячейки не существует
         * @throws NoRequiredCellValueException если нет значения
         */
        @JvmStatic
        fun getCellValueAsBooleanElseThrow(
            row: Row,
            columnIndex: Int,
            stringConvertMap: Map<String, Boolean>?
        ): Boolean {
            return getCellValueAsBooleanElseThrow(getCellOrThrow(row, columnIndex), stringConvertMap)
        }

        @JvmStatic
        fun getCellValueAsBooleanElseThrow(row: Row, columnIndex: Int): Boolean {
            return getCellValueAsBooleanElseThrow(row, columnIndex, stringToBooleanConvertMap)
        }

        /**
         * Получить значение ячейки как булево или выкинуть исключение
         * @param cell ячейка
         * @param stringConvertMap мапа для сопоставления строк с булевым при парсинге строк
         * @return значение ячейки как булево
         * @throws ErrorCellException если в ячейке ошибка
         * @throws IncompatibleCellTypeException если ячейка содержит неподходящий тип данных
         * @throws StringToBooleanConvertException если не удалось подобрать значение при парсинге строки
         * @throws NoRequiredCellException если ячейки не существует
         * @throws NoRequiredCellValueException если нет значения
         */
        @JvmStatic
        fun getCellValueAsBooleanElseThrow(
            cell: Cell?,
            stringConvertMap: Map<String, Boolean>?
        ): Boolean {
            throwIfCellIsNull(cell)
            return getCellValueAsBoolean(cell, stringConvertMap).orElseThrow { NoRequiredCellValueException(cell!!) }
        }

        @JvmStatic
        fun getCellValueAsBooleanElseThrow(cell: Cell?): Boolean {
            return getCellValueAsBooleanElseThrow(cell, stringToBooleanConvertMap)
        }

        //Получение справочника yes/no:

        //TODO

        //Получение строки

        //TODO
    }
}
