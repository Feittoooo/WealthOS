package com.wealthos.i18n;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

// Translations for backend code (errors, activity texts). Uses the locale of the current request.
@Component
public class Messages {

    private final MessageSource source;

    public Messages(MessageSource source) {
        this.source = source;
    }

    public String get(String code, Object... args) {
        return source.getMessage(code, args, code, locale());
    }

    public Locale locale() {
        return LocaleContextHolder.getLocale();
    }

    public boolean isSpanish() {
        return "es".equals(locale().getLanguage());
    }
}
