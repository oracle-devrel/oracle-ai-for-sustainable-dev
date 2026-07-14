

 openssl pkcs12 -export -in oracledatabase-financial.org.cert  -inkey  oracledatabase-financial.key  -out oracledatabasefinancialorg.p12 -name oracledatabasefinancialorg
 cp oracledatabasefinancialorg.p12 src/main/resources/