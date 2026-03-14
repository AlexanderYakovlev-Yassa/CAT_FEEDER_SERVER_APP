package pl.torun.alex.feeder.feeder_server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleAuthorityId implements Serializable {

    @Column(name = "role_id_fk")
    private Long roleId;

    @Column(name = "authority_id_fk")
    private Long authorityId;
}

