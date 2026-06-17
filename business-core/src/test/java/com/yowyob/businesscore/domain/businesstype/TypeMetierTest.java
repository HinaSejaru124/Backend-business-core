package com.yowyob.businesscore.domain.businesstype;

import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.domain.shared.StatutType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitaires du domaine pur — TypeMetier + VersionType.
 * Zéro Spring, zéro base de données, zéro réseau.
 * Vérifie les règles métier fondamentales.
 */
class TypeMetierTest {

    private static final UUID TENANT_A = UUID.randomUUID();
    private static final UUID TENANT_B = UUID.randomUUID();

    // ══════════════════════════════════════════════════════════════════════
    // Création
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Création d'un TypeMetier")
    class Creation {

        @Test
        @DisplayName("doit créer un type en BROUILLON avec le code normalisé en majuscules")
        void creer_normalise_le_code_en_majuscules() {
            TypeMetier type = TypeMetier.creer(TENANT_A, "pharmacie", "Pharmacie", null);

            assertThat(type.statut()).isEqualTo(StatutType.BROUILLON);
            assertThat(type.code()).isEqualTo("PHARMACIE");
            assertThat(type.nom()).isEqualTo("Pharmacie");
            assertThat(type.id()).isNotNull();
            assertThat(type.tenantId()).isEqualTo(TENANT_A);
        }

        @Test
        @DisplayName("doit rejeter un code vide")
        void creer_rejette_code_vide() {
            assertThatThrownBy(() -> TypeMetier.creer(TENANT_A, "", "Pharmacie", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("code");
        }

        @Test
        @DisplayName("doit rejeter un nom vide")
        void creer_rejette_nom_vide() {
            assertThatThrownBy(() -> TypeMetier.creer(TENANT_A, "PHARMA", "", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("nom");
        }

        @Test
        @DisplayName("doit rejeter un tenantId null")
        void creer_rejette_tenant_null() {
            assertThatThrownBy(() -> TypeMetier.creer(null, "PHARMA", "Pharmacie", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("tenantId");
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Cycle de vie
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Cycle de vie : BROUILLON → PUBLIE → ARCHIVE")
    class CycleVie {

        @Test
        @DisplayName("doit passer de BROUILLON à PUBLIE")
        void publier_depuis_brouillon_ok() {
            TypeMetier type = TypeMetier.creer(TENANT_A, "PHARMA", "Pharmacie", null);

            TypeMetier publie = type.publier();

            assertThat(publie.statut()).isEqualTo(StatutType.PUBLIE);
            // L'original est inchangé (record immuable)
            assertThat(type.statut()).isEqualTo(StatutType.BROUILLON);
        }

        @Test
        @DisplayName("ne doit pas publier un type déjà PUBLIE")
        void publier_depuis_publie_leve_conflit() {
            TypeMetier publie = TypeMetier.creer(TENANT_A, "PHARMA", "Pharmacie", null)
                    .publier();

            assertThatThrownBy(publie::publier)
                    .isInstanceOf(ProblemException.class)
                    .hasMessageContaining("BROUILLON");
        }

        @Test
        @DisplayName("doit passer de PUBLIE à ARCHIVE")
        void archiver_depuis_publie_ok() {
            TypeMetier archive = TypeMetier.creer(TENANT_A, "PHARMA", "Pharmacie", null)
                    .publier()
                    .archiver();

            assertThat(archive.statut()).isEqualTo(StatutType.ARCHIVE);
        }

        @Test
        @DisplayName("ne doit pas archiver un BROUILLON")
        void archiver_depuis_brouillon_leve_conflit() {
            TypeMetier brouillon = TypeMetier.creer(TENANT_A, "PHARMA", "Pharmacie", null);

            assertThatThrownBy(brouillon::archiver)
                    .isInstanceOf(ProblemException.class)
                    .hasMessageContaining("PUBLIE");
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Gardes métier
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Gardes métier")
    class Gardes {

        @Test
        @DisplayName("verifierAppartenance doit passer pour le bon tenant")
        void appartenance_ok_pour_bon_tenant() {
            TypeMetier type = TypeMetier.creer(TENANT_A, "PHARMA", "Pharmacie", null);

            assertThatNoException().isThrownBy(() -> type.verifierAppartenance(TENANT_A));
        }

        @Test
        @DisplayName("verifierAppartenance doit rejeter un tenant étranger")
        void appartenance_rejette_tenant_etranger() {
            TypeMetier type = TypeMetier.creer(TENANT_A, "PHARMA", "Pharmacie", null);

            assertThatThrownBy(() -> type.verifierAppartenance(TENANT_B))
                    .isInstanceOf(ProblemException.class)
                    .hasMessageContaining("tenant");
        }

        @Test
        @DisplayName("verifierPeutVersionner doit passer sur un type PUBLIE")
        void peut_versionner_si_publie() {
            TypeMetier publie = TypeMetier.creer(TENANT_A, "PHARMA", "Pharmacie", null)
                    .publier();

            assertThatNoException().isThrownBy(publie::verifierPeutVersionner);
        }

        @Test
        @DisplayName("verifierPeutVersionner doit rejeter un BROUILLON")
        void ne_peut_pas_versionner_si_brouillon() {
            TypeMetier brouillon = TypeMetier.creer(TENANT_A, "PHARMA", "Pharmacie", null);

            assertThatThrownBy(brouillon::verifierPeutVersionner)
                    .isInstanceOf(ProblemException.class);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // VersionType
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("VersionType — RG-03 immuabilité")
    class VersionTypeTests {

        @Test
        @DisplayName("doit créer une version non immuable avec numéro >= 1")
        void creer_version_ok() {
            UUID typeId = UUID.randomUUID();
            VersionType v = VersionType.creer(typeId, TENANT_A, 1);

            assertThat(v.numero()).isEqualTo(1);
            assertThat(v.immuable()).isFalse();
            assertThat(v.publieeLe()).isNull();
        }

        @Test
        @DisplayName("doit rendre la version immuable après publication — RG-03")
        void publier_rend_immuable() {
            VersionType v = VersionType.creer(UUID.randomUUID(), TENANT_A, 1);
            VersionType publiee = v.publier(java.time.Instant.now());

            assertThat(publiee.immuable()).isTrue();
            assertThat(publiee.publieeLe()).isNotNull();
            // L'original reste inchangé
            assertThat(v.immuable()).isFalse();
        }

        @Test
        @DisplayName("ne doit pas republier une version déjà immuable — RG-03")
        void republier_version_immuable_leve_conflit() {
            VersionType publiee = VersionType.creer(UUID.randomUUID(), TENANT_A, 1)
                    .publier(java.time.Instant.now());

            assertThatThrownBy(() -> publiee.publier(java.time.Instant.now()))
                    .isInstanceOf(ProblemException.class)
                    .hasMessageContaining("RG-03");
        }

        @Test
        @DisplayName("libelle doit indiquer l'état de la version")
        void libelle_correct() {
            VersionType brouillon = VersionType.creer(UUID.randomUUID(), TENANT_A, 2);
            VersionType publiee   = brouillon.publier(java.time.Instant.now());

            assertThat(brouillon.libelle()).isEqualTo("v2 [brouillon]");
            assertThat(publiee.libelle()).isEqualTo("v2 [publiée]");
        }
    }
}
