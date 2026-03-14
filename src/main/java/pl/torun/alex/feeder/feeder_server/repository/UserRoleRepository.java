package pl.torun.alex.feeder.feeder_server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.torun.alex.feeder.feeder_server.entity.UserRole;
import pl.torun.alex.feeder.feeder_server.entity.UserRoleId;

import java.util.List;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {
    List<UserRole> findByIdUserId(Long userId);
    List<UserRole> findByIdRoleId(Long roleId);
}

