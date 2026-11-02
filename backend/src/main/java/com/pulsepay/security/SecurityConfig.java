package com.pulsepay.security;
import org.springframework.context.annotation.*; import org.springframework.security.config.annotation.web.builders.HttpSecurity; import org.springframework.security.config.http.SessionCreationPolicy; import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; import org.springframework.security.crypto.password.PasswordEncoder; import org.springframework.security.web.SecurityFilterChain; import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
@Configuration public class SecurityConfig {
 @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
 @Bean SecurityFilterChain security(HttpSecurity http, JwtFilter filter)throws Exception{return http.csrf(c->c.disable()).cors(c->{}).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)).authorizeHttpRequests(a->a.requestMatchers(org.springframework.http.HttpMethod.POST,"/api/v1/auth/logout").permitAll().requestMatchers("/api/v1/auth/**","/api/v1/status","/actuator/**","/v3/api-docs/**","/swagger-ui/**").permitAll().requestMatchers("/api/v1/api-keys/**","/api/v1/webhooks/**","/api/v1/audit-logs/**").hasAnyRole("ADMIN","MERCHANT").anyRequest().authenticated()).addFilterBefore(filter,UsernamePasswordAuthenticationFilter.class).build();}
}

