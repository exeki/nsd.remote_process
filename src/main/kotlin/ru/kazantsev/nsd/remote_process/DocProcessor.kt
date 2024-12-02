package ru.kazantsev.nsd.remote_process

import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.kazantsev.nsd.remote_process.exception.NoRowsToProcess

import java.io.File
import java.io.FileOutputStream
import java.util.function.Function

import kotlin.math.roundToLong

import ru.kazantsev.nsd.remote_process.exception.PlannedException

/**
 * Обработчик файлов.
 * Читает файл, ставит отметки о выполнении в файле.
 */
@Suppress("MemberVisibilityCanBePrivate", "unused", "CanBePrimaryConstructorProperty")
open class DocProcessor
/**
 * Основной конструктор
 * @param path путь да обрабатываемого файла
 * @param markCellIndex индекс столбца для отметки, будет использован этот столбец и следующий
 */
    (path: String, markCellIndex: Int) {
    /** Индекс столбца для отметки, будет использован этот столбец и следующий */
    val markCellIndex: Int = markCellIndex

    /** Логгер обыкновенный */
    val logger: Logger = LoggerFactory.getLogger(DocProcessor::class.java)!!

    /** Путь до обрабатываемого файлы */
    val path: String = path

    /** Обрабатываемый файл */
    val file: File = File(path)

    /** Workbook собранный из обрабатываемого файла. Из него считывается информация и в него вносятся отметки о выполнении. */
    val workbook: Workbook = XSSFWorkbook(path)

    /** Первый лист воркбука, он же будет обрабатываться. */
    val sheet: Sheet = workbook.getSheetAt(0)

    /** Перечень строк к обработке. Будется только строки, у которых в столбце для отметки либо пусто, либо false. */
    var rowsToProcess: MutableList<Row> = mutableListOf()
        protected set

    /** Текущий инндекс обработки строк */
    var currentRowIndex = 0
        protected set

    /** Общее количество строк к обработке */
    var count = 0
        protected set

    /** Количество успешно обработанных строк */
    var successCount = 0
        protected set

    /** Количество обработанных с ошибкой строк */
    var errorsCount = 0
        protected set

    /** Путь до папки, куда сложить результат обработки */
    var outDirPath: String? = null
        protected set

    /** Файл с результатом обработки */
    var outFile: File? = null
        protected set

    init {
        logger.info("Сборка нового DocProcessor. Читаю файл: ${file.path}")
        val rowIt = sheet.rowIterator()
        logger.info("Лист: ${sheet.sheetName}")
        logger.info("Строк на листе: ${sheet.lastRowNum}")

        val headRow = rowIt.next()
        Utilities.getOrCreateCell(headRow, markCellIndex).setCellValue("Отметка о успешном выполнении")
        Utilities.getOrCreateCell(headRow, markCellIndex + 1).setCellValue("Сообщение")
        logger.debug("Собираю строки для обработки")
        while (rowIt.hasNext()) {
            val row = rowIt.next()
            val markCell = Utilities.getOrCreateCell(row, markCellIndex)
            val cellType = markCell.cellType
            logger.debug("Проверяю строку {}, тип ячейки отметки {}", row.rowNum, cellType)
            Utilities.getOrCreateCell(row, markCellIndex + 1)
            val cellIsBlank = cellType == CellType.BLANK
            logger.debug("cellIsBlank: $cellIsBlank")
            val cellIsBooleanAndFalse = cellType == CellType.BOOLEAN && !markCell.booleanCellValue
            logger.debug("cellIsBooleanAndFalse: $cellIsBooleanAndFalse")
            val cellIsStringAndIsBlank = cellType == CellType.STRING && (markCell.stringCellValue == null || markCell.stringCellValue.isEmpty())
            logger.debug("cellIsStringAndIsBlank: $cellIsStringAndIsBlank")

            if (cellIsBlank || cellIsBooleanAndFalse || cellIsStringAndIsBlank) {
                logger.debug("Добавляем строку к обработке")
                rowsToProcess.add(row)
            } else logger.debug("Не добавляем строку в обработке")
        }
        count = rowsToProcess.size
        if(count == 0) {
            val e = NoRowsToProcess(this)
            logger.error(e.message)
            throw e
        }
        logger.info("Файл прочтен, DocProcessor собран. Строк к обработке: $count")
    }

    /**
     * Записать данные о результате. Данные будут записаны в отдельный файл с постфиксом _processed,
     * созданный либо по указанному outDirPath, либо в той же папке, что и прочтенный файл.
     */
    fun write() {

        if (outDirPath != null) {
            val file = File(outDirPath!!)
            if (!file.exists()) {
                logger.info("Создаю папку для записи результата: $outDirPath")
                file.mkdirs()
            }
        }

        val outFilePath = if (outDirPath == null) path + "_processed.xlsx"
        else outDirPath + "//" + file.name + "_processed.xlsx"

        outFile = File(outFilePath)

        logger.info("Записываю данные в файл: $outFile")

        val out = FileOutputStream(outFile!!)
        workbook.write(out)
        out.close()
        logger.info("Результат записан")
    }

    /**
     * Получить процент завершения процесса
     */
    protected fun getProgress(): Double {
        val value = if (count <= 1) 0.0
        else ((currentRowIndex.toDouble() / (count - 1)) * 10000).roundToLong().toDouble() / 100
        return if (value > 100) 100.0
        else value
    }

    /**
     * Установить результат с ошибкой на строке
     * @param row строка
     * @param message сообщение об ошибке
     */
    fun setRowError(row: Row, message: String) {
        logger.info("Прогресс ${getProgress()}%: ошибка: $message")
        row.getCell(markCellIndex).setCellValue(false)
        row.getCell(markCellIndex + 1).setCellValue(message)
        errorsCount += 1
    }

    /**
     * Установить результат с ошибкой на строке
     * @param row строка
     * @param e исключение
     */
    fun setRowError(row: Row, e: Exception) {
        if (e is PlannedException) setRowError(row, e.message!!)
        else setRowError(row, "${e.javaClass.simpleName}: ${e.message}")
    }

    /**
     * Установить успешный результат обработки по строке
     * @param row строка по которой результат
     * @param message текст сообщения
     */
    fun setRowSuccess(row: Row, message: String = "") {
        logger.info("Прогресс ${getProgress()}%. успешно обработано: $message")
        row.getCell(markCellIndex).setCellValue(true)
        row.getCell(markCellIndex + 1)?.setCellValue(message)
        successCount += 1
    }

    /**
     * Проверить, есть ли еще строки и обработке
     */
    operator fun hasNext(): Boolean {
        return currentRowIndex < rowsToProcess.size
    }

    /**
     * Получить следующую строку без лога
     */
    protected fun nextNoLog(): Row {
        val row = rowsToProcess[currentRowIndex]
        currentRowIndex += 1
        return row
    }

    /**
     * Получить следующую строку
     */
    fun next(): Row {
        logger.info("Получаю для обработки строку с индексом $currentRowIndex из $count")
        return nextNoLog()
    }

    /**
     * Получить следующую пачку строк
     * @param size размер пачки
     */
    fun getNextBatch(size: Int): List<Row> {
        logger.info("Получаю пачку строк из $size штук начиная с индекса $currentRowIndex")
        val rows: MutableList<Row> = ArrayList()
        for (i in 1..size) {
            if (hasNext()) rows.add(nextNoLog())
        }
        return rows
    }

    /**
     * Обработать следующую строку в контейнере.
     * Принимает на вход функцию (или Closure).
     * Функция должна принимать на вход строку документа (Row), а отдавать строку (String) как результат успешной обработки.
     * Функция может выкинуть исключение, что будет значить завершение с ошибкой.
     * @param closure функция (или Closure)
     */
    fun processNext(closure: Function<Row?, String?>) {
        val row = next()
        try {
            val result = closure.apply(row)
            if (result != null) setRowSuccess(row, result) else setRowSuccess(row)
        } catch (ea: PlannedException) {
            setRowError(row, ea.message!!)
        } catch (e: Exception) {
            setRowError(row, "При редактировании ОП ${row.rowNum} произошла необрабатываемая ошибка: ${e.message}")
        }
    }

    /**
     * Обработать все строки в контейнере.
     * Принимает на вход функцию (или Closure).
     * Функция должна принимать на вход строку документа (Row), а отдавать строку (String) как результат успешной обработки.
     * Функция может выкинуть исключение, что будет значить завершение с ошибкой.
     * @param closure функция (или Closure)
     */
    fun processAll(closure: Function<Row?, String?>) {
        while (hasNext()) {
            processNext(closure)
        }
        logger.warn("Обработка завершена. Всего: $count. Успешно обработано: $successCount. Ошибок: $errorsCount")
    }

    /**
     * Обработать все строки в контейнере И записать файл с результатом.
     * Принимает на вход функцию (или Closure).
     * Функция должна принимать на вход строку документа (Row), а отдавать строку (String) как результат успешной обработки.
     * Функция может выкинуть исключение, что будет значить завершение с ошибкой.
     * @param closure функция (или Closure)
     */
    fun processAllAndWrite(closure: Function<Row?, String?>) {
        processAll(closure)
        write()
    }

}
