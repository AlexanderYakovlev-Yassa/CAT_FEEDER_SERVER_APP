package pl.torun.alex.feeder.feeder_server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleId implements Serializable {

    @Column(name = "user_id_fk")
    private Long userId;

    @Column(name = "role_id_fk")
    private Long roleId;
}

