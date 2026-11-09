FROM python:3.12-alpine
RUN apk add --no-cache iproute2
COPY server.py entrypoint.sh /
CMD ["sh", "/entrypoint.sh"]
