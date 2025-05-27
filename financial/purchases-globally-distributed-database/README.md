
To run without k8s run this...

export DB_URL=jdbc:oracle:thin:@financialdb_high?TNS_ADMIN=C:\Users\opc\Downloads\Wallet_financialdb
$env:DB_URL = "jdbc:oracle:thin:@financialdb_high?TNS_ADMIN=C:\Users\opc\Downloads\Wallet_financialdb"
mvn package; java -jar target/

To run within k8s... run ./all.sh

export DOCKER_REGISTRY (eg `export DOCKER_REGISTRY=eu-frankfurt-1.ocir.io/oradbclouducm/financial` )

