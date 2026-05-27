package pl.commercelink.pim.api;

import java.util.List;

public record Brand(String name, List<String> aliases) {

    public Brand {
        aliases = aliases == null ? List.of() : List.copyOf(aliases);
    }

    public boolean matches(String input) {
        if (input == null) {
            return false;
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        if (name != null && name.equalsIgnoreCase(trimmed)) {
            return true;
        }
        for (String alias : aliases) {
            if (alias.equalsIgnoreCase(trimmed)) {
                return true;
            }
        }
        return false;
    }
}
