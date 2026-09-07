set -e
cd /opt/liqi-saas-v2/app/frontend
echo "=== 修改 VITE_BASE_PATH ==="
sed -i 's|VITE_BASE_PATH=/|VITE_BASE_PATH=/admin/|' .env.prod
grep VITE_BASE_PATH .env.prod
echo "=== 构建前端 (build:prod) ==="
pnpm build:prod
echo "=== 发布到 nginx ==="
rm -rf /var/www/liqi-admin
cp -r dist /var/www/liqi-admin
echo "=== reload nginx ==="
nginx -s reload
echo "=== 验证 ==="
curl -s -o /dev/null -w "index.html: %{http_code}\n" http://127.0.0.1/admin/index.html
echo "DONE"
