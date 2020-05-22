package taylor.programming.bootmoneymanager2.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

import javax.sql.DataSource;

@Configuration
@EnableWebSecurity
public class ApplicationSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    DataSource dataSource;

   @Autowired
    public ApplicationSecurityConfig(){

   }

    @Override
    protected void configure(HttpSecurity http) throws Exception {


         http
                 .csrf().disable()
                 .authorizeRequests()
                 .antMatchers("/resources/static/**","/resources/templates/fragments/**","/about", "/*.css", "/footer.html", "/footer.th.xml"
                    , "/*.jpg", "/news.th.xml", "/backGroundImageV6.jpg", "/resources/static/images/**",
                 "/images/**","/register","/account-creation/**","/.well-known/**", "/css/**", "/confirm-account**", "/verification_email_sent**").permitAll() // White list PAGES
                 .anyRequest()
                 .authenticated()
                 .and()
                 .formLogin()
                .loginPage("/login").permitAll()
                .defaultSuccessUrl("/perform_login",true)
                 .failureUrl("/login_error1")
                 .and()
                 .logout()
                    .logoutUrl("/logoutHiddingRus")
                    .clearAuthentication(true)
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
                    .logoutSuccessUrl("/login");

         http.sessionManagement().maximumSessions(1).expiredUrl("/prelogout");


    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {

       auth
               .jdbcAuthentication()
               .dataSource(dataSource)
             //  .passwordEncoder(passwordEncoder)
               .usersByUsernameQuery("select username, password, enabled "
                + "from MONEYMANAGERUSERS "
                + "where username = ?")
               .authoritiesByUsernameQuery("select username, authority "
               + "from authorities "
               + "where username = ?");
    }

}
