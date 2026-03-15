package pl.torun.alex.feeder.feeder_server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import pl.torun.alex.feeder.feeder_server.config.AppUserDetails;
import pl.torun.alex.feeder.feeder_server.entity.AppUser;
import pl.torun.alex.feeder.feeder_server.entity.UserRole;
import pl.torun.alex.feeder.feeder_server.repository.AppUserRepository;
import pl.torun.alex.feeder.feeder_server.repository.RoleAuthorityRepository;
import pl.torun.alex.feeder.feeder_server.repository.UserRoleRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleAuthorityRepository roleAuthorityRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser appUser = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        List<GrantedAuthority> authorities = new ArrayList<>();

        List<UserRole> userRoles = userRoleRepository.findByIdUserId(appUser.getId());
        for (UserRole userRole : userRoles) {
            Long roleId = userRole.getId().getRoleId();
            // Add the role itself as an authority (e.g. ROLE_ADMIN)
            if (userRole.getRole() != null) {
                authorities.add(new SimpleGrantedAuthority(userRole.getRole().getRole()));
            }
            // Add all authorities linked to this role
            roleAuthorityRepository.findByIdRoleId(roleId).forEach(ra -> {
                if (ra.getAuthority() != null) {
                    authorities.add(new SimpleGrantedAuthority(ra.getAuthority().getAuthority()));
                }
            });
        }

        return new AppUserDetails(
                appUser.getId(),
                appUser.getUsername(),
                appUser.getPassword(),
                !appUser.isBlocked(),
                authorities
        );
    }
}

