package ru.mirea.velik.service;

import ru.mirea.velik.exception.BusinessException;
import ru.mirea.velik.exception.EntityNotFoundException;
import ru.mirea.velik.model.Client;
import ru.mirea.velik.repository.ClientRepository;
import ru.mirea.velik.repository.RentalRepository;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ClientService {

    private final ClientRepository clientRepository;
    private final RentalRepository rentalRepository;

    public ClientService(ClientRepository clientRepository, RentalRepository rentalRepository) {
        this.clientRepository = clientRepository;
        this.rentalRepository = rentalRepository;
    }

    public Client create(String fullName, String phone, String email) {
        validateClientData(fullName, phone, email);
        Client client = new Client(fullName.trim(), phone.trim(), email != null ? email.trim() : null);
        return clientRepository.save(client);
    }

    public Client getById(int id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Клиент", id));
    }

    public List<Client> getAll() {
        return clientRepository.findAll();
    }

    public Client update(int id, String fullName, String phone, String email) {
        Client existing = getById(id);
        validateClientData(fullName, phone, email);
        existing.setFullName(fullName.trim());
        existing.setPhone(phone.trim());
        existing.setEmail(email != null ? email.trim() : null);
        return clientRepository.update(existing);
    }

    public void delete(int id) {
        getById(id); // проверка существования
        // Бизнес-правило: нельзя удалить клиента с активными арендами
        if (rentalRepository.hasActiveRentalForClient(id)) {
            throw new BusinessException("Нельзя удалить клиента с id=" + id + ": у него есть активные или созданные аренды");
        }
        if (!clientRepository.deleteById(id)) {
            throw new EntityNotFoundException("Клиент", id);
        }
    }

    public List<Client> searchByName(String namePart) {
        if (namePart == null || namePart.isBlank()) {
            throw new BusinessException("Строка поиска по имени не может быть пустой");
        }
        return clientRepository.findByNameContaining(namePart.trim());
    }

    public List<Client> searchByPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new BusinessException("Телефон для поиска не может быть пустым");
        }
        return clientRepository.findByPhone(phone.trim());
    }

    public List<Client> sortByName() {
        return getAll().stream()
                .sorted(Comparator.comparing(Client::getFullName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    public List<Client> sortByIdDesc() {
        return getAll().stream()
                .sorted(Comparator.comparingInt(Client::getId).reversed())
                .collect(Collectors.toList());
    }

    private void validateClientData(String fullName, String phone, String email) {
        // Бизнес-правило 1: обязательное ФИО
        if (fullName == null || fullName.isBlank()) {
            throw new BusinessException("ФИО клиента обязательно для заполнения");
        }
        // Бизнес-правило 2: обязательный телефон
        if (phone == null || phone.isBlank()) {
            throw new BusinessException("Телефон клиента обязателен для заполнения");
        }
        if (phone.trim().length() < 5) {
            throw new BusinessException("Телефон должен содержать минимум 5 символов");
        }
        if (email != null && !email.isBlank() && !email.contains("@")) {
            throw new BusinessException("Некорректный формат email");
        }
    }
}
