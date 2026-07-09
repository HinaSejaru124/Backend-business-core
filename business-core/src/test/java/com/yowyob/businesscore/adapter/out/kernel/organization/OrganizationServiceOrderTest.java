package com.yowyob.businesscore.adapter.out.kernel.organization;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrganizationServiceOrderTest {

    @Test
    void ordonne_selon_les_dependances_du_catalogue() {
        List<OrganizationServiceOrder.CatalogEntry> catalog = List.of(
                new OrganizationServiceOrder.CatalogEntry("BILLING", List.of("COMMERCIAL")),
                new OrganizationServiceOrder.CatalogEntry("CASHIER", List.of("ACCOUNTING")),
                new OrganizationServiceOrder.CatalogEntry("COMMERCIAL", List.of()),
                new OrganizationServiceOrder.CatalogEntry("ACCOUNTING", List.of()));

        List<String> ordered = OrganizationServiceOrder.ordonner(
                List.of("BILLING", "CASHIER", "COMMERCIAL", "ACCOUNTING"),
                catalog);

        assertThat(ordered).containsExactlyInAnyOrder("COMMERCIAL", "ACCOUNTING", "BILLING", "CASHIER");
        assertThat(ordered.indexOf("COMMERCIAL")).isLessThan(ordered.indexOf("BILLING"));
        assertThat(ordered.indexOf("ACCOUNTING")).isLessThan(ordered.indexOf("CASHIER"));
    }

    @Test
    void conserve_l_ordre_configure_si_catalogue_vide() {
        List<String> codes = List.of("COMMERCIAL", "ACCOUNTING", "BILLING", "CASHIER");

        assertThat(OrganizationServiceOrder.ordonner(codes, List.of())).containsExactlyElementsOf(codes);
    }
}
