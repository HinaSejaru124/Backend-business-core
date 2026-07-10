import { redirect } from "next/navigation";

/** L'audit fait partie de l'espace connecté : redirection vers la console. */
export default function AuditRedirect() {
  redirect("/console/audit");
}
