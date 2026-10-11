package ru.mirea.velik.fx;

import javafx.fxml.FXMLLoader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;

/**
 * Загрузка FXML-разметки. Контроллеры создаются через фабрику,
 * которая передаёт им {@link AppContext} с сервисами.
 */
public final class ViewLoader {

    private static final String FXML_ROOT = "/fxml/";
    private static final String STYLESHEET = "/css/app.css";

    private final AppContext context;

    ViewLoader(AppContext context) {
        this.context = context;
    }

    /** Загружает разметку и возвращает её корневой узел. */
    public <T> T load(String fxmlName) {
        return this.<T, Object>loaded(fxmlName).root();
    }

    /** Загружает разметку и возвращает корневой узел вместе с контроллером. */
    public <T, C> Loaded<T, C> loaded(String fxmlName) {
        FXMLLoader loader = new FXMLLoader(resource(FXML_ROOT + fxmlName));
        loader.setControllerFactory(this::createController);
        try {
            T root = loader.load();
            return new Loaded<>(root, loader.getController());
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось загрузить разметку " + fxmlName, e);
        }
    }

    public static String stylesheet() {
        return resource(STYLESHEET).toExternalForm();
    }

    private Object createController(Class<?> type) {
        try {
            return type.getConstructor(AppContext.class).newInstance(context);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Не удалось создать контроллер " + type.getName(), e);
        }
    }

    private static URL resource(String path) {
        URL url = ViewLoader.class.getResource(path);
        if (url == null) {
            throw new IllegalStateException("Ресурс не найден: " + path);
        }
        return url;
    }

    public record Loaded<T, C>(T root, C controller) {
    }
}
