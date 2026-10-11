package ru.mirea.velik.fx.controller;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import ru.mirea.velik.fx.AppContext;
import ru.mirea.velik.fx.util.FormDialog;
import ru.mirea.velik.model.Client;
import ru.mirea.velik.model.RentalView;

import java.util.List;

/**
 * Раздел «Клиенты».
 */
public class ClientsController extends DirectoryController<Client> {

    @FXML
    private TableColumn<Client, String> nameColumn;
    @FXML
    private TableColumn<Client, String> phoneColumn;
    @FXML
    private TableColumn<Client, String> emailColumn;

    public ClientsController(AppContext context) {
        super(context);
    }

    @FXML
    private void initialize() {
        nameColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getFullName()));
        phoneColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getPhone()));
        emailColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                cell.getValue().getEmail() != null ? cell.getValue().getEmail() : "—"));
        initDirectory();
    }

    @Override
    protected List<Client> loadItems() {
        return context.clientService().getAll();
    }

    @Override
    protected boolean matches(Client client, String query) {
        return contains(client.getFullName(), query)
                || contains(client.getPhone(), query)
                || contains(client.getEmail(), query);
    }

    @Override
    protected String describe(Client client) {
        return client.getFullName() + " (" + client.getPhone() + ")";
    }

    @Override
    protected void delete(Client client) {
        context.clientService().delete(client.getId());
    }

    @Override
    protected boolean openForm(Client client) {
        String title = client == null ? "Новый клиент" : "Изменение клиента";
        return FormDialog.<ClientFormController>show(window(), context, "client-form.fxml", title,
                form -> form.setClient(client));
    }

    @Override
    protected List<RentalView> rentalsOf(Client client) {
        return context.rentalService().getViewsByClient(client.getId());
    }
}
