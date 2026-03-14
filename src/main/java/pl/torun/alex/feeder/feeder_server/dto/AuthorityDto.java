package pl.torun.alex.feeder.feeder_server.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthorityDto {
    private Long id;
    private String authority;
}

