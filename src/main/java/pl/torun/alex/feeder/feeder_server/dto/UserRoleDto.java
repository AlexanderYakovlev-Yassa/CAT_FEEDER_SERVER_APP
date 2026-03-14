package pl.torun.alex.feeder.feeder_server.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRoleDto {
    private Long userId;
    private Long roleId;
}

