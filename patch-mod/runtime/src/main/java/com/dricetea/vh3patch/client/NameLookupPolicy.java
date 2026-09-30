package com.dricetea.vh3patch.client;

import java.util.function.Function;
import java.util.function.Predicate;

/** Stateless lookup: each invocation observes current language resources. */
public final class NameLookupPolicy {
    private NameLookupPolicy() {}

    public static String resolve(String id, String fallback, Function<String, String> entityKey,
                                 Predicate<String> hasTranslation, Function<String, String> translate) {
        // Adapter returns null for invalid and unregistered IDs, before default registry lookup.
        String actualEntityKey = entityKey.apply(id);
        if (actualEntityKey == null) return fallback;
        String override = "vh3_translation_patch.mob." + id.replace(':', '.');
        if (hasTranslation.test(override)) return translate.apply(override);
        if (hasTranslation.test(actualEntityKey)) return translate.apply(actualEntityKey);
        return fallback;
    }
}
