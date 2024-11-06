package ru.kazantsev.nsd.remote_process.example

import org.apache.poi.ss.usermodel.Row
import ru.kazantsev.nsd.basic_api_connector.ConnectorParams
import ru.kazantsev.nsd.json_rpc_connector.Connector
import ru.kazantsev.nsd.json_rpc_connector.RpcRequestDto
import ru.kazantsev.nsd.json_rpc_connector.RpcResponseDto
import ru.kazantsev.nsd.json_rpc_connector.RpcUtilities
import ru.kazantsev.nsd.remote_process.DocProcessor
import ru.kazantsev.nsd.remote_process.Utilities

static void main(String[] args) {

    //Заранее создаем jsonPrc коннектор и утилитарный класс для работы с rpc модулем
    RpcUtilities rpc = RpcUtilities.getInstance()
    Connector connector = new Connector(ConnectorParams.byConfigFile("MY_INSTALLATION_ID"))

    //Создаем DocProcessor, в нем указываем путь до файла и индекс столбца, куда будет записываться результат обработки
    DocProcessor docProcessor = new DocProcessor('C:\\выгрузка.xlsx', 3)
    while (docProcessor.hasNext()) {
        //Получаем пачку строк к обработке
        List<Row> batch = docProcessor.getNextBatch(50)
        try {
            //Создаем лист dto, что бы в нем агрегировать данные
            List<Map<String, Object>> dtos = batch.collect { return ['row': it, 'id': Utilities.getCellValueElseThrow(it, 0)] }
            List<String> ids = batch.collect { it.id as String }
            RpcResponseDto response = connector.jsonRpcFind('someEntity', ['idAttr' rpc.opIn(ids), 'removed': false], ['idAttr', 'UUID'])
            List<Map<String, Object>> arr = Utilities.checkRpcResponse(response, true).result
            arr.each {
                Map dto = dtos.find { dto -> dto.id == it.idAttr }
                dto['uuid'] = it.UUID
            }
            List<RpcRequestDto> requestDtos
            dtos.each { if (it.uuid == null) docProcessor.setRowError(it.row, "Не удалось найти в системе по ID") }
            dtos.removeAll { it.uuid == null }
            dtos.each { dto ->
                def attrs = rpc
                        .attrs('someLogicalAttr', Utilities.getCellValueAsBoolean(row, 1, null).orElse(null))
                        .put('someNumericAttr', Utilities.getCellValueAsNumericElseThrow(row, 2))
                def requestDto = new RpcRequestDto.Edit(dto.uuid, attrs)
                requestDto.setId(dto.id)
                requestDtos.add(requestDto)
            }
            List<RpcResponseDto> editResponseDtos = connector.sendRequest(requestDtos)
            editResponseDtos.each{responseDto ->
                def dto = dto.find{it.id == responseDto.id}
                if(responseDto.error == null) docProcessor.setRowSuccess(dto.row, "Успешно обработано")
                else docProcessor.setRowError(dto.row, "Ошибка при редактировании: " + responseDto.error.message)
            }
        } catch (Exception e) {
            batch.each { docProcessor.setRowError(it, e) }
        }
    }

    //Вызываем метод processAllAndWrite и передаем в него функцию, которая будет выполнена по каждой строке.
    docProcessor.processAllAndWrite { Row row ->
        String someId = row.getCell(0).getStringCellValue()

        //Библиотека jsonRpc содержит метод, который может редактировать объекты по условию
        RpcResponseDto res = connector.jsonRpcEdit(
                'someEntity',
                rpc.query('idAttr', someId),
                rpc.attrs('someLogicalAttr', Utilities.getCellValueAsBoolean(row, 1, null).orElse(null))
                        .put('someNumericAttr', Utilities.getCellValueAsNumericElseThrow(row, 2))
        )
        //RPC запросы возвращают 200, даже если на той стороне произошла ошибка. Этот метод выкинет исключение если в теле ответа есть сообщение об ошибке
        Utilities.checkRpcResponse(res, false)
        return "Успешно обработано"
    }
}
