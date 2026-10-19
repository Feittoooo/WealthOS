package com.wealthos.i18n;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/i18n")
public class I18nController {

    private final MessageBundles bundles;

    public I18nController(MessageBundles bundles) {
        this.bundles = bundles;
    }

    @GetMapping("/{lang}")
    public ResponseEntity<Map<String, String>> messages(@PathVariable String lang) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .body(bundles.forLanguage(lang));
    }
}
