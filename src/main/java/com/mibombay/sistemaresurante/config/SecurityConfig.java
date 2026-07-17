package com.mibombay.sistemaresurante.config;

import com.mibombay.sistemaresurante.security.CustomAuthenticationFilter;
import com.mibombay.sistemaresurante.security.CustomAuthenticationProvider;
import com.mibombay.sistemaresurante.security.CustomUserDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomAuthenticationProvider customAuthenticationProvider;

    public SecurityConfig(CustomAuthenticationProvider customAuthenticationProvider) {
        this.customAuthenticationProvider = customAuthenticationProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain superadminFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/superadmin/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/superadmin/login", "/superadmin/logout").permitAll()
                        .requestMatchers("/superadmin/**").hasRole("SUPERADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/superadmin/login")
                        .loginProcessingUrl("/superadmin/login")
                        .defaultSuccessUrl("/superadmin/dashboard")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/superadmin/logout")
                        .logoutSuccessUrl("/superadmin/login?logout")
                        .permitAll()
                )
                .authenticationProvider(customAuthenticationProvider)
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain normalFilterChain(HttpSecurity http, AuthenticationManager authManager) throws Exception {
        CustomAuthenticationFilter customFilter = new CustomAuthenticationFilter(authManager);
        customFilter.setFilterProcessesUrl("/login");
        customFilter.setUsernameParameter("username");
        customFilter.setPasswordParameter("password");
        customFilter.setSecurityContextRepository(new HttpSessionSecurityContextRepository());

        customFilter.setRequiresAuthenticationRequestMatcher(
                new AntPathRequestMatcher("/login", "POST")
        );

        customFilter.setAuthenticationSuccessHandler((request, response, authentication) -> {
            if (authentication.getPrincipal() instanceof CustomUserDetails user && user.isEsSuperadmin()) {
                response.sendRedirect("/superadmin/dashboard");
            } else {
                response.sendRedirect("/dashboard");
            }
        });

        customFilter.setAuthenticationFailureHandler((request, response, exception) -> {
            response.sendRedirect("/login?error");
        });

        http.securityMatcher("/**")
                .addFilterAt(customFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/login?**", "/registro-empresa", "/registro-exito", "/css/**", "/js/**", "/ws/**").permitAll()
                        .requestMatchers("/usuarios/**").hasRole("ADMIN")
                        .requestMatchers("/productos/**").hasRole("ADMIN")
                        .requestMatchers("/clientes/**").hasAnyRole("ADMIN", "CAJERO")
                        .requestMatchers("/proveedores/**").hasRole("ADMIN")
                        .requestMatchers("/compras/**").hasRole("ADMIN")
                        .requestMatchers("/ventas/**").hasAnyRole("ADMIN", "CAJERO")
                        .requestMatchers("/cierzx/**").hasAnyRole("ADMIN", "CAJERO")
                        .requestMatchers("/cierrez/**").hasRole("ADMIN")
                        .requestMatchers("/inventario-fisico/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"))
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                .authenticationProvider(customAuthenticationProvider)
                .csrf(csrf -> csrf.disable());

        return http.build();
    }
}
