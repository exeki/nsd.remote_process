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
            //Собираем пачку id
            List<String> ids = batch.collect { Utilities.getCellValueElseThrow(it, 0)}
            //Ищем сущности в системе. Первый запрос
            //Запрос должен вернуть в result массив мап с ключами idAttr и UUID (тк были запрошены именно эти поля в ответе)
            RpcResponseDto response = connector.jsonRpcFind('someEntity', ['idAttr' rpc.opIn(ids), 'removed': false], ['idAttr', 'UUID'])
            //Проверяем запрос на отсутствие ошибок и получаем результат
            List<Map<String, Object>> arr = Utilities.checkRpcResponse(response, true).result
            //Массив rpc dto для запроса на редактирование
            List<RpcRequestDto> requestDtos = []
            batch.each { Row row ->
                //Снова получаем id из строки
                String id = Utilities.getCellValueElseThrow(row, 0)
                //Ищем UUID по id
                String uuid = arr.find{it.idAttr == id}?.UUID
                //Если UUID не нашли - то пишем ошибку в строке
                if(uuid == null) docProcessor.setRowError(row, "Не удалось найти в системе по ID")
                else {
                    //Иначе сразу собираем RpcRequestDto для отправки запроса на редактирование, кладем туда атрибуты из файла
                    def attrs = rpc
                            .attrs('someLogicalAttr', Utilities.getCellValueAsBoolean(row, 1, null).orElse(null))
                            .put('someNumericAttr', Utilities.getCellValueAsNumericElseThrow(row, 2))
                    def requestDto = new RpcRequestDto.Edit(uuid, attrs)
                    //Устанавливаем ID запроса, ответ придет с тем же ID то бы мы могли сопоставить отправленные и пришедшие данные
                    requestDto.setId(id)
                    requestDtos.add(requestDto)
                }
            }
            //Отправляем запрос на редактирование, 50 штук в одном запросе
            List<RpcResponseDto> editResponseDtos = connector.sendRequest(requestDtos)
            //По массиву ответов
            editResponseDtos.each{responseDto ->
                //Ищем строку по которой был запрос
                Row row = batch.find{Utilities.getCellValueElseThrow(it, 0) == responseDto.id}
                //Если в responseDto нет ошибки, ставим успешное выполнение
                if(responseDto.error == null) docProcessor.setRowSuccess(row, "Успешно обработано")
                //Иначе ставим ошибку
                else docProcessor.setRowError(row, "Ошибка при редактировании: " + responseDto.error.message)
            }
        } catch (Exception e) {
            //Незапланированное исключение приводит к ошибке обработки всех строк в пачке
            batch.each { docProcessor.setRowError(it, e) }
        }
    }
    //Не забываем записать результат
    docProcessor.write()
}
