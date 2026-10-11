package ru.mirea.velik.service;

import ru.mirea.velik.exception.BusinessException;
import ru.mirea.velik.exception.EntityNotFoundException;
import ru.mirea.velik.exception.ValidationException;
import ru.mirea.velik.model.Client;
import ru.mirea.velik.repository.ClientRepository;
import ru.mirea.velik.repository.RentalRepository;

import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ClientService {

    // Длины соответствуют столбцам таблицы clients
    private static final int MAX_NAME_LENGTH = 150;
    private static final int MAX_PHONE_LENGTH = 30;
    private static final int MAX_EMAIL_LENGTH = 100;
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\+?[0-9()\\-\\s]+");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

    private final ClientRepository clientRepository;
    private final RentalRepository rentalRepository;

    public ClientService(ClientRepository clientRepository, RentalRepository rentalRepository) {
        this.clientRepository = clientRepository;
        this.rentalRepository = rentalRepository;
    }

    public Client create(String fullName, String phone, String email) {
        validateClientData(null, fullName, phone, email);
        Client client = new Client(fullName.trim(), phone.trim(), normalizeEmail(email));
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
        validateClientData(id, fullName, phone, email);
        existing.setFullName(fullName.trim());
        existing.setPhone(phone.trim());
        existing.setEmail(normalizeEmail(email));
        return clientRepository.update(existing);
    }

    private String normalizeEmail(String email) {
        return email == null || email.isBlank() ? null : email.trim();
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

    /**
     * @param selfId id редактируемого клиента или null при создании (нужен для проверки уникальности телефона)
     */
    private void validateClientData(Integer selfId, String fullName, String phone, String email) {
        // Бизнес-правило 1: обязательное ФИО
        if (fullName == null || fullName.isBlank()) {
            throw new ValidationException("fullName", "ФИО клиента обязательно для заполнения");
        }
        if (fullName.trim().length() > MAX_NAME_LENGTH) {
            throw new ValidationException("fullName", "ФИО не должно быть длиннее " + MAX_NAME_LENGTH + " символов");
        }
        // Бизнес-правило 2: обязательный телефон
        if (phone == null || phone.isBlank()) {
            throw new ValidationException("phone", "Телефон клиента обязателен для заполнения");
        }
        String trimmedPhone = phone.trim();
        if (trimmedPhone.length() < 5) {
            throw new ValidationException("phone", "Телефон должен содержать минимум 5 символов");
        }
        if (trimmedPhone.length() > MAX_PHONE_LENGTH) {
            throw new ValidationException("phone", "Телефон не должен быть длиннее " + MAX_PHONE_LENGTH + " символов");
        }
        if (!PHONE_PATTERN.matcher(trimmedPhone).matches()) {
            throw new ValidationException("phone", "Телефон может содержать только цифры, пробелы, скобки, дефис и знак + в начале");
        }
        // Телефон уникален (ограничение uq_clients_phone в базе данных)
        boolean phoneTaken = clientRepository.findByPhone(trimmedPhone).stream()
                .anyMatch(other -> selfId == null || other.getId() != selfId);
        if (phoneTaken) {
            throw new ValidationException("phone", "Клиент с телефоном " + trimmedPhone + " уже существует");
        }
        if (email != null && !email.isBlank()) {
            String trimmedEmail = email.trim();
            if (trimmedEmail.length() > MAX_EMAIL_LENGTH) {
                throw new ValidationException("email", "Email не должен быть длиннее " + MAX_EMAIL_LENGTH + " символов");
            }
            if (!EMAIL_PATTERN.matcher(trimmedEmail).matches()) {
                throw new ValidationException("email", "Некорректный формат email. Пример: name@mail.ru");
            }
        }
    }
}
