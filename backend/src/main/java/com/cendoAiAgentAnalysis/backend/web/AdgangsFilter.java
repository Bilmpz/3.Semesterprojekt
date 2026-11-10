// ID:          W4
// Delsystem:   Web-API
// Hvad:        Login og adgangstjek – al adgang til data går gennem Web-API (designmål 1). Afklares om det er med i prototypen.
// Se docs/Byggeplan_v0.6.docx


package com.cendoAiAgentAnalysis.backend.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;


@Configuration
@EnableWebSecurity
    public class AdgangsFilter {

        @Bean //første sikkerhedsregel om hvilke endpoints der kræver login
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception { //betyder at metoden kan kaste en evt. fejl videre - håndteres længere oppe
            return http
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/api/health").permitAll()
                            .requestMatchers("/api/**").authenticated()
                            .anyRequest().permitAll()
                    )
                    .httpBasic(Customizer.withDefaults())
                    .build();
                                                                            //Alt der er ovenstående er "Spring Security" selv
                                                                            //Kun variabelnavnet er selvvalgt.

        }

        @Bean                                                               //endnu et objekt spring skal administrere
        public UserDetailsService userDetailsService(                       //UserDetailsService er et interface fra Spring Security til at finde brugere
                @Value("${cendo.security.username}") String username,       //@Value -spring henter værdier fra konfigurationen
                @Value("${cendo.security.password}") String password,
                PasswordEncoder passwordEncoder) {                          //Spring finder automatisk PasswordEncoder -Bean og giver den til metoden. #DependencyINjektion

            UserDetails user = User.builder()
                    .username(username)                                     //angiver brugerens brugernavn
                    .password(passwordEncoder.encode(password))             //
                    .roles("USER")                                          //giver brugeren rollen USER
                    .build();                                               //Spring Security opbygger brugerobjekt
            return new InMemoryUserDetailsManager(user);                    //holder brugeren i hukommelsen uden database
        }

        @Bean
        public PasswordEncoder passwordEncoder(){                           //"PasswordEncoder er interface fra Spring Security. Opretter encoderen som kan hashe og kontrollere passwords, ikke selve passwordet.
            return new BCryptPasswordEncoder();                             //"BCrypt..." er Springs implementering og bruger BCrypt til at hashe kodeord, før det gemmes i brugerobjektet.
        }


    }