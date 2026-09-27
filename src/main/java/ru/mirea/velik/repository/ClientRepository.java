package ru.mirea.velik.repository;

import ru.mirea.velik.model.Client;

import java.util.List;

public interface ClientRepository extends Repository<Client, Integer> {

    List<Client> findByNameContaining(String namePart);

    List<Client> findByPhone(String phone);
}
