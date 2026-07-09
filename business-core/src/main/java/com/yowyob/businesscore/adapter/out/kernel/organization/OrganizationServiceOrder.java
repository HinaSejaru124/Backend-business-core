package com.yowyob.businesscore.adapter.out.kernel.organization;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Ordonne les souscriptions de services organisation selon {@code requiredDependencies}
 * du catalogue kernel ({@code GET /api/organizations/services/catalog}).
 */
final class OrganizationServiceOrder {

    private OrganizationServiceOrder() {
    }

    record CatalogEntry(String code, List<String> requiredDependencies) {
    }

    @SuppressWarnings("unchecked")
    static List<CatalogEntry> parserCatalogue(Object corps) {
        if (!(corps instanceof List<?> items)) {
            return List.of();
        }
        List<CatalogEntry> entries = new ArrayList<>();
        for (Object item : items) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            Object code = map.get("code");
            if (code == null) {
                continue;
            }
            entries.add(new CatalogEntry(code.toString(), lireDependances(map.get("requiredDependencies"))));
        }
        return entries;
    }

    @SuppressWarnings("unchecked")
    private static List<String> lireDependances(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().map(Object::toString).toList();
    }

    /**
     * Tri topologique stable : en cas d'égalité, conserve l'ordre de {@code codes}.
     */
    static List<String> ordonner(List<String> codes, List<CatalogEntry> catalog) {
        if (codes.isEmpty()) {
            return List.of();
        }
        Set<String> codesSouhaites = new HashSet<>(codes);
        Map<String, Integer> indegree = new LinkedHashMap<>();
        Map<String, List<String>> dependants = new LinkedHashMap<>();
        for (String code : codes) {
            indegree.put(code, 0);
            dependants.put(code, new ArrayList<>());
        }
        for (CatalogEntry entry : catalog) {
            if (!codesSouhaites.contains(entry.code())) {
                continue;
            }
            for (String dep : entry.requiredDependencies()) {
                if (!codesSouhaites.contains(dep)) {
                    continue;
                }
                dependants.get(dep).add(entry.code());
                indegree.merge(entry.code(), 1, Integer::sum);
            }
        }

        List<String> ordre = new ArrayList<>();
        Set<String> restants = new LinkedHashSet<>(codes);
        while (ordre.size() < codes.size()) {
            String suivant = null;
            for (String code : codes) {
                if (restants.contains(code) && indegree.get(code) == 0) {
                    suivant = code;
                    break;
                }
            }
            if (suivant == null) {
                for (String code : codes) {
                    if (restants.contains(code)) {
                        ordre.add(code);
                    }
                }
                break;
            }
            ordre.add(suivant);
            restants.remove(suivant);
            for (String dependant : dependants.get(suivant)) {
                indegree.merge(dependant, -1, Integer::sum);
            }
        }
        return ordre;
    }
}
