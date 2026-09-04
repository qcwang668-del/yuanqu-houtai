#!/usr/bin/env bash
# GPU 服务器（192.168.8.43 · 双 RTX 5090 · 主机名 fangnangpu）直连助手
# 用法：
#   scripts/gpu_ssh.sh 'nvidia-smi'          # 执行远程命令
#   scripts/gpu_ssh.sh                       # 交互式登录
#   scripts/gpu_ssh.sh 'cd /home/fangnan/PycharmProjects/... && <构建/推理命令>'
#
# 说明：按项目铁律用 root 密码 sshpass 直连；密码只从 deploy/secrets/gpu-server.env
# 读入（该文件已被 .gitignore 忽略），绝不写进任何被 git 跟踪的文件。
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${GPU_ENV_FILE:-$ROOT/deploy/secrets/gpu-server.env}"

if [ ! -f "$ENV_FILE" ]; then
  echo "缺凭据文件：$ENV_FILE" >&2
  echo "请从示例复制并填入 GPU root 密码：" >&2
  echo "  cp deploy/secrets/gpu-server.env.example deploy/secrets/gpu-server.env" >&2
  echo "  # 密码见兄弟项目【密钥收集】password.txt 的「GPU服务器（双5090…）」一节" >&2
  exit 1
fi
set -a; . "$ENV_FILE"; set +a

: "${GPU_HOST:=192.168.8.43}"
: "${GPU_USER:=root}"
: "${GPU_PASS:?deploy/secrets/gpu-server.env 缺 GPU_PASS}"

command -v sshpass >/dev/null 2>&1 || { echo "未安装 sshpass：apt-get install -y sshpass" >&2; exit 1; }

exec sshpass -p "$GPU_PASS" ssh \
  -o StrictHostKeyChecking=no \
  -o UserKnownHostsFile=/dev/null \
  -o ConnectTimeout=15 \
  -o ServerAliveInterval=15 \
  "${GPU_USER}@${GPU_HOST}" "$@"
