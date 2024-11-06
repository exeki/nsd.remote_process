# NSD_REMOTE_PROCESS

## Что это и зачем оно вообще нужно?

*Листай на следующий раздел если не нужны размусоливания.*

Глупая, но нужная библиотека, решающая проблему манипуляций с большими данными в Naumen SD (от пары десятков, до миллионов строк).
Как вы проводите разовые импорты данных в СД? 
Парсите csv строку в консоли? Есть лимит объема строки, и на одну транзакцию есть лимит, и на время выполнения скрипта тоже.
Пишите конфигурацию импорта? Они бесят, и у них есть свои лимиты по объему и времени выполнения.
Читаете сохраненный в SD файл и сохраняете данные из него? Если это excel, то у SD есть проблемы с чтением более 200 тыс строк - зависает намертво. 
Забираете данные из другого источника при помощи планировщика и сохраняете в систему? Это вариант, согласен, но только источник чаще всего не лучше excel, чаще всего именно его дает клиент.

И на этом перепутье вы можете встретить универсального, домощенного, созданного на коленке уничтожителя миграций данных - эту либу.
Она, повторюсь, крайне примитивна, но избавлена от перечисленных выше проблем (спойлер, вы встретите другие, привет HTTP протокол).

## Из чего оно состоит?

Плохие новости, просто взять и запустить что бы все заработало не получиться. 
Для того, чтобы с этим работать, нужно хотя бы базово понимать две составляющие:
1. Библиотеку Apache POI. Это библиотека для работы с документами форматов excel. Документацию найдете в интернете. Тут используется версия 5.3.0.
2. Мою библиотеку [nsd.json_rpc_connector](https://github.com/exeki/nsd.json_rpc_connector)или другую мою библиотеку [nsd.json_rpc_connector](https://github.com/exeki/nsd.json_rpc_connector) (если у вас на инсталляции есть [модуль jsonRpc](https://github.com/exeki/ru.itsm365.jsonRpc_doc)) или обе их сразу. Именно они используются для записи данных в NSD.
Без этого просто никак. 
## Как оно работает?

Вы пишите на своем ПК программу с применением данной библиотеки, не собираете ее в jar а просто запускаете из IDE, и она ходит в SD по HTTP протоколу и последовательно обрабатывает каждую строку. 
Из за HTTP протокола это может быть долго (хотя тут есть варианты как ускориться, о них далее), но зато наверняка, в своей IDE вы не ограничены временем выполнения транзакции SD, а в качестве источника данных можно использовать вашу файловую систему. 
Не нравиться запускать на своем ПК? Запусти где хочешь, но только не усложняй слишком сильно задачу. Напомню, что мы рассматриваем проведением разовой обработки, а которой более не вспомним никогда. 

## Основные классы для взаимодействия

### DocProcessor

Используется как входная точка, поставляет методы, которые принимают на вход Closure по обработке строк, такие как:
- processNext(Closure closure) - обрабатывает следующую строку на очереди переданной closure
- processAll(Closure closure) - обрабатывает все строки переданной closure
- processAllAndWrite -обрабатывает все строки переданной closure И записывает файл с результатом обработки

Касательно Closure нужно добавить следующее: 

1. Она выполняется внутри контейнера, отвечающего за обработку ошибок и возвращенного результата. 
2. Функция будет выполнена по каждой строке.
3. В случае успешного завершения функции строка будет считаться успешно обработанной. 
4. В случае если функция выкинет исключение (специально или намеренно) - строка будет считаться обработанной с ошибкой.  
5. Функция обязательно должна принимать на вход Row, ее внедрит туда DocProcessor .
6. Функция может отдавать в качестве результата строку, что бы при помощи него конкретизировать результат успешного выполнения. Строку будет записана в файл с результатом выполнения.

Дополнительно стоит упомянуть о следующих функциях DocProcessor, которые можно использовать за рамками контейнерной обработки (то есть вместо методов серии process):

- hasNext() - проверяет остались ли строки к обработки
- getNext() - получает следующую строку в обработку,
- getNextBatch(Integer size) - получить пачку строку размером size, что бы вы могли обработать их разом. Это отлично сочетается с возможностью jsonRpc отправлять batch операции и является способом **значительно** ускорить обработку больших объемов данных
- setRowError(Row row,  String message) - записывает провал обработки строки
- setRowSuccess(Row row, String message) - записывает успех обработки строки

### Utilities

В основном используется для типизированного/не типизированного гарантированного/не гарантированного получения данных из ячеек.

Примеры методов:
- getCellValue - получает значение ячейки без типизации
- getCellValueAsDate - получает значение ячейки как дату 
- getCellValueAsNumeric - как дробное число
- getCellValueAsBoolean - как булево

Все перечисленные выше методы возвращают Optional.
Также есть методы, которые возвращают конкретные значения, иначе выбрасывают исключение, у них идентичные названия, но на конце нужно добавить ElseThrow. Например getCellValueAsNumericElseThrow.
Все перечисленные методы - статические. 

## Примеры использования
### Пример 1

Допустим, нужно отредактировать в системе сущности, клиент передал файл формата:

| Столбец 1  | Столбец 2 | Столбец 3 |
| ---------- | --------- | --------- |
| dfg2134df  | да        | 3123      |
| fgh345dfgf | нет       | 23        |
| 32fg823gfd |           | 1         |
| xcv83ghrrt | нет       | 98        |
| 3245bfg34  | да        | 2134      |

Столбец 1 содержит строковый ID сущности, которую будем редактировать;
Столбец 2 содержит строки, содержащие да или нет, это потребуется конвертировать в булево для записи;
Столбец 3 содержит какие то числа, которые записываем как есть.

```groovy
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
```

### Пример 2

Предположим что работаем с тем же файлом, что и в прошлом примере. 
Но там на каждую строку идет два обращения. Модуль jsonRpc позволяет оставить только одно, тк содержит методы для выполнения нескольких операций за один запрос. 

```groovy
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

```

//TODO пример на batch
