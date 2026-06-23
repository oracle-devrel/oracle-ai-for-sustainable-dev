

HOSTNAME="${PUBLIC_HOSTNAME:-oracledev.ai}"
P12_NAME="${P12_NAME:-oracledev-ai.p12}"

openssl pkcs12 -export -in "${HOSTNAME}.cert" -inkey "${HOSTNAME}.key" -out "${P12_NAME}" -name "${HOSTNAME}"
 cp oracledatabasefinancialorg.p12 src/main/resources/
