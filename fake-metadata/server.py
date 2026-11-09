"""Simulação do serviço de metadata de instância de nuvem (somente dados fictícios)."""
import datetime
import json
import os
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

BIND = os.environ.get("BIND_ADDR", "169.254.169.254")
PORT = int(os.environ.get("PORT", "80"))

ROLE = "app"
TEXT = {
    "/latest/meta-data/": "ami-id\nhostname\niam/\ninstance-id\nlocal-ipv4\n",
    "/latest/meta-data/ami-id": "ami-0lab0000000000000",
    "/latest/meta-data/hostname": "ip-10-0-1-23.lab.internal",
    "/latest/meta-data/instance-id": "i-0recargalab000001",
    "/latest/meta-data/local-ipv4": "10.0.1.23",
    "/latest/meta-data/iam/": "security-credentials/\n",
    "/latest/meta-data/iam/security-credentials/": ROLE,
}


class Handler(BaseHTTPRequestHandler):
    server_version = "fake-metadata/1.0"

    def _send(self, code, body, ctype):
        data = body.encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def do_GET(self):
        path = self.path.split("?", 1)[0]
        if path in TEXT:
            return self._send(200, TEXT[path], "text/plain")
        if path == f"/latest/meta-data/iam/security-credentials/{ROLE}":
            exp = datetime.datetime.now(datetime.timezone.utc) + datetime.timedelta(hours=6)
            return self._send(200, json.dumps({
                "Code": "Success",
                "Type": "AWS-HMAC",
                "AccessKeyId": "LAB-FAKE-KEY",
                "SecretAccessKey": "lab-fake-secret",
                "Token": "lab-fake-session-token",
                "Expiration": exp.isoformat(timespec="seconds"),
            }, indent=2), "application/json")
        return self._send(404, "Not Found", "text/plain")

    def log_message(self, fmt, *args):
        print("[fake-metadata] " + (fmt % args), flush=True)


if __name__ == "__main__":
    print(f"fake-metadata ouvindo em {BIND}:{PORT}", flush=True)
    ThreadingHTTPServer((BIND, PORT), Handler).serve_forever()
