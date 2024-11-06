package ru.kazantsev.nsd.remote_process.example

import org.apache.poi.ss.usermodel.Row
import ru.kazantsev.nsd.basic_api_connector.ConnectorParams
import ru.kazantsev.nsd.json_rpc_connector.Connector
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
