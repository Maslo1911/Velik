package ru.mirea.velik;

import ru.mirea.velik.exception.BusinessException;
import ru.mirea.velik.exception.DatabaseException;
import ru.mirea.velik.exception.EntityNotFoundException;
import ru.mirea.velik.model.Bike;
import ru.mirea.velik.model.Client;
import ru.mirea.velik.model.Rental;
import ru.mirea.velik.model.RentalStatus;
import ru.mirea.velik.repository.JdbcBikeRepository;
import ru.mirea.velik.repository.JdbcClientRepository;
import ru.mirea.velik.repository.JdbcRentalRepository;
import ru.mirea.velik.service.BikeService;
import ru.mirea.velik.service.ClientService;
import ru.mirea.velik.service.RentalService;
import ru.mirea.velik.util.DatabaseManager;
import ru.mirea.velik.util.ExcelExporter;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

/**
 * Консольная информационная система проката велосипедов.
 * Архитектура: Console UI → Service → Repository/JDBC → PostgreSQL
 */
public class Main {

    private static final Scanner scanner = new Scanner(System.in);
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static ClientService clientService;
    private static BikeService bikeService;
    private static RentalService rentalService;

    public static void main(String[] args) {
        try {
            initServices();
            // Проверка подключения
            Connection conn = DatabaseManager.getConnection();
            conn.close();
            System.out.println("Подключение к базе данных успешно установлено.\n");
        } catch (DatabaseException e) {
            System.err.println("КРИТИЧЕСКАЯ ОШИБКА: " + e.getMessage());
            System.err.println("Создайте БД по скрипту schema.sql и проверьте параметры подключения.");
            return;
        } catch (java.sql.SQLException e) {
            System.err.println("Ошибка закрытия соединения: " + e.getMessage());
            return;
        }

        boolean running = true;
        while (running) {
            printMainMenu();
            String input = scanner.nextLine().trim();
            try {
                switch (input) {
                    case "1" -> handleClients();
                    case "2" -> handleBikes();
                    case "3" -> handleRentals();
                    case "4" -> handleSearch();
                    case "5" -> handleFilter();
                    case "6" -> handleStatistics();
                    case "7" -> handleExport();
                    case "8" -> handleShowTables();
                    case "0" -> {
                        System.out.println("Выход из программы...");
                        running = false;
                    }
                    default -> System.out.println("Ошибка: неизвестный пункт меню. Повторите ввод.\n");
                }
            } catch (BusinessException | EntityNotFoundException e) {
                System.out.println("Ошибка: " + e.getMessage() + "\n");
            } catch (DatabaseException e) {
                System.out.println("Ошибка БД: " + e.getMessage() + "\n");
            } catch (Exception e) {
                System.out.println("Непредвиденная ошибка: " + e.getMessage() + "\n");
            }
        }
        scanner.close();
    }

    private static void initServices() {
        var clientRepo = new JdbcClientRepository();
        var bikeRepo = new JdbcBikeRepository();
        var rentalRepo = new JdbcRentalRepository();
        bikeService = new BikeService(bikeRepo, rentalRepo);
        clientService = new ClientService(clientRepo, rentalRepo);
        rentalService = new RentalService(rentalRepo, clientRepo, bikeRepo, bikeService);
    }

    private static void printMainMenu() {
        System.out.println("========================================");
        System.out.println("       СИСТЕМА ПРОКАТА ВЕЛОСИПЕДОВ");
        System.out.println("========================================");
        System.out.println("1. Клиенты");
        System.out.println("2. Велосипеды");
        System.out.println("3. Аренда (заявки)");
        System.out.println("4. Поиск");
        System.out.println("5. Фильтрация");
        System.out.println("6. Статистика");
        System.out.println("7. Экспорт данных");
        System.out.println("8. Вывести таблицы базы данных");
        System.out.println("0. Выход");
        System.out.print("Выберите действие: ");
    }

    // ==================== КЛИЕНТЫ ====================

    private static void handleClients() {
        boolean back = false;
        while (!back) {
            System.out.println("\n----------- КЛИЕНТЫ -----------");
            System.out.println("1. Добавить клиента");
            System.out.println("2. Показать всех");
            System.out.println("3. Показать по ID");
            System.out.println("4. Изменить");
            System.out.println("5. Удалить");
            System.out.println("6. Сортировка по ФИО");
            System.out.println("7. Сортировка по ID (убыв.)");
            System.out.println("0. Назад");
            System.out.print("Выберите действие: ");
            String input = scanner.nextLine().trim();
            try {
                switch (input) {
                    case "1" -> addClient();
                    case "2" -> printList(clientService.getAll());
                    case "3" -> {
                        int id = readInt("Введите ID клиента: ");
                        System.out.println(clientService.getById(id));
                    }
                    case "4" -> updateClient();
                    case "5" -> {
                        int id = readInt("Введите ID клиента для удаления: ");
                        clientService.delete(id);
                        System.out.println("Клиент удалён.");
                    }
                    case "6" -> printList(clientService.sortByName());
                    case "7" -> printList(clientService.sortByIdDesc());
                    case "0" -> back = true;
                    default -> System.out.println("Неизвестный пункт.");
                }
            } catch (BusinessException | EntityNotFoundException e) {
                System.out.println("Ошибка: " + e.getMessage());
            } catch (DatabaseException e) {
                System.out.println("Ошибка БД: " + e.getMessage());
            }
        }
    }

    private static void addClient() {
        System.out.print("ФИО: ");
        String name = scanner.nextLine();
        System.out.print("Телефон: ");
        String phone = scanner.nextLine();
        System.out.print("Email (можно пусто): ");
        String email = scanner.nextLine();
        if (email.isBlank()) email = null;
        Client c = clientService.create(name, phone, email);
        System.out.println("Создан: " + c);
    }

    private static void updateClient() {
        int id = readInt("ID клиента: ");
        System.out.print("Новое ФИО: ");
        String name = scanner.nextLine();
        System.out.print("Новый телефон: ");
        String phone = scanner.nextLine();
        System.out.print("Новый email (можно пусто): ");
        String email = scanner.nextLine();
        if (email.isBlank()) email = null;
        Client c = clientService.update(id, name, phone, email);
        System.out.println("Обновлён: " + c);
    }

    // ==================== ВЕЛОСИПЕДЫ ====================

    private static void handleBikes() {
        boolean back = false;
        while (!back) {
            System.out.println("\n----------- ВЕЛОСИПЕДЫ -----------");
            System.out.println("1. Добавить велосипед");
            System.out.println("2. Показать все");
            System.out.println("3. Показать по ID");
            System.out.println("4. Изменить");
            System.out.println("5. Удалить");
            System.out.println("6. Сортировка по цене (возр.)");
            System.out.println("7. Сортировка по цене (убыв.)");
            System.out.println("0. Назад");
            System.out.print("Выберите действие: ");
            String input = scanner.nextLine().trim();
            try {
                switch (input) {
                    case "1" -> addBike();
                    case "2" -> printList(bikeService.getAll());
                    case "3" -> {
                        int id = readInt("Введите ID велосипеда: ");
                        System.out.println(bikeService.getById(id));
                    }
                    case "4" -> updateBike();
                    case "5" -> {
                        int id = readInt("Введите ID велосипеда для удаления: ");
                        bikeService.delete(id);
                        System.out.println("Велосипед удалён.");
                    }
                    case "6" -> printList(bikeService.sortByPriceAsc());
                    case "7" -> printList(bikeService.sortByPriceDesc());
                    case "0" -> back = true;
                    default -> System.out.println("Неизвестный пункт.");
                }
            } catch (BusinessException | EntityNotFoundException e) {
                System.out.println("Ошибка: " + e.getMessage());
            } catch (DatabaseException e) {
                System.out.println("Ошибка БД: " + e.getMessage());
            }
        }
    }

    private static void addBike() {
        System.out.print("Модель: ");
        String model = scanner.nextLine();
        System.out.print("Тип (городской/горный/шоссейный/электро): ");
        String type = scanner.nextLine();
        double price = readDouble("Цена за час: ");
        Bike b = bikeService.create(model, type, true, price);
        System.out.println("Создан: " + b);
    }

    private static void updateBike() {
        int id = readInt("ID велосипеда: ");
        System.out.print("Новая модель: ");
        String model = scanner.nextLine();
        System.out.print("Новый тип: ");
        String type = scanner.nextLine();
        System.out.print("Доступен (true/false): ");
        boolean available = Boolean.parseBoolean(scanner.nextLine().trim());
        double price = readDouble("Новая цена за час: ");
        Bike b = bikeService.update(id, model, type, available, price);
        System.out.println("Обновлён: " + b);
    }

    // ==================== АРЕНДА ====================

    private static void handleRentals() {
        boolean back = false;
        while (!back) {
            System.out.println("\n----------- АРЕНДА (ЗАЯВКИ) -----------");
            System.out.println("1. Создать заявку на аренду");
            System.out.println("2. Показать все");
            System.out.println("3. Показать по ID");
            System.out.println("4. Изменить статус");
            System.out.println("5. Удалить");
            System.out.println("6. Сортировка по дате начала");
            System.out.println("7. Сортировка по стоимости (убыв.)");
            System.out.println("0. Назад");
            System.out.print("Выберите действие: ");
            String input = scanner.nextLine().trim();
            try {
                switch (input) {
                    case "1" -> createRental();
                    case "2" -> printList(rentalService.getAll());
                    case "3" -> {
                        int id = readInt("Введите ID аренды: ");
                        System.out.println(rentalService.getById(id));
                    }
                    case "4" -> changeRentalStatus();
                    case "5" -> {
                        int id = readInt("Введите ID аренды для удаления: ");
                        rentalService.delete(id);
                        System.out.println("Аренда удалена.");
                    }
                    case "6" -> printList(rentalService.sortByStartTimeAsc());
                    case "7" -> printList(rentalService.sortByCostDesc());
                    case "0" -> back = true;
                    default -> System.out.println("Неизвестный пункт.");
                }
            } catch (BusinessException | EntityNotFoundException e) {
                System.out.println("Ошибка: " + e.getMessage());
            } catch (DatabaseException e) {
                System.out.println("Ошибка БД: " + e.getMessage());
            }
        }
    }

    private static void createRental() {
        int clientId = readInt("ID клиента: ");
        int bikeId = readInt("ID велосипеда: ");
        Rental r = rentalService.create(clientId, bikeId);
        System.out.println("Создана заявка: " + r);
    }

    private static void changeRentalStatus() {
        int id = readInt("ID аренды: ");
        System.out.println("Доступные статусы: CREATED, ACTIVE, COMPLETED, CANCELLED");
        System.out.print("Новый статус: ");
        String statusStr = scanner.nextLine().trim().toUpperCase();
        RentalStatus status;
        try {
            status = RentalStatus.valueOf(statusStr);
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка: некорректный статус.");
            return;
        }
        Rental r = rentalService.changeStatus(id, status);
        System.out.println("Статус изменён: " + r);
    }

    // ==================== ПОИСК ====================

    private static void handleSearch() {
        boolean back = false;
        while (!back) {
            System.out.println("\n----------- ПОИСК -----------");
            System.out.println("1. Клиенты по ФИО");
            System.out.println("2. Клиенты по телефону");
            System.out.println("3. Велосипеды по модели");
            System.out.println("4. Аренды по ID клиента");
            System.out.println("5. Аренды по ID велосипеда");
            System.out.println("0. Назад");
            System.out.print("Выберите действие: ");
            String input = scanner.nextLine().trim();
            try {
                switch (input) {
                    case "1" -> {
                        System.out.print("Часть ФИО: ");
                        printList(clientService.searchByName(scanner.nextLine()));
                    }
                    case "2" -> {
                        System.out.print("Телефон: ");
                        printList(clientService.searchByPhone(scanner.nextLine()));
                    }
                    case "3" -> {
                        System.out.print("Часть модели: ");
                        printList(bikeService.searchByModel(scanner.nextLine()));
                    }
                    case "4" -> {
                        int id = readInt("ID клиента: ");
                        printList(rentalService.searchByClientId(id));
                    }
                    case "5" -> {
                        int id = readInt("ID велосипеда: ");
                        printList(rentalService.searchByBikeId(id));
                    }
                    case "0" -> back = true;
                    default -> System.out.println("Неизвестный пункт.");
                }
            } catch (BusinessException | EntityNotFoundException e) {
                System.out.println("Ошибка: " + e.getMessage());
            } catch (DatabaseException e) {
                System.out.println("Ошибка БД: " + e.getMessage());
            }
        }
    }

    // ==================== ФИЛЬТРАЦИЯ ====================

    private static void handleFilter() {
        boolean back = false;
        while (!back) {
            System.out.println("\n----------- ФИЛЬТРАЦИЯ -----------");
            System.out.println("1. Велосипеды по типу");
            System.out.println("2. Только доступные велосипеды");
            System.out.println("3. Аренды по статусу");
            System.out.println("4. Аренды по диапазону дат начала");
            System.out.println("0. Назад");
            System.out.print("Выберите действие: ");
            String input = scanner.nextLine().trim();
            try {
                switch (input) {
                    case "1" -> {
                        System.out.print("Тип (городской/горный/шоссейный/электро): ");
                        printList(bikeService.filterByType(scanner.nextLine()));
                    }
                    case "2" -> printList(bikeService.filterAvailable());
                    case "3" -> {
                        System.out.print("Статус (CREATED/ACTIVE/COMPLETED/CANCELLED): ");
                        RentalStatus st = RentalStatus.valueOf(scanner.nextLine().trim().toUpperCase());
                        printList(rentalService.filterByStatus(st));
                    }
                    case "4" -> {
                        LocalDateTime from = readDateTime("Дата начала (yyyy-MM-dd HH:mm): ");
                        LocalDateTime to = readDateTime("Дата конца (yyyy-MM-dd HH:mm): ");
                        printList(rentalService.filterByDateRange(from, to));
                    }
                    case "0" -> back = true;
                    default -> System.out.println("Неизвестный пункт.");
                }
            } catch (BusinessException | EntityNotFoundException e) {
                System.out.println("Ошибка: " + e.getMessage());
            } catch (IllegalArgumentException e) {
                System.out.println("Ошибка: некорректное значение статуса.");
            } catch (DatabaseException e) {
                System.out.println("Ошибка БД: " + e.getMessage());
            }
        }
    }

    // ==================== СТАТИСТИКА ====================

    private static void handleStatistics() {
        try {
            List<Client> clients = clientService.getAll();
            List<Bike> bikes = bikeService.getAll();
            List<Rental> rentals = rentalService.getAll();

            long availableBikes = bikes.stream().filter(Bike::isAvailable).count();
            long created = rentalService.countByStatus(RentalStatus.CREATED);
            long active = rentalService.countByStatus(RentalStatus.ACTIVE);
            long completed = rentalService.countByStatus(RentalStatus.COMPLETED);
            long cancelled = rentalService.countByStatus(RentalStatus.CANCELLED);
            double revenue = rentalService.totalRevenue();

            System.out.println("\n=========== СТАТИСТИКА СИСТЕМЫ ===========");
            System.out.println("Всего клиентов:           " + clients.size());
            System.out.println("Всего велосипедов:        " + bikes.size());
            System.out.println("Доступных велосипедов:    " + availableBikes);
            System.out.println("Всего заявок на аренду:   " + rentals.size());
            System.out.println("  — созданных (CREATED):  " + created);
            System.out.println("  — активных (ACTIVE):    " + active);
            System.out.println("  — завершённых:          " + completed);
            System.out.println("  — отменённых:           " + cancelled);
            System.out.printf("Общая выручка:            %.2f руб.%n", revenue);
            System.out.println("==========================================\n");
        } catch (DatabaseException e) {
            System.out.println("Ошибка БД при получении статистики: " + e.getMessage());
        }
    }

    // ==================== ЭКСПОРТ ====================

    private static void handleExport() {
        try {
            System.out.print("Имя файла (например, export.xlsx): ");
            String fileName = scanner.nextLine().trim();
            if (fileName.isBlank()) {
                fileName = "velik_export.xlsx";
            }
            if (!fileName.endsWith(".xlsx")) {
                fileName += ".xlsx";
            }
            ExcelExporter exporter = new ExcelExporter();
            exporter.export(
                    clientService.getAll(),
                    bikeService.getAll(),
                    rentalService.getAll(),
                    fileName
            );
            System.out.println("Данные успешно экспортированы в файл: " + fileName);
        } catch (IOException e) {
            System.out.println("Ошибка записи файла: " + e.getMessage());
        } catch (DatabaseException e) {
            System.out.println("Ошибка БД: " + e.getMessage());
        }
    }

    // ==================== ПОКАЗ ТАБЛИЦ ====================

    private static void handleShowTables() {
        try (Connection conn = DatabaseManager.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            System.out.println("\nТаблицы в базе данных:");
            try (ResultSet rs = meta.getTables(conn.getCatalog(), null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    System.out.println("  - " + rs.getString("TABLE_NAME"));
                }
            }
            System.out.println();
            System.out.println("--- clients ---");
            printList(clientService.getAll());
            System.out.println("--- bikes ---");
            printList(bikeService.getAll());
            System.out.println("--- rentals ---");
            printList(rentalService.getAll());
        } catch (Exception e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    // ==================== УТИЛИТЫ ВВОДА ====================

    private static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("Ошибка: ID должен быть целым числом.");
            }
        }
    }

    private static double readDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim().replace(',', '.');
            try {
                return Double.parseDouble(line);
            } catch (NumberFormatException e) {
                System.out.println("Ошибка: введите число.");
            }
        }
    }

    private static LocalDateTime readDateTime(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try {
                return LocalDateTime.parse(line, DTF);
            } catch (DateTimeParseException e) {
                System.out.println("Ошибка: формат даты yyyy-MM-dd HH:mm (например 2026-09-27 14:30)");
            }
        }
    }

    private static <T> void printList(List<T> list) {
        if (list == null || list.isEmpty()) {
            System.out.println("(пусто)");
            return;
        }
        list.forEach(System.out::println);
        System.out.println("Всего: " + list.size());
    }
}
