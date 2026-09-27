package ru.mirea.velik.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.mirea.velik.model.Bike;
import ru.mirea.velik.model.Client;
import ru.mirea.velik.model.Rental;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Экспорт основных данных системы в Excel (.xlsx).
 */
public class ExcelExporter {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public void export(List<Client> clients, List<Bike> bikes, List<Rental> rentals, String filePath) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            createClientsSheet(workbook, clients);
            createBikesSheet(workbook, bikes);
            createRentalsSheet(workbook, rentals);

            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
            }
        }
    }

    private void createClientsSheet(Workbook workbook, List<Client> clients) {
        Sheet sheet = workbook.createSheet("Клиенты");
        String[] headers = {"ID", "ФИО", "Телефон", "Email"};
        createHeaderRow(sheet, headers, workbook);

        int rowNum = 1;
        for (Client c : clients) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(c.getId());
            row.createCell(1).setCellValue(c.getFullName());
            row.createCell(2).setCellValue(c.getPhone());
            row.createCell(3).setCellValue(c.getEmail() != null ? c.getEmail() : "");
        }
        autoSize(sheet, headers.length);
    }

    private void createBikesSheet(Workbook workbook, List<Bike> bikes) {
        Sheet sheet = workbook.createSheet("Велосипеды");
        String[] headers = {"ID", "Модель", "Тип", "Доступен", "Цена/час"};
        createHeaderRow(sheet, headers, workbook);

        int rowNum = 1;
        for (Bike b : bikes) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(b.getId());
            row.createCell(1).setCellValue(b.getModel());
            row.createCell(2).setCellValue(b.getType());
            row.createCell(3).setCellValue(b.isAvailable() ? "да" : "нет");
            row.createCell(4).setCellValue(b.getPricePerHour());
        }
        autoSize(sheet, headers.length);
    }

    private void createRentalsSheet(Workbook workbook, List<Rental> rentals) {
        Sheet sheet = workbook.createSheet("Аренды");
        String[] headers = {"ID", "ID клиента", "ID велосипеда", "Начало", "Конец", "Статус", "Стоимость"};
        createHeaderRow(sheet, headers, workbook);

        int rowNum = 1;
        for (Rental r : rentals) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(r.getId());
            row.createCell(1).setCellValue(r.getClientId());
            row.createCell(2).setCellValue(r.getBikeId());
            row.createCell(3).setCellValue(r.getStartTime() != null ? r.getStartTime().format(DTF) : "");
            row.createCell(4).setCellValue(r.getEndTime() != null ? r.getEndTime().format(DTF) : "");
            row.createCell(5).setCellValue(r.getStatus() != null ? r.getStatus().name() : "");
            row.createCell(6).setCellValue(r.getTotalCost());
        }
        autoSize(sheet, headers.length);
    }

    private void createHeaderRow(Sheet sheet, String[] headers, Workbook workbook) {
        Row header = sheet.createRow(0);
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    private void autoSize(Sheet sheet, int columns) {
        for (int i = 0; i < columns; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}
