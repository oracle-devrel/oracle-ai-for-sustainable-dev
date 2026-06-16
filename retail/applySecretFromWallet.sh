#!/bin/bash

read -p "K8s secret name (e.g., grabdish-wallet): " SECRET_NAME
read -p "Namespace (e.g., msdataworkshop): " NAMESPACE
read -p "Path to wallet directory (contains cwallet.sso, tnsnames.ora, etc.): " WALLET_DIR

# Strip any surrounding quotes
WALLET_DIR="${WALLET_DIR%\"}"
WALLET_DIR="${WALLET_DIR#\"}"
WALLET_DIR="${WALLET_DIR%\'}"
WALLET_DIR="${WALLET_DIR#\'}"

# Resolve to absolute path
if command -v realpath >/dev/null 2>&1; then
  WALLET_DIR="$(realpath "$WALLET_DIR")"
else
  OLDPWD="$(pwd)"
  cd "$WALLET_DIR" || { echo "❌ Cannot access wallet dir: $WALLET_DIR"; exit 1; }
  WALLET_DIR="$(pwd)"
  cd "$OLDPWD" || exit 1
fi

FILES=(README cwallet.sso ewallet.p12 keystore.jks ojdbc.properties sqlnet.ora tnsnames.ora truststore.jks)
for f in "${FILES[@]}"; do
  if [[ ! -f "$WALLET_DIR/$f" ]]; then
    echo "❌ Missing required file: $WALLET_DIR/$f"
    exit 1
  fi
done

b64_no_wrap () { base64 < "$1" | tr -d '\n'; }

README_B64=$(b64_no_wrap "$WALLET_DIR/README")
CWL_B64=$(b64_no_wrap "$WALLET_DIR/cwallet.sso")
EWL_B64=$(b64_no_wrap "$WALLET_DIR/ewallet.p12")
KEY_B64=$(b64_no_wrap "$WALLET_DIR/keystore.jks")
OJD_B64=$(b64_no_wrap "$WALLET_DIR/ojdbc.properties")
SQLNET_B64=$(b64_no_wrap "$WALLET_DIR/sqlnet.ora")
TNS_B64=$(b64_no_wrap "$WALLET_DIR/tnsnames.ora")
TRUST_B64=$(b64_no_wrap "$WALLET_DIR/truststore.jks")

kubectl apply -f - <<EOF
apiVersion: v1
kind: Secret
metadata:
  name: ${SECRET_NAME}
  namespace: ${NAMESPACE}
type: Opaque
data:
  README: ${README_B64}
  cwallet.sso: ${CWL_B64}
  ewallet.p12: ${EWL_B64}
  keystore.jks: ${KEY_B64}
  ojdbc.properties: ${OJD_B64}
  sqlnet.ora: ${SQLNET_B64}
  tnsnames.ora: ${TNS_B64}
  truststore.jks: ${TRUST_B64}
EOF