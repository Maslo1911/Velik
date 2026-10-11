package ru.mirea.velik.fx;

import ru.mirea.velik.repository.BikeRepository;
import ru.mirea.velik.repository.ClientRepository;
import ru.mirea.velik.repository.JdbcBikeRepository;
import ru.mirea.velik.repository.JdbcClientRepository;
import ru.mirea.velik.repository.JdbcRentalRepository;
import ru.mirea.velik.repository.RentalRepository;
import ru.mirea.velik.service.BikeService;
import ru.mirea.velik.service.ClientService;
import ru.mirea.velik.service.ExportService;
import ru.mirea.velik.service.RentalService;
import ru.mirea.velik.service.StatisticsService;

/**
 * Сборка зависимостей приложения: репозитории создаются один раз,
 * контроллеры получают только сервисы.
 */
public final class AppContext {

    private final ClientService clientService;
    private final BikeService bikeService;
    private final RentalService rentalService;
    private final StatisticsService statisticsService;
    private final ExportService exportService;
    private final ViewLoader viewLoader;

    public AppContext() {
        ClientRepository clientRepository = new JdbcClientRepository();
        BikeRepository bikeRepository = new JdbcBikeRepository();
        RentalRepository rentalRepository = new JdbcRentalRepository();

        this.clientService = new ClientService(clientRepository, rentalRepository);
        this.bikeService = new BikeService(bikeRepository, rentalRepository);
        this.rentalService = new RentalService(rentalRepository, clientRepository, bikeRepository, bikeService);
        this.statisticsService = new StatisticsService(rentalService, clientService, bikeService);
        this.exportService = new ExportService();
        this.viewLoader = new ViewLoader(this);
    }

    public ClientService clientService() {
        return clientService;
    }

    public BikeService bikeService() {
        return bikeService;
    }

    public RentalService rentalService() {
        return rentalService;
    }

    public StatisticsService statisticsService() {
        return statisticsService;
    }

    public ExportService exportService() {
        return exportService;
    }

    public ViewLoader viewLoader() {
        return viewLoader;
    }
}
