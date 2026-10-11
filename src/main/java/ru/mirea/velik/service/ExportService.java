package ru.mirea.velik.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.mirea.velik.model.RentalView;
import ru.mirea.velik.util.Formats;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Экспорт набора заявок в файл .xlsx или .csv (формат определяется расширением файла).
 */
public class ExportService {

    private static final String[] HEADERS = {
            "№ заявки", "Клиент", "Телефон", "Велосипед", "Тип", "Начало", "Окончание", "Статус", "Стоимость, ₽"
    };
    private static final char CSV_SEPARATOR = ';';

    public void export(List<RentalView> rentals, Path file) throws IOException {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".csv")) {
            exportCsv(rentals, file);
        } else if (name.endsWith(".xlsx")) {
            exportXlsx(rentals, file);
        } else {
            throw new IOException("Неподдерживаемый формат файла. Выберите .xlsx или .csv");
        }
    }

    private void exportXlsx(List<RentalView> rentals, Path file) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Аренды");

            CellStyle headerStyle = workbook.createCellStyle();
            Font bold = workbook.createFont();
            bold.setBold(true);
            headerStyle.setFont(bold);

            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (RentalView view : rentals) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(view.id());
                String[] texts = textColumns(view);
                for (int i = 0; i < texts.length; i++) {
                    row.createCell(i + 1).setCellValue(texts[i]);
                }
                row.createCell(HEADERS.length - 1).setCellValue(view.totalCost());
            }
            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (OutputStream out = Files.newOutputStream(file)) {
                workbook.write(out);
            }
        }
    }

    private void exportCsv(List<RentalView> rentals, Path file) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write('﻿'); // BOM, чтобы Excel правильно определил кодировку UTF-8
            writer.write(csvLine(HEADERS));
            for (RentalView view : rentals) {
                String[] texts = textColumns(view);
                String[] cells = new String[HEADERS.length];
                cells[0] = String.valueOf(view.id());
                System.arraycopy(texts, 0, cells, 1, texts.length);
                cells[HEADERS.length - 1] = String.format(Locale.ROOT, "%.2f", view.totalCost());
                writer.write(csvLine(cells));
            }
        }
    }

    /** Текстовые столбцы между номером заявки и стоимостью. */
    private String[] textColumns(RentalView view) {
        return new String[]{
                view.clientName(),
                view.clientPhone(),
                view.bikeModel(),
                view.bikeType(),
                view.startTime() != null ? Formats.dateTime(view.startTime()) : "",
                view.endTime() != null ? Formats.dateTime(view.endTime()) : "",
                view.status().getTitle()
        };
    }

    private String csvLine(String[] cells) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                line.append(CSV_SEPARATOR);
            }
            line.append(escapeCsv(cells[i]));
        }
        return line.append("\r\n").toString();
    }

    private String escapeCsv(String value) {
        if (value.indexOf(CSV_SEPARATOR) >= 0 || value.contains("\"") || value.contains("\n")) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }
}
