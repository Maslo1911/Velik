package ru.mirea.velik.fx;

import javafx.application.Application;

/**
 * Точка входа для запуска из IDE и из JAR: класс не наследует Application,
 * поэтому JavaFX может находиться в classpath, а не только в module path.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        Application.launch(VelikApplication.class, args);
    }
}
