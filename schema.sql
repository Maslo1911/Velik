-- Перед запуском создайте базу:  CREATE DATABASE velik ENCODING 'UTF8';
-- Затем выполните этот скрипт:   psql -U postgres -d velik -f schema.sql

DROP TABLE IF EXISTS rentals;
DROP TABLE IF EXISTS bikes;
DROP TABLE IF EXISTS clients;

-- -----------------------------------------------------------
-- Таблица клиентов
-- -----------------------------------------------------------
CREATE TABLE clients (
    id          SERIAL PRIMARY KEY,
    full_name   VARCHAR(150) NOT NULL,
    phone       VARCHAR(30)  NOT NULL,
    email       VARCHAR(100),
    CONSTRAINT uq_clients_phone UNIQUE (phone)
);

-- -----------------------------------------------------------
-- Таблица велосипедов
-- -----------------------------------------------------------
CREATE TABLE bikes (
    id              SERIAL PRIMARY KEY,
    model           VARCHAR(100) NOT NULL,
    type            VARCHAR(50)  NOT NULL,
    available       BOOLEAN      NOT NULL DEFAULT TRUE,
    price_per_hour  NUMERIC(10, 2) NOT NULL,
    CONSTRAINT chk_price_positive CHECK (price_per_hour > 0)
);

-- -----------------------------------------------------------
-- Таблица заявок на аренду (основная сущность)
-- -----------------------------------------------------------
CREATE TABLE rentals (
    id          SERIAL PRIMARY KEY,
    client_id   INT NOT NULL,
    bike_id     INT NOT NULL,
    start_time  TIMESTAMP NOT NULL,
    end_time    TIMESTAMP NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'CREATED'
                CHECK (status IN ('CREATED', 'ACTIVE', 'COMPLETED', 'CANCELLED')),
    total_cost  NUMERIC(10, 2) NOT NULL DEFAULT 0,
    CONSTRAINT chk_total_cost_nonneg CHECK (total_cost >= 0),
    CONSTRAINT chk_end_after_start   CHECK (end_time IS NULL OR end_time >= start_time),
    CONSTRAINT fk_rentals_client FOREIGN KEY (client_id) REFERENCES clients(id),
    CONSTRAINT fk_rentals_bike   FOREIGN KEY (bike_id)   REFERENCES bikes(id)
);

-- -----------------------------------------------------------
-- Начальные тестовые данные
-- минимум 5 клиентов, 10+ аренд, разные статусы
-- -----------------------------------------------------------

INSERT INTO clients (full_name, phone, email) VALUES
('Иванов Иван Иванович',   '+79001112233', 'ivanov@mail.ru'),
('Петрова Анна Сергеевна', '+79002223344', 'petrova@mail.ru'),
('Сидоров Пётр Алексеевич','+79003334455', 'sidorov@mail.ru'),
('Козлова Мария Дмитриевна','+79004445566', 'kozlova@mail.ru'),
('Морозов Дмитрий Олегович','+79005556677', 'morozov@mail.ru'),
('Васильева Елена Игоревна','+79006667788', NULL),
('Новиков Алексей Викторович','+79007778899', 'novikov@yandex.ru');

INSERT INTO bikes (model, type, available, price_per_hour) VALUES
('Stels Navigator 500',   'городской',  TRUE,  150.00),
('Trek Marlin 5',         'горный',     TRUE,  250.00),
('Giant Contend AR 3',    'шоссейный',  TRUE,  300.00),
('Xiaomi Himo C26',       'электро',    TRUE,  400.00),
('Forward Apache 2.0',    'городской',  TRUE,  120.00),
('Merida Big.Nine 20',    'горный',     TRUE,  280.00),
('Cube Agree C:62',       'шоссейный',  TRUE,  350.00),
('Eltreco XT 800',        'электро',    TRUE,  450.00),
('Stark Outpost',         'городской',  TRUE,  100.00),
('Scott Scale 970',       'горный',     TRUE,  320.00),
('Cannondale Synapse',    'шоссейный',  TRUE,  380.00),
('Jetson Bolt Pro',       'электро',    TRUE,  420.00);

INSERT INTO rentals (client_id, bike_id, start_time, end_time, status, total_cost) VALUES
(1, 1,  '2026-09-20 10:00:00', '2026-09-20 13:00:00', 'COMPLETED', 450.00),
(2, 2,  '2026-09-21 11:00:00', '2026-09-21 14:30:00', 'COMPLETED', 1000.00),
(3, 3,  '2026-09-22 09:00:00', NULL,                   'ACTIVE',    0.00),
(4, 4,  '2026-09-23 12:00:00', NULL,                   'CREATED',   0.00),
(5, 5,  '2026-09-18 15:00:00', '2026-09-18 16:00:00', 'CANCELLED', 0.00),
(1, 6,  '2026-09-24 08:00:00', '2026-09-24 12:00:00', 'COMPLETED', 1120.00),
(2, 7,  '2026-09-25 10:30:00', NULL,                   'ACTIVE',    0.00),
(3, 8,  '2026-09-19 14:00:00', '2026-09-19 17:00:00', 'COMPLETED', 1350.00),
(6, 9,  '2026-09-26 09:00:00', NULL,                   'CREATED',   0.00),
(7, 10, '2026-09-15 11:00:00', '2026-09-15 13:00:00', 'COMPLETED', 640.00),
(4, 11, '2026-09-27 08:00:00', NULL,                   'ACTIVE',    0.00),
(5, 12, '2026-09-10 16:00:00', '2026-09-10 18:00:00', 'CANCELLED', 0.00);

-- Обновляем доступность велосипедов, которые сейчас в CREATED/ACTIVE
UPDATE bikes SET available = FALSE WHERE id IN (3, 4, 7, 9, 11);

-- Проверка
SELECT 'clients' AS tbl, COUNT(*) AS cnt FROM clients
UNION ALL SELECT 'bikes', COUNT(*) FROM bikes
UNION ALL SELECT 'rentals', COUNT(*) FROM rentals;
