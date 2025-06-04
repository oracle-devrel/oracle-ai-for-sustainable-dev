import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig { //} extends WebSecurityConfigurerAdapter {

//    @Override
//    protected void configure(HttpSecurity http) throws Exception {
//        http
//                .headers()
//                .frameOptions().disable() // Disable X-Frame-Options header
//                .and()
//                .authorizeRequests()
//                .anyRequest().permitAll(); // Allow all requests (adjust as needed)
//    }

//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        http
//                .authorizeHttpRequests(auth -> auth
//                        .requestMatchers("/public").permitAll() // Allow public access
//                        .anyRequest().authenticated()  // Require authentication for all other endpoints
//                )
//                .httpBasic(); // Enable Basic Authentication
//
//        return http.build();
//    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/public").permitAll() // Allow public access
                        .anyRequest().permitAll()  // Require authentication for all other endpoints
                )
                .headers()
                .frameOptions().disable() // Disable X-Frame-Options header
                .and()
                .authorizeRequests()
//                .anyRequest().permitAll() // Allow all requests (adjust as needed)
                ; // Enable Basic Authentication



        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails user = User.withDefaultPasswordEncoder()
                .username("oracleai")
                .password("oracleai")
                .roles("USER")
                .build();

        return new InMemoryUserDetailsManager(user);
    }
}
