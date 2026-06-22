

- Grafana can be reached through the financial app host, for example
  `https://oracledev.ai/financial/grafana`.
- Generally Grafana is not incorporated into the application as is done here so there are two things that have been done that are usually not necessary..
  - In order to allow Grafana in iframe the env var GF_SECURITY_ALLOW_EMBEDDING, set to true, is added to the Grafana deployment
  - In order to use the same host (and thus ingress) for mapping to grafana, which requires bridging the `grafana` and `financial` namespaces, grafana-headless-service.yaml and grafana-manual-endpoint.yaml that are used to bridge the namespaces
