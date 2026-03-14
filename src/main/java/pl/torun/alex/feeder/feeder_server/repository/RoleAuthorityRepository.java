package pl.torun.alex.feeder.feeder_server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.torun.alex.feeder.feeder_server.entity.RoleAuthority;
import pl.torun.alex.feeder.feeder_server.entity.RoleAuthorityId;

import java.util.List;

@Repository
public interface RoleAuthorityRepository extends JpaRepository<RoleAuthority, RoleAuthorityId> {
    List<RoleAuthority> findByIdRoleId(Long roleId);
    List<RoleAuthority> findByIdAuthorityId(Long authorityId);
}

