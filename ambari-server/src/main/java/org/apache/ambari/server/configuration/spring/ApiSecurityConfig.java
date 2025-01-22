package org.apache.ambari.server.configuration.spring;

import java.util.Arrays;

import org.apache.ambari.server.security.AmbariEntryPoint;
import org.apache.ambari.server.security.authentication.AmbariDelegatingAuthenticationFilter;
import org.apache.ambari.server.security.authentication.AmbariLocalAuthenticationProvider;
import org.apache.ambari.server.security.authentication.jwt.AmbariJwtAuthenticationProvider;
import org.apache.ambari.server.security.authentication.kerberos.AmbariAuthToLocalUserDetailsService;
import org.apache.ambari.server.security.authentication.kerberos.AmbariKerberosAuthenticationProvider;
import org.apache.ambari.server.security.authentication.kerberos.AmbariKerberosTicketValidator;
import org.apache.ambari.server.security.authentication.kerberos.AmbariProxiedUserDetailsService;
import org.apache.ambari.server.security.authentication.pam.AmbariPamAuthenticationProvider;
import org.apache.ambari.server.security.authorization.AmbariAuthorizationFilter;
import org.apache.ambari.server.security.authorization.AmbariLdapAuthenticationProvider;
import org.apache.ambari.server.security.authorization.internal.AmbariInternalAuthenticationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
@EnableWebSecurity
@Import(GuiceBeansConfig.class)
@ComponentScan("org.apache.ambari.server.security")
public class ApiSecurityConfig {

  private final GuiceBeansConfig guiceBeansConfig;

  @Autowired
  private AmbariEntryPoint ambariEntryPoint;
  @Autowired
  private AmbariDelegatingAuthenticationFilter delegatingAuthenticationFilter;
  @Autowired
  private AmbariAuthorizationFilter authorizationFilter;

  public ApiSecurityConfig(GuiceBeansConfig guiceBeansConfig) {
    this.guiceBeansConfig = guiceBeansConfig;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf().disable()
            .authorizeHttpRequests(authz -> authz.anyRequest().authenticated())
            .headers(headers -> headers.httpStrictTransportSecurity().disable().frameOptions().disable())
            .exceptionHandling(exceptionHandling -> exceptionHandling.authenticationEntryPoint(ambariEntryPoint))
            .addFilterBefore(guiceBeansConfig.ambariUserAuthorizationFilter(), BasicAuthenticationFilter.class)
            .addFilterAt(delegatingAuthenticationFilter, BasicAuthenticationFilter.class)
            .addFilterBefore(authorizationFilter, FilterSecurityInterceptor.class);
    return http.build();
  }

  @Bean
  public AuthenticationManager authenticationManager(
          AmbariJwtAuthenticationProvider ambariJwtAuthenticationProvider,
          AmbariPamAuthenticationProvider ambariPamAuthenticationProvider,
          AmbariLocalAuthenticationProvider ambariLocalAuthenticationProvider,
          AmbariLdapAuthenticationProvider ambariLdapAuthenticationProvider,
          AmbariInternalAuthenticationProvider ambariInternalAuthenticationProvider,
          AmbariKerberosAuthenticationProvider ambariKerberosAuthenticationProvider) {
    return new ProviderManager(Arrays.asList(
            ambariJwtAuthenticationProvider,
            ambariPamAuthenticationProvider,
            ambariLocalAuthenticationProvider,
            ambariLdapAuthenticationProvider,
            ambariInternalAuthenticationProvider,
            ambariKerberosAuthenticationProvider
    ));
  }

  @Bean
  public AmbariKerberosAuthenticationProvider ambariKerberosAuthenticationProvider(
          AmbariKerberosTicketValidator ambariKerberosTicketValidator,
          AmbariAuthToLocalUserDetailsService authToLocalUserDetailsService,
          AmbariProxiedUserDetailsService proxiedUserDetailsService) {

    return new AmbariKerberosAuthenticationProvider(authToLocalUserDetailsService,
            proxiedUserDetailsService,
            ambariKerberosTicketValidator);
  }
}
