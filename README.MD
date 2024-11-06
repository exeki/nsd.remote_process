# NSD_REMOTE_PROCESS

## Что это и зачем оно вообще нужно?

*Листай на следующий раздел если не нужны размусоливания*

Глупая, но нужная библиотека, решающая проблему манипуляций с большими данными в Naumen SD (от пары десятков, до миллионов строк).
Как вы проводите разовые импорты данных в СД? 
Парсите csv строку в консоли? Есть лимит объема строки, и на одну транзакцию есть лимит, и на время выполнения скрипта тоже.
Пишите конфигурацию импорта? Большие конфигурации сложно писать из-за отсутствия типизации, и у них есть свои лимиты по объему и времени выполнения.
Читаете сохраненный в SD файл и сохраняете данные из него? Если это excel, то у SD есть проблемы с чтением более 200 тыс. строк - зависает намертво. 
Забираете данные из другого источника при помощи планировщика и сохраняете в систему? Это вариант, согласен, но только источник чаще всего не лучше excel, чаще всего именно его дает клиент.

И на этом перепутье вы можете встретить универсального, домощенного, созданного на коленке уничтожителя миграций данных - эту либу.
Она, повторюсь, крайне примитивна, но избавлена от перечисленных выше проблем (спойлер, вы встретите другие, привет HTTP протокол).

## Из чего оно состоит?

Плохие новости, просто взять и запустить что бы все заработало не получиться. 
Для того, чтобы с этим работать, нужно хотя бы базово понимать две составляющие:
1. Библиотеку Apache POI. Это библиотека для работы с документами форматов excel. Документацию найдете в интернете. Тут используется версия 5.3.0.
2. Мою библиотеку [nsd.json_rpc_connector](https://github.com/exeki/nsd.json_rpc_connector)или другую мою библиотеку [nsd.json_rpc_connector](https://github.com/exeki/nsd.json_rpc_connector) (если у вас на инсталляции есть [модуль jsonRpc](https://github.com/exeki/ru.itsm365.jsonRpc_doc)) или обе их сразу. Именно они используются для записи данных в NSD.
Без этого просто никак. Перечисленные библиотеки поставляются вместе с текущей, их не нужно подключать.

## Как оно работает?

Вы пишите на своем ПК программу с применением данной библиотеки, не собираете ее в jar а просто запускаете из IDE, и она ходит в SD по HTTP протоколу и последовательно обрабатывает каждую строку. 
Из-за HTTP протокола это может быть долго (хотя тут есть варианты как ускориться, о них далее в примере 3), но зато наверняка и без ручного труда в процессе, в своей IDE вы не ограничены временем выполнения транзакции SD, а в качестве источника данных можно использовать вашу файловую систему. 
По факту выполнения библиотека может записать файл с результатом, который является копией исходного файла, но добавит в указанный вами столбец признак true\false, сигнализирующий о результате обработки, а так же в следующий столбец сообщение с конкретизацией успеха/ошибки. 

## Основные классы для взаимодействия

### DocProcessor

Используется как входная точка, поставляет методы, которые принимают на вход Closure по обработке строк, такие как:
- processNext(Closure closure) - обрабатывает следующую строку на очереди переданной closure
- processAll(Closure closure) - обрабатывает все строки переданной closure
- processAllAndWrite -обрабатывает все строки переданной closure И записывает файл с результатом обработки

Касательно Closure нужно добавить следующее: 

1. Она выполняется внутри контейнера, отвечающего за обработку ошибок и возвращенного результата;
2. Closure будет выполнена по каждой строке;
3. В случае успешного завершения функции строка будет считаться успешно обработанной;
4. В случае если Closure выкинет исключение (специально или намеренно) - строка будет считаться обработанной с ошибкой; 
5. Closure обязательно должна принимать на вход Row, ее внедрит туда DocProcessor;
6. Функция может отдавать в качестве результата строку, что бы при помощи нее конкретизировать результат успешного выполнения. Строку будет записана в файл с результатом выполнения.

Дополнительно стоит упомянуть о следующих функциях DocProcessor, которые можно использовать за рамками контейнерной обработки (то есть вместо методов серии process):

- hasNext() - проверяет, остались ли строки к обработке;
- getNext() - получает следующую строку в обработку;
- getNextBatch(Integer size) - получить пачку строку размером size, что бы вы могли обработать их разом. Это отлично сочетается с возможностью jsonRpc отправлять batch операции и является способом **значительно** ускорить обработку больших объемов данных;
- setRowError(Row row,  String message) - записывает провал обработки строки;
- setRowSuccess(Row row, String message) - записывает успех обработки строки.

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

## Как подключить?

1. Добавить репозиторий github.com
2. Добавить зависимость текущей библиотеки
3. Пользуйтесь

```kotlin
plugins {
    id("groovy")
}

group = "my.awesome.group"
version = "1.1.1"

//Репозитории
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/exeki/*")
        credentials {
            username = System.getenv("GITHUB_USERNAME")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
    mavenCentral()
    mavenLocal()
}

//Зависимость
dependencies {
    implementation("ru.kazantsev.nsd:remote_process:1.0.2")
}
```

## Примеры использования

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
Столбец 3 содержит какие-то числа, которые записываем как есть.

Все дальнейшие примеры использования библиотеки будут рассматривать обработку этой таблицы.

### Пример 1

Типичный и простейший пример обработки файла:

```groovy
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

В прошлом примере на каждую строку идет два обращения. 
Модуль jsonRpc позволяет оставить только одно, тк содержит методы для выполнения нескольких операций за один запрос. 
Это позволит сэкономить время обработки на HTTP соединении.

```groovy
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

### Пример 3

Предположим в файле миллион строк, в таком случае обработка может затянуться. Если просят сделать быстро, то может подойти вариант отправки batch операция с применением jsonRpc.
Предположим, что мы хотим отправлять batch операции на редактирование по 50 штук.
Тогда это будет выглядеть так:

```groovy
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
```

Это значительно ускорит выполнение за счет экономии времени на открытие/закрытие HTTP соединение и ожидание ответа сервера, буквально в разы. 
