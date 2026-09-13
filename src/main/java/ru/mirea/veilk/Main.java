package ru.mirea.veilk;

import java.util.Scanner;

public class Main {

    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        boolean running = true;

        while (running) {
            printMainMenu();
            String input = scanner.nextLine().trim();

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
        }

        scanner.close();
    }

    private static void printMainMenu() {
        System.out.println("========================================");
        System.out.println("           СИСТЕМА ПРОКАТА ВЕЛОСИПЕДОВ");
        System.out.println("========================================");
        System.out.println("1. Клиенты");
        System.out.println("2. Велосипеды");
        System.out.println("3. Аренда");
        System.out.println("4. Поиск");
        System.out.println("5. Фильтрация");
        System.out.println("6. Статистика");
        System.out.println("7. Экспорт данных");
        System.out.println("8. Вывести таблицы базы данных");
        System.out.println("0. Выход");
        System.out.print("Выберите действие: ");
    }

    // ==== Заглушки пунктов меню (реализация будет добавлена позже) ====

    private static void handleClients() {
        printSubMenuStub("КЛИЕНТЫ");
    }

    private static void handleBikes() {
        printSubMenuStub("ВЕЛОСИПЕДЫ");
    }

    private static void handleRentals() {
        printSubMenuStub("АРЕНДА");
    }

    private static void handleSearch() {
        printStub("Поиск");
    }

    private static void handleFilter() {
        printStub("Фильтрация");
    }

    private static void handleStatistics() {
        printStub("Статистика");
    }

    private static void handleExport() {
        printStub("Экспорт данных");
    }

    private static void handleShowTables() {
        printStub("Вывод таблиц базы данных");
    }

    private static void printStub(String sectionName) {
        System.out.println("\n[" + sectionName + "] — функция ещё не реализована.\n");
    }

    private static void printSubMenuStub(String title) {
        System.out.println("\n---------------------------");
        System.out.println("   " + title + " (заглушка)");
        System.out.println("---------------------------");
        System.out.println("1. Добавить");
        System.out.println("2. Показать все");
        System.out.println("3. Показать по ID");
        System.out.println("4. Изменить");
        System.out.println("5. Удалить");
        System.out.println("0. Назад");
        System.out.print("Выберите действие: ");

        String input = scanner.nextLine().trim();
        if (!input.equals("0")) {
            System.out.println("Функция ещё не реализована.\n");
        }
    }
}