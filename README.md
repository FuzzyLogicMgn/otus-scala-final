## Приложение для учёта личных финансов
Приложение предназначено для хранения счетов с балансами денежных средств. 
Предусмотрено модификация суммы на счёте за счёт выполнения транзакции. 
Сумма в валюте транзакции автоматически конвертируется в валюту счёта по курсу, актуальному на дату выполнения транзакции.
Приложение использует CQRS-подход.

### Command-часть
Command-часть настроена на использование локального журнала для хранения событий Akka Persistence (директория ./journal).
Запуск узлов command-части:
```
sbt -J-Dconfig.resource=command-node/application.conf "runMain ru.otus.sc.command.CommandMain 2081"
sbt -J-Dconfig.resource=command-node/application.conf "runMain ru.otus.sc.command.CommandMain 2082"
sbt -J-Dconfig.resource=command-node/application.conf "runMain ru.otus.sc.command.CommandMain 2083"
```
Swagger документация доступна по адресу:
```
http://localhost:8081/docs
```
Удалить текущие события из журнала
```
rm -rf journal
```

### Query-часть
Query-часть использует базу данных H2. Файл БД будет расположен в директории запуска приложения (accounting-db.*).
Запуск query-части:
```bash
sbt -J-Dconfig.resource=query-node/application.conf "runMain ru.otus.sc.query.QueryMain"
```

Swagger документация доступна по адресу:
```
http://localhost:8090/docs
```

Удалить файлы БД:
```
rm accounting-db.*
```
