package ru.kazantsev.nsd.remote_process.example

import org.apache.poi.ss.usermodel.Row
import ru.kazantsev.nsd.basic_api_connector.ConnectorParams
import ru.kazantsev.nsd.basic_api_connector.Connector
import ru.kazantsev.nsd.remote_process.DocProcessor
import ru.kazantsev.nsd.remote_process.Utilities
import ru.kazantsev.nsd.remote_process.exception.PlannedException

static void main(String[] args) {

    //Заранее создаем коннектор
    Connector connector = new Connector(ConnectorParams.byConfigFile("MY_INSTALLATION_ID"))

    //Создаем DocProcessor, в нем указываем путь до файла и индекс столбца, куда будет записываться результат обработки
    DocProcessor docProcessor = new DocProcessor('C:\\выгрузка.xlsx', 3)

    //Вызываем метод processAllAndWrite и передаем в него функцию, которая будет выполнена по каждой строке.

    docProcessor.processAllAndWrite { Row row ->
        //Тут получаем идентификатор искомой сущности
        String someId = Utilities.getCellValueElseThrow(row, 0)

        //Чаще всего в переданных клиентом файлах идентификатор это не UUID, по этому проводим поиск сущности
        String uuid
        List<Map<String, Object>> search = connector.find('someEntity', ['idAttr': someId], ['UUID'])
        //Если сущность в базе не найдена - выкидываем исключение, которое обработчик запишет в файл результата
        if (search.isEmpty()) throw new PlannedException("Не удалось найти сущность с ID ${someId}")
        else uuid = search.last().UUID

        //Редактируем ранее найденный объект
        connector.edit(
                uuid,
                [
                        //Здесь утилитарный класс помогает получить типизированное значение
                        'someLogicalAttr': Utilities.getCellValueAsBoolean(row, 1, null).orElse(null),
                        //Здесь будет выкинуто исключение, если значение в документе отсутствует
                        'someNumericAttr': Utilities.getCellValueAsNumericElseThrow(row, 2)
                ]
        )

        //Это будет записано как результат обработки помимо булевого признака
        return "Успешно обработано"
    }
}
