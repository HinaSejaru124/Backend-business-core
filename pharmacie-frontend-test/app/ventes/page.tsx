import PageHeader from "@/components/PageHeader";
import BlocageKernel from "@/components/BlocageKernel";

export default function VentesPage() {
  return (
    <div className="animate-fade-up">
      <PageHeader
        eyebrow="Historique"
        title="Ventes"
        description="Liste des ventes exécutées, avec statut de trace Business Core en direct."
      />

      <div className="mt-6">
        <BlocageKernel contexte="L'historique des ventes" />
      </div>
    </div>
  );
}
