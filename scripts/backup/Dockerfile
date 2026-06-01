# PostgreSQL 백업 사이드카: pg_dump/pg_restore(postgres 이미지) + mc(MinIO Client)
FROM postgres:16

# mc 설치 — 호스트 아키텍처(amd64/arm64) 자동 감지
RUN set -eux; \
    apt-get update; \
    apt-get install -y --no-install-recommends curl ca-certificates; \
    arch="$(dpkg --print-architecture)"; \
    curl -fsSL "https://dl.min.io/client/mc/release/linux-${arch}/mc" -o /usr/local/bin/mc; \
    chmod +x /usr/local/bin/mc; \
    apt-get purge -y curl; apt-get autoremove -y; \
    rm -rf /var/lib/apt/lists/*

COPY db-backup.sh db-restore.sh entrypoint.sh /usr/local/bin/
RUN chmod +x /usr/local/bin/db-backup.sh /usr/local/bin/db-restore.sh /usr/local/bin/entrypoint.sh

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
