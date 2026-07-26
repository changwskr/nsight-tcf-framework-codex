#!/usr/bin/env bash
set -euo pipefail
apachectl -t
httpd -M | egrep 'proxy|proxy_http|proxy_balancer|lbmethod|ssl|headers|status|rewrite' || true
curl -k -I https://nh.marketing.com/portal/ || true
