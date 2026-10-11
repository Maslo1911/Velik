package ru.mirea.velik.fx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Control;
import javafx.scene.control.TextField;
import ru.mirea.velik.fx.AppContext;
import ru.mirea.velik.model.Client;

import java.util.Map;

/**
 * Форма клиента.
 */
public class ClientFormController extends FormController {

    private final AppContext context;

    @FXML
    private TextField fullNameField;
    @FXML
    private TextField phoneField;
    @FXML
    private TextField emailField;

    /** Редактируемый клиент или null при создании нового. */
    private Client editing;

    public ClientFormController(AppContext context) {
        this.context = context;
    }

    public void setClient(Client client) {
        this.editing = client;
        if (client != null) {
            fullNameField.setText(client.getFullName());
            phoneField.setText(client.getPhone());
            emailField.setText(client.getEmail());
        }
    }

    @Override
    protected void save() {
        if (editing == null) {
            context.clientService().create(fullNameField.getText(), phoneField.getText(), emailField.getText());
        } else {
            context.clientService().update(editing.getId(), fullNameField.getText(), phoneField.getText(), emailField.getText());
        }
    }

    @Override
    protected Map<String, Control> fields() {
        return Map.of(
                "fullName", fullNameField,
                "phone", phoneField,
                "email", emailField
        );
    }
}
