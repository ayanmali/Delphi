package com.delphi.delphi.configs;

/*
 * Overriding the basic HTTP authentication
 * Check if the given user is in the database, and if so, generate the JWT for the user
 */

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

// import com.diamond.diamond.repositories.AccountRepository;

@Configuration
@PropertySource("classpath:application.properties")
public class AppConfig {
    
    // CORS Configuration (global)
    // @Bean
    // public WebMvcConfigurer corsConfigurer() {
    //     return new WebMvcConfigurer() {
    //         @Override
    //         public void addCorsMappings(CorsRegistry registry) {
    //             registry.addMapping("/").allowedOrigins("http://localhost:3000");
    //         }
    //     };
    // }

    // private final AccountRepository userRepository;

    // public AppConfig(AccountRepository userRepository) {
    //     this.userRepository = userRepository;
    // }

    /*
     * Creates a function to check if a given user exists in the users repository
     */
    // @Bean
    // UserDetailsService userDetailsService() {
    //     return username -> userRepository.findByEmail(username)
    //             .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    // }

    /*
     * Creates an instance of an encoder to encode the user's plain text password
     */
    // @Bean
    // BCryptPasswordEncoder passwordEncoder() {
    //     return new BCryptPasswordEncoder();
    // }

    /*
     * 
     */
    // @Bean
    // public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
    //     return config.getAuthenticationManager();
    // }

    // @Bean
    // AuthenticationProvider authenticationProvider() {
    //     DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();

    //     authProvider.setUserDetailsService(userDetailsService());
    //     authProvider.setPasswordEncoder(passwordEncoder());

    //     return authProvider;
    // }

}

