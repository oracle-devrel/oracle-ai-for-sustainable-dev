module "oke" {
  count  = var.create_cluster ? 1 : 0
  source = "github.com/oracle-terraform-modules/terraform-oci-oke?ref=v5.4.3"

  providers = {
    oci      = oci
    oci.home = oci.home
  }

  tenancy_id                        = var.tenancy_ocid
  compartment_id                    = var.compartment_ocid
  region                            = var.region
  home_region                       = var.home_region
  ssh_public_key_path               = var.ssh_public_key_path
  output_detail                     = var.output_detail
  create_cluster                    = true
  cluster_name                      = var.oke_cluster_name
  cluster_type                      = "enhanced"
  kubernetes_version                = var.kubernetes_version
  control_plane_is_public           = var.control_plane_is_public
  assign_public_ip_to_control_plane = var.assign_public_ip_to_control_plane
  control_plane_allowed_cidrs       = var.control_plane_allowed_cidrs
  cni_type                          = var.cni_type
  vcn_cidrs                         = var.vcn_cidrs
  pods_cidr                         = var.pods_cidr
  services_cidr                     = var.services_cidr
  worker_pool_mode                  = "node-pool"
  worker_pool_size                  = var.worker_pool_size
  worker_shape                      = var.worker_shape
  worker_pools = {
    financial = {
      size = var.worker_pool_size
    }
  }
  create_bastion  = var.create_bastion
  create_operator = var.create_operator
  freeform_tags   = var.oke_freeform_tags
}

locals {
  native_ingress_workload_identity_condition = "request.principal.type = 'workload', request.principal.namespace = 'native-ingress-controller-system', request.principal.service_account = 'oci-native-ingress-controller', request.principal.cluster_id = '${module.oke[0].cluster_id}'"
}

resource "oci_identity_policy" "native_ingress_controller" {
  count = var.create_cluster && var.enable_native_ingress_controller && var.create_native_ingress_controller_policy ? 1 : 0

  compartment_id = var.compartment_ocid
  name           = "${var.oke_cluster_name}-native-ingress-controller"
  description    = "Allows the OCI Native Ingress Controller workload identity to manage load balancer resources for ${var.oke_cluster_name}."

  statements = [
    "Allow any-user to manage load-balancers in compartment id ${var.compartment_ocid} where all {${local.native_ingress_workload_identity_condition}}",
    "Allow any-user to use virtual-network-family in compartment id ${var.compartment_ocid} where all {${local.native_ingress_workload_identity_condition}}",
    "Allow any-user to manage cabundles in compartment id ${var.compartment_ocid} where all {${local.native_ingress_workload_identity_condition}}",
    "Allow any-user to manage cabundle-associations in compartment id ${var.compartment_ocid} where all {${local.native_ingress_workload_identity_condition}}",
    "Allow any-user to manage leaf-certificates in compartment id ${var.compartment_ocid} where all {${local.native_ingress_workload_identity_condition}}",
    "Allow any-user to read leaf-certificate-bundles in compartment id ${var.compartment_ocid} where all {${local.native_ingress_workload_identity_condition}}",
    "Allow any-user to manage leaf-certificate-versions in compartment id ${var.compartment_ocid} where all {${local.native_ingress_workload_identity_condition}}",
    "Allow any-user to manage certificate-associations in compartment id ${var.compartment_ocid} where all {${local.native_ingress_workload_identity_condition}}",
    "Allow any-user to read certificate-authorities in compartment id ${var.compartment_ocid} where all {${local.native_ingress_workload_identity_condition}}",
    "Allow any-user to manage certificate-authority-associations in compartment id ${var.compartment_ocid} where all {${local.native_ingress_workload_identity_condition}}",
    "Allow any-user to read certificate-authority-bundles in compartment id ${var.compartment_ocid} where all {${local.native_ingress_workload_identity_condition}}",
    "Allow any-user to read public-ips in compartment id ${var.compartment_ocid} where all {${local.native_ingress_workload_identity_condition}}",
    "Allow any-user to manage floating-ips in compartment id ${var.compartment_ocid} where all {${local.native_ingress_workload_identity_condition}}",
    "Allow any-user to manage waf-family in compartment id ${var.compartment_ocid} where all {${local.native_ingress_workload_identity_condition}}",
    "Allow any-user to read cluster-family in compartment id ${var.compartment_ocid} where all {${local.native_ingress_workload_identity_condition}}",
  ]
}

resource "oci_containerengine_addon" "cert_manager" {
  count = var.create_cluster && (var.enable_cert_manager || var.enable_native_ingress_controller) ? 1 : 0

  addon_name = "CertManager"
  cluster_id = module.oke[0].cluster_id

  remove_addon_resources_on_delete = true

  configurations {
    key   = "numOfReplicas"
    value = "1"
  }
}

resource "oci_containerengine_addon" "native_ingress_controller" {
  count = var.create_cluster && var.enable_native_ingress_controller ? 1 : 0

  addon_name = "NativeIngressController"
  cluster_id = module.oke[0].cluster_id

  remove_addon_resources_on_delete = true

  configurations {
    key   = "authType"
    value = "workloadIdentity"
  }

  configurations {
    key   = "loadBalancerSubnetId"
    value = module.oke[0].pub_lb_subnet_id
  }

  configurations {
    key   = "compartmentId"
    value = var.compartment_ocid
  }

  depends_on = [
    oci_containerengine_addon.cert_manager,
    oci_identity_policy.native_ingress_controller,
  ]
}

resource "oci_database_autonomous_database" "financial" {
  count = var.create_database ? 1 : 0

  compartment_id                      = var.compartment_ocid
  db_name                             = var.autonomous_database_name
  display_name                        = var.autonomous_database_display_name
  admin_password                      = var.autonomous_database_admin_password
  db_workload                         = var.autonomous_database_workload
  db_version                          = var.autonomous_database_version
  compute_model                       = var.autonomous_database_compute_model
  compute_count                       = var.autonomous_database_compute_count
  data_storage_size_in_tbs            = var.autonomous_database_storage_tbs
  is_auto_scaling_enabled             = var.autonomous_database_auto_scaling
  is_mtls_connection_required         = var.autonomous_database_mtls_required
  license_model                       = "LICENSE_INCLUDED"
  freeform_tags                       = var.freeform_tags
  is_auto_scaling_for_storage_enabled = true
}

locals {
  autonomous_database_ocid = var.create_database ? oci_database_autonomous_database.financial[0].id : var.existing_autonomous_database_ocid
}
