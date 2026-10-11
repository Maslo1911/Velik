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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
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
            DatabaseManager.checkConnection();
            System.out.println("Подключение к базе данных успешно установлено.\n");
        } catch (DatabaseException e) {
            System.err.println("КРИТИЧЕСКАЯ ОШИБКА: " + e.getMessage());
            System.err.println("Создайте БД по скрипту schema.sql и проверьте параметры подключения.");
            return;
        }

        boolean running = true;
        while (running) {
            printMainMenu();
            String input = readLine().trim();
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
            String input = readLine().trim();
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
        String name = readLine();
        System.out.print("Телефон: ");
        String phone = readLine();
        System.out.print("Email (можно пусто): ");
        String email = readLine();
        if (email.isBlank()) email = null;
        Client c = clientService.create(name, phone, email);
        System.out.println("Создан: " + c);
    }

    private static void updateClient() {
        int id = readInt("ID клиента: ");
        System.out.println("Текущие данные: " + clientService.getById(id));
        System.out.print("Новое ФИО: ");
        String name = readLine();
        System.out.print("Новый телефон: ");
        String phone = readLine();
        System.out.print("Новый email (можно пусто): ");
        String email = readLine();
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
            String input = readLine().trim();
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
        String model = readLine();
        System.out.print("Тип (городской/горный/шоссейный/электро): ");
        String type = readLine();
        double price = readDouble("Цена за час: ");
        Bike b = bikeService.create(model, type, true, price);
        System.out.println("Создан: " + b);
    }

    private static void updateBike() {
        int id = readInt("ID велосипеда: ");
        System.out.println("Текущие данные: " + bikeService.getById(id));
        System.out.print("Новая модель: ");
        String model = readLine();
        System.out.print("Новый тип: ");
        String type = readLine();
        boolean available = readBoolean("Доступен (да/нет): ");
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
            System.out.println("4. Изменить клиента / велосипед");
            System.out.println("5. Изменить статус");
            System.out.println("6. Удалить");
            System.out.println("7. Сортировка по дате начала");
            System.out.println("8. Сортировка по стоимости (убыв.)");
            System.out.println("0. Назад");
            System.out.print("Выберите действие: ");
            String input = readLine().trim();
            try {
                switch (input) {
                    case "1" -> createRental();
                    case "2" -> printList(rentalService.getAll());
                    case "3" -> {
                        int id = readInt("Введите ID аренды: ");
                        System.out.println(rentalService.getById(id));
                    }
                    case "4" -> updateRental();
                    case "5" -> changeRentalStatus();
                    case "6" -> {
                        int id = readInt("Введите ID аренды для удаления: ");
                        rentalService.delete(id);
                        System.out.println("Аренда удалена.");
                    }
                    case "7" -> printList(rentalService.sortByStartTimeAsc());
                    case "8" -> printList(rentalService.sortByCostDesc());
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

    private static void updateRental() {
        int id = readInt("ID аренды: ");
        System.out.println("Текущие данные: " + rentalService.getById(id));
        int clientId = readInt("Новый ID клиента: ");
        int bikeId = readInt("Новый ID велосипеда: ");
        Rental r = rentalService.update(id, clientId, bikeId);
        System.out.println("Заявка обновлена: " + r);
    }

    private static void changeRentalStatus() {
        int id = readInt("ID аренды: ");
        System.out.println("Текущие данные: " + rentalService.getById(id));
        System.out.println("Доступные статусы: CREATED, ACTIVE, COMPLETED, CANCELLED");
        System.out.print("Новый статус: ");
        String statusStr = readLine().trim().toUpperCase();
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
            String input = readLine().trim();
            try {
                switch (input) {
                    case "1" -> {
                        System.out.print("Часть ФИО: ");
                        printList(clientService.searchByName(readLine()));
                    }
                    case "2" -> {
                        System.out.print("Телефон: ");
                        printList(clientService.searchByPhone(readLine()));
                    }
                    case "3" -> {
                        System.out.print("Часть модели: ");
                        printList(bikeService.searchByModel(readLine()));
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
            String input = readLine().trim();
            try {
                switch (input) {
                    case "1" -> {
                        System.out.print("Тип (городской/горный/шоссейный/электро): ");
                        printList(bikeService.filterByType(readLine()));
                    }
                    case "2" -> printList(bikeService.filterAvailable());
                    case "3" -> {
                        System.out.print("Статус (CREATED/ACTIVE/COMPLETED/CANCELLED): ");
                        RentalStatus st = RentalStatus.valueOf(readLine().trim().toUpperCase());
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
            Map<RentalStatus, Long> byStatus = rentalService.countByStatuses();

            long availableBikes = bikes.stream().filter(Bike::isAvailable).count();
            long totalRentals = byStatus.values().stream().mapToLong(Long::longValue).sum();
            double revenue = rentalService.totalRevenue();

            System.out.println("\n=========== СТАТИСТИКА СИСТЕМЫ ===========");
            System.out.println("Всего клиентов:           " + clients.size());
            System.out.println("Всего велосипедов:        " + bikes.size());
            System.out.println("Доступных велосипедов:    " + availableBikes);
            System.out.println("Всего заявок на аренду:   " + totalRentals);
            System.out.println("  — созданных (CREATED):  " + byStatus.get(RentalStatus.CREATED));
            System.out.println("  — активных (ACTIVE):    " + byStatus.get(RentalStatus.ACTIVE));
            System.out.println("  — завершённых:          " + byStatus.get(RentalStatus.COMPLETED));
            System.out.println("  — отменённых:           " + byStatus.get(RentalStatus.CANCELLED));
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
            String fileName = readLine().trim();
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
        try {
            System.out.println("\nТаблицы в базе данных:");
            for (String tableName : DatabaseManager.getTableNames()) {
                System.out.println("  - " + tableName);
            }
            System.out.println();
            System.out.println("--- clients ---");
            printList(clientService.getAll());
            System.out.println("--- bikes ---");
            printList(bikeService.getAll());
            System.out.println("--- rentals ---");
            printList(rentalService.getAll());
        } catch (DatabaseException e) {
            System.out.println("Ошибка БД: " + e.getMessage());
        }
    }

    // ==================== УТИЛИТЫ ВВОДА ====================

    /**
     * Чтение строки из консоли. Если ввод закрыт (Ctrl+D / Ctrl+Z), программа завершается
     * корректно, а не падает с NoSuchElementException.
     */
    private static String readLine() {
        if (!scanner.hasNextLine()) {
            System.out.println("\nВвод завершён. Выход из программы...");
            System.exit(0);
        }
        return scanner.nextLine();
    }

    private static boolean readBoolean(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = readLine().trim().toLowerCase();
            switch (line) {
                case "да", "д", "yes", "y", "true" -> {
                    return true;
                }
                case "нет", "н", "no", "n", "false" -> {
                    return false;
                }
                default -> System.out.println("Ошибка: введите «да» или «нет».");
            }
        }
    }

    private static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = readLine().trim();
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
            String line = readLine().trim().replace(',', '.');
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
            String line = readLine().trim();
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
