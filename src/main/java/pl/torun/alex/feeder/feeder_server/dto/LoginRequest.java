package pl.torun.alex.feeder.feeder_server.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
}
