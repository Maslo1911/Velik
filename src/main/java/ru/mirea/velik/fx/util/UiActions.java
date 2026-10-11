package ru.mirea.velik.fx.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mirea.velik.exception.BusinessException;
import ru.mirea.velik.exception.DatabaseException;
import ru.mirea.velik.exception.EntityNotFoundException;

/**
 * Единая обработка ошибок для действий пользователя: исключения сервисов и базы данных
 * превращаются в понятные сообщения, технические подробности уходят в журнал.
 */
public final class UiActions {

    private static final Logger log = LoggerFactory.getLogger(UiActions.class);

    private UiActions() {
    }

    /** @return true, если действие выполнено без ошибок */
    public static boolean run(Runnable action) {
        try {
            action.run();
            return true;
        } catch (BusinessException | EntityNotFoundException e) {
            Alerts.warning(e.getMessage());
        } catch (DatabaseException e) {
            log.error("Ошибка работы с базой данных", e);
            Alerts.error("Ошибка работы с базой данных", e.getMessage());
        }
        return false;
    }
}
