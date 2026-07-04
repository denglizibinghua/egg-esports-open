#!/usr/bin/env bash
# ============================================================
# Egg Esports MySQL 备份脚本
# 用法：crontab 每日 03:00 执行
#   0 3 * * * /opt/egg-esports/backup-mysql.sh >> /var/log/egg-backup.log 2>&1
# 依赖：mysqldump、gzip
# 保留策略：最近 7 天的备份
# ============================================================

set -euo pipefail

# ---- 配置（从环境变量读取，与 systemd .env 复用） ----
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${DB_USER:-egg}"
DB_NAME="${DB_NAME:-egg_esports}"
DB_PASSWORD="${DB_PASSWORD:?DB_PASSWORD 未设置}"

# 备份目录与保留天数
BACKUP_DIR="/opt/egg-esports/backups"
RETAIN_DAYS=7

# ---- 主流程 ----
mkdir -p "$BACKUP_DIR"

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/egg_${DB_NAME}_${TIMESTAMP}.sql.gz"

echo "[$(date '+%F %T')] 开始备份 $DB_NAME ..."

# mysqldump 导出 + gzip 压缩（--single-transaction 保证 InnoDB 一致性快照）
mysqldump \
    --host="$DB_HOST" \
    --port="$DB_PORT" \
    --user="$DB_USER" \
    --password="$DB_PASSWORD" \
    --single-transaction \
    --quick \
    --routines \
    --triggers \
    "$DB_NAME" | gzip > "$BACKUP_FILE"

BACKUP_SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
echo "[$(date '+%F %T')] 备份完成: $BACKUP_FILE ($BACKUP_SIZE)"

# 清理超过保留期的旧备份
DELETED=$(find "$BACKUP_DIR" -name "egg_${DB_NAME}_*.sql.gz" -mtime +$RETAIN_DAYS -print -delete | wc -l)
if [ "$DELETED" -gt 0 ]; then
    echo "[$(date '+%F %T')] 清理 $DELETED 个超过 ${RETAIN_DAYS} 天的旧备份"
fi
