package pl.torun.alex.feeder.feeder_server.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleAuthorityDto {
    private Long roleId;
    private Long authorityId;
}

