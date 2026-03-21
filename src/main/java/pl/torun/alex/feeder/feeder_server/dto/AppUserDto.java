package pl.torun.alex.feeder.feeder_server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppUserDto {
    private Long id;
    private String username;
    private String password;
    private boolean blocked;

    // IDs of devices this user manages
    private Set<Long> deviceIds = new HashSet<>();
}
