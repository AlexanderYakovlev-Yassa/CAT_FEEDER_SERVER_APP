package pl.torun.alex.feeder.feeder_server.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "role_authority")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleAuthority {

    @EmbeddedId
    private RoleAuthorityId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("roleId")
    @JoinColumn(name = "role_id_fk")
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("authorityId")
    @JoinColumn(name = "authority_id_fk")
    private Authority authority;
}

