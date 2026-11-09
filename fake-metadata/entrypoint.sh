#!/bin/sh
# Cria o IP do serviço de metadata no loopback do namespace de rede compartilhado com a API.
ip addr add 169.254.169.254/32 dev lo 2>/dev/null || true
exec python -u /server.py
