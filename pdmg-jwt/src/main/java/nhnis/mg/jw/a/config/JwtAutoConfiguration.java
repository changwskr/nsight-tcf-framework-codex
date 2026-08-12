package nhnis.mg.jw.a.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.sql.DataSource;

@Configuration
@EnableConfigurationProperties({JwtSecurityProperties.class, JwtInternalCallProperties.class})
public class JwtAutoConfiguration {

    @Bean
    public PasswordEncoder jwtPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtSchemaInitializer jwtSchemaInitializer(@Qualifier("rdwDataSource") DataSource rdwDataSource,
                                                     JwtSecurityProperties properties) {
        JwtSchemaInitializer initializer = new JwtSchemaInitializer(new JdbcTemplate(rdwDataSource), properties);
        initializer.init();
        return initializer;
    }
}
