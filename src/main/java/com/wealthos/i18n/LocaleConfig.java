package com.wealthos.i18n;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;
import java.util.Locale;

/**
 * The frontend sends the language the user picked in Accept-Language on every call, so error
 * messages and validation texts come back in that language.
 */
@Configuration
public class LocaleConfig {

    public static final Locale SPANISH = Locale.forLanguageTag("es-ES");
    public static final List<String> LANGUAGES = List.of("en", "es");

    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setSupportedLocales(List.of(Locale.ENGLISH, SPANISH));
        resolver.setDefaultLocale(Locale.ENGLISH);
        return resolver;
    }
}
