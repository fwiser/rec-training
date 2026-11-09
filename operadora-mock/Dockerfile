FROM python:3.12-alpine
WORKDIR /srv
COPY server.py .
USER nobody
EXPOSE 9000
CMD ["python", "-u", "server.py"]
