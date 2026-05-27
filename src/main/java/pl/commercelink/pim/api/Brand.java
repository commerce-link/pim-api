package pl.commercelink.pim.api;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public record Brand(String name, List<String> aliases) {

    public Brand {
        aliases = aliases == null ? List.of() : List.copyOf(aliases);
    }

    public boolean matches(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }
        String candidate = input.trim();
        return Stream.concat(Stream.of(name), aliases.stream())
                .filter(Objects::nonNull)
                .anyMatch(candidate::equalsIgnoreCase);
    }
}
