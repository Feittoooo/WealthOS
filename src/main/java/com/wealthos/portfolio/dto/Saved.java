package com.wealthos.portfolio.dto;

import java.util.List;

// What a create/update returns: the saved item plus message keys for anything the user should know.
public record Saved<T>(T item, List<String> warnings) {

    public static <T> Saved<T> of(T item) {
        return new Saved<>(item, List.of());
    }
}
