output "cluster_id" {
  description = "Created OKE cluster OCID when create_cluster is true."
  value       = var.create_cluster ? module.oke[0].cluster_id : null
}

output "cluster_kubeconfig" {
  description = "Kubeconfig emitted by the OKE module when output_detail is true."
  value       = var.create_cluster ? try(module.oke[0].cluster_kubeconfig, null) : null
  sensitive   = true
}

output "cluster_endpoints" {
  description = "OKE cluster endpoints when create_cluster is true."
  value       = var.create_cluster ? try(module.oke[0].cluster_endpoints, null) : null
}

output "autonomous_database_ocid" {
  description = "Created or supplied Autonomous Database OCID."
  value       = local.autonomous_database_ocid
}

output "database_service_name" {
  description = "Default database service name expected by the Kubernetes config."
  value       = "${var.autonomous_database_name}_high"
}
