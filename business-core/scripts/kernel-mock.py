#!/usr/bin/env python3
"""Mock kernel pour tests locaux.

Couvre le strict nécessaire pour exécuter les flux du Business Core hors kernel réel :
- POST /oauth2/token            -> jeton bidon (cache JWT)
- POST /api/client-applications -> data.clientSecret (provisioning dev)
- GET  /api/inventory/...       -> balance (VERIFIER_STOCK)
- POST /api/sales/orders ...    -> id (ENREGISTRER_VENTE)
- GET  /api/cashier/bills/...   -> id/amount/currency (facture)
- POST /api/cashier/payments    -> id (ENCAISSER)
Toute autre route renvoie un objet générique contenant tous les champs utiles.
"""
from http.server import HTTPServer, BaseHTTPRequestHandler
import json
import uuid


def generic_body():
    return json.dumps({
        "id": str(uuid.uuid4()),
        "balance": 1000,
        "amount": 100.00,
        "currency": "XAF",
        "data": {"clientId": "kc_mock", "clientSecret": "sec_mock"},
    }).encode()


class Handler(BaseHTTPRequestHandler):
    def log_message(self, fmt, *args):
        print(f"[kernel-mock] {self.command} {self.path}")

    def _drain(self):
        length = int(self.headers.get("Content-Length", 0) or 0)
        if length:
            self.rfile.read(length)

    def _send(self, code, body=None):
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.end_headers()
        if body:
            self.wfile.write(body)

    def do_POST(self):
        self._drain()
        if "/oauth2/token" in self.path:
            self._send(200, json.dumps(
                {"access_token": "mock-jwt", "token_type": "Bearer", "expires_in": 3600}
            ).encode())
        else:
            self._send(201, generic_body())

    def do_GET(self):
        # Le core cashier renvoie une LISTE de transactions ; les autres GET un objet.
        if "/cashier/transactions" in self.path:
            items = json.dumps([
                {"id": str(uuid.uuid4()), "amount": 100.00, "currency": "XAF",
                 "status": "COMPLETED", "date": "2026-06-17T21:00:00Z"},
                {"id": str(uuid.uuid4()), "amount": 250.50, "currency": "XAF",
                 "status": "COMPLETED", "date": "2026-06-17T21:05:00Z"},
            ]).encode()
            self._send(200, items)
        else:
            self._send(200, generic_body())


if __name__ == "__main__":
    port = 8089
    print(f"Kernel mock listening on http://localhost:{port}")
    HTTPServer(("0.0.0.0", port), Handler).serve_forever()
