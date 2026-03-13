package pl.torun.alex.feeder.feeder_server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.torun.alex.feeder.feeder_server.entity.AppUser;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
}
