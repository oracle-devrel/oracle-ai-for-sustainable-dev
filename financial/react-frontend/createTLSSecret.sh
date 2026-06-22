


HOSTNAME="${PUBLIC_HOSTNAME:-oracledev.ai}"
TLS_SECRET_NAME="${TLS_SECRET_NAME:-oracledev-ai-tls}"

kubectl create secret tls "${TLS_SECRET_NAME}" --cert="${HOSTNAME}.cert" --key="${HOSTNAME}.key" -n financial
