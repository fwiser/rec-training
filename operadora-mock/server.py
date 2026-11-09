"""Mock da operadora de telefonia (parceiro externo) usado pela RecargaLab."""
import datetime
import json
import os
import random
import re
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

PORT = int(os.environ.get("PORT", "9000"))


class Handler(BaseHTTPRequestHandler):
    server_version = "operadora-mock/1.0"

    def _json(self, code, obj):
        body = json.dumps(obj, ensure_ascii=False).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        if self.path in ("/", "/health"):
            return self._json(200, {"status": "UP", "servico": "operadora-mock"})
        m = re.fullmatch(r"/comprovantes/(RC-\d{1,6})\.json", self.path)
        if m:
            return self._json(200, {
                "comprovante": m.group(1),
                "operadora": "Vivo",
                "produto": "Vivo R$ 30",
                "telefone": "+55 11 98888-7777",
                "valor": 30.00,
                "status": "PAGO",
                "emitidoEm": datetime.datetime.now(datetime.timezone.utc).isoformat(timespec="seconds"),
            })
        return self._json(404, {"erro": "não encontrado"})

    def do_POST(self):
        length = int(self.headers.get("Content-Length") or 0)
        raw = self.rfile.read(length) if length else b""
        if self.path == "/recargas":
            try:
                pedido = json.loads(raw or b"{}")
            except ValueError:
                return self._json(400, {"erro": "JSON inválido"})
            return self._json(200, {
                "protocolo": "OP-%06d" % random.randint(0, 999999),
                "status": "OK",
                "telefone": pedido.get("telefone"),
            })
        return self._json(404, {"erro": "não encontrado"})

    def log_message(self, fmt, *args):
        print("[operadora-mock] " + (fmt % args), flush=True)


if __name__ == "__main__":
    print(f"operadora-mock ouvindo em 0.0.0.0:{PORT}", flush=True)
    ThreadingHTTPServer(("0.0.0.0", PORT), Handler).serve_forever()
