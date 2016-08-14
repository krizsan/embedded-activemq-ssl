package se.ivankrizsan.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;

/**
 * Configuration that enables injection of configuration property values in Spring-based tests.
 *
 * @author Ivan Krizsan
 */
@Configuration
public class ReadTestPropertiesConfiguration {
    /**
     * Bean that enables injection of configuration property values.
     *
     * @return PropertySourcesPlaceholderConfigurer bean.
     */
    @Bean
    public static PropertySourcesPlaceholderConfigurer testProperties() {
        final PropertySourcesPlaceholderConfigurer theConfigurer = new PropertySourcesPlaceholderConfigurer();
        theConfigurer.setLocation(new ClassPathResource("embedded-activemq-ssl.properties"));
        return theConfigurer;
    }
}
