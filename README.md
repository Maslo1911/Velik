# Система проката велосипедов (Velik)

Консольная информационная система на Java для управления прокатом велосипедов.

**Предметная область:** Прокат велосипедов / Заявка на аренду велосипеда  
**СУБД:** PostgreSQL  
**Вариант КР1 МИРЭА**

## Архитектура

```
Console UI (Main)
    ↓
Service (бизнес-логика + проверки)
    ↓
Repository / JDBC (PreparedStatement)
    ↓
PostgreSQL
```

### Структура пакетов

```
ru.mirea.velik
├── Main.java                 — консольное меню (UI)
├── model/
│   ├── Client.java
│   ├── Bike.java
│   ├── Rental.java           — основная сущность (заявка на аренду)
│   └── RentalStatus.java     — enum
├── repository/
│   ├── Repository.java       — интерфейс (полиморфизм)
│   ├── ClientRepository.java
│   ├── BikeRepository.java
│   ├── RentalRepository.java
│   ├── JdbcClientRepository.java
│   ├── JdbcBikeRepository.java
│   └── JdbcRentalRepository.java
├── service/
│   ├── ClientService.java
│   ├── BikeService.java
│   └── RentalService.java
├── exception/
│   ├── BusinessException.java
│   ├── EntityNotFoundException.java
│   └── DatabaseException.java
└── util/
    ├── DatabaseManager.java
    └── ExcelExporter.java
```

## Требования

- JDK 17+
- Maven 3.6+
- PostgreSQL 14+

## Быстрый старт

### 1. Создание базы данных

```bash
# Войти в psql от суперпользователя
psql -U postgres

# Создать БД
CREATE DATABASE velik ENCODING 'UTF8';
\q

# Применить схему и тестовые данные
psql -U postgres -d velik -f schema.sql
```

Или выполните содержимое `schema.sql` в pgAdmin / DBeaver после создания БД `velik`.

### 2. Настройка подключения (при необходимости)

По умолчанию:
- URL: `jdbc:postgresql://localhost:5432/velik`
- User: `postgres`
- Password: `postgres`

Можно переопределить:
```bash
export DB_URL="jdbc:postgresql://localhost:5432/velik"
export DB_USER="postgres"
export DB_PASSWORD="your_password"
```

или системными свойствами Java:
```
-Ddb.url=... -Ddb.user=... -Ddb.password=...
```

### 3. Сборка и запуск

```bash
cd velik
mvn clean package
java -jar target/velik-1.0-SNAPSHOT.jar
```

Или из IntelliJ IDEA:
1. Откройте проект (File → Open → папка `velik`)
2. Дождитесь индексации Maven
3. Запустите `Main.java`

## Главное меню

```
========================================
       СИСТЕМА ПРОКАТА ВЕЛОСИПЕДОВ
========================================
1. Клиенты
2. Велосипеды
3. Аренда (заявки)
4. Поиск
5. Фильтрация
6. Статистика
7. Экспорт данных
8. Вывести таблицы базы данных
0. Выход
```

## Бизнес-правила (реализованы в Service)

1. ФИО и телефон клиента обязательны.
2. Цена велосипеда за час должна быть > 0.
3. Нельзя создать аренду на несуществующего клиента / велосипед.
4. Нельзя арендовать недоступный велосипед или велосипед с уже активной арендой.
5. Запрещены недопустимые переходы статусов:
   - CREATED → ACTIVE / CANCELLED
   - ACTIVE → COMPLETED / CANCELLED
   - COMPLETED / CANCELLED — терминальные
6. Нельзя удалить клиента или велосипед с активными арендами.
7. При завершении аренды автоматически рассчитывается стоимость (минимум 1 час).

## Поиск (минимум 2)

- Клиенты по ФИО
- Клиенты по телефону
- Велосипеды по модели
- Аренды по клиенту / по велосипеду

## Фильтрация (минимум 2)

- Велосипеды по типу
- Только доступные велосипеды
- Аренды по статусу
- Аренды по диапазону дат

## Сортировка (минимум 2)

- Клиенты по ФИО / по ID
- Велосипеды по цене
- Аренды по дате начала / по стоимости

## Статистика (5+ показателей)

- Всего клиентов
- Всего велосипедов
- Доступных велосипедов
- Всего заявок + разбивка по статусам
- Общая выручка

## Экспорт

Пункт меню 7 → Excel-файл `.xlsx` (листы: Клиенты, Велосипеды, Аренды).

## ER-диаграмма (текстово)

```
clients (1) ──────< (N) rentals (N) >────── (1) bikes
   id PK                 id PK                  id PK
   full_name             client_id FK           model
   phone UNIQUE          bike_id FK             type
   email                 start_time             available
                         end_time               price_per_hour
                         status
                         total_cost
```

