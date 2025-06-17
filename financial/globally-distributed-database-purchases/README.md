
## Prereqs

Oracle Database is created and the wallet is extracted to a directory such as `C:\Users\opc\Downloads\Wallet_financialdb` in below examples

## To run without k8s (ie standalone)...

- export DB_URL=jdbc:oracle:thin:@financialdb_high?TNS_ADMIN=C:\Users\opc\Downloads\Wallet_financialdb
- (or if on powershell: $env:DB_URL = "jdbc:oracle:thin:@financialdb_high?TNS_ADMIN=C:\Users\opc\Downloads\Wallet_financialdb")
- mvn package; java -jar target/[appropriatejar]

## To run within k8s...
- create a Kubernetes secret using `createSecretFromWallet.sh` or `createSecretFromWalletMac.sh` script in the `k8s` directory
- (optionally create a secret in OCI Vault and provide the secret OCID in the deploy descriptor - if present the microservice will use this instead of k8s secret)
- export DOCKER_REGISTRY (eg `export DOCKER_REGISTRY=eu-frankfurt-1.ocir.io/oradbclouducm/financial` )
- run ./all.sh

