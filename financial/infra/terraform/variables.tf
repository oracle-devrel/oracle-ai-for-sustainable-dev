variable "auth" {
  description = "OCI Terraform provider auth mode. APIKey is the default for local development."
  type        = string
  default     = "APIKey"
}

variable "config_file_profile" {
  description = "OCI CLI config profile to use from ~/.oci/config."
  type        = string
  default     = "DEFAULT"
}

variable "tenancy_ocid" {
  description = "OCI tenancy OCID."
  type        = string
  default     = null
}

variable "user_ocid" {
  description = "OCI user OCID for API-key authentication."
  type        = string
  default     = null
}

variable "fingerprint" {
  description = "OCI API key fingerprint."
  type        = string
  default     = null
}

variable "private_key_path" {
  description = "Path to the OCI API private key."
  type        = string
  default     = null
}

variable "region" {
  description = "OCI region, for example us-ashburn-1."
  type        = string
}

variable "home_region" {
  description = "OCI home region. Defaults to region when unset."
  type        = string
  default     = null
}

variable "compartment_ocid" {
  description = "Compartment OCID for OKE, OCIR repositories, and Autonomous Database."
  type        = string
}

variable "create_cluster" {
  description = "Create a new OKE cluster. Set false when using an existing cluster."
  type        = bool
  default     = true
}

variable "oke_cluster_name" {
  description = "OKE cluster name."
  type        = string
  default     = "financial-demo"
}

variable "kubernetes_version" {
  description = "OKE Kubernetes version, for example v1.34.2."
  type        = string
  default     = "v1.36.0"
}

variable "worker_pool_size" {
  description = "Number of managed worker nodes in the default pool."
  type        = number
  default     = 2
}

variable "worker_shape" {
  description = "Default worker node shape used by the OKE module."
  type        = map(any)
  default = {
    shape            = "VM.Standard.E4.Flex"
    ocpus            = 2
    memory           = 16
    boot_volume_size = 50

    # Balanced boot volume performance.
    boot_volume_vpus_per_gb = 10
  }
}

variable "ssh_public_key_path" {
  description = "Path to the SSH public key for OKE workers/operator/bastion."
  type        = string
}

variable "create_bastion" {
  description = "Create a bastion host through the OKE module."
  type        = bool
  default     = false
}

variable "create_operator" {
  description = "Create an operator host with kubectl and helm through the OKE module."
  type        = bool
  default     = false
}

variable "control_plane_is_public" {
  description = "Expose the OKE control plane endpoint publicly."
  type        = bool
  default     = true
}

variable "assign_public_ip_to_control_plane" {
  description = "Assign a public IP to the OKE control plane endpoint when control_plane_is_public is true."
  type        = bool
  default     = true
}

variable "control_plane_allowed_cidrs" {
  description = "CIDR blocks allowed to connect to the public Kubernetes API endpoint on port 6443."
  type        = list(string)
  default     = []
}

variable "cni_type" {
  description = "OKE pod networking type. Use npn for OCI VCN-native pod networking or flannel for overlay networking."
  type        = string
  default     = "npn"

  validation {
    condition     = contains(["npn", "flannel"], var.cni_type)
    error_message = "cni_type must be npn or flannel."
  }
}

variable "vcn_cidrs" {
  description = "VCN CIDR blocks for a newly created OKE VCN."
  type        = list(string)
  default     = ["10.0.0.0/16"]
}

variable "pods_cidr" {
  description = "Pod CIDR for Flannel clusters. Ignored by the OKE module when cni_type is npn."
  type        = string
  default     = "10.244.0.0/16"
}

variable "services_cidr" {
  description = "Kubernetes service CIDR."
  type        = string
  default     = "10.96.0.0/16"
}

variable "output_detail" {
  description = "Ask the OKE module to output detailed values, including kubeconfig."
  type        = bool
  default     = true
}

variable "enable_native_ingress_controller" {
  description = "Enable the OKE OCI Native Ingress Controller add-on for Kubernetes Ingress resources."
  type        = bool
  default     = true
}

variable "enable_cert_manager" {
  description = "Enable the OKE CertManager managed add-on. Keep this true when using cert-manager for TLS even if native OCI ingress is disabled."
  type        = bool
  default     = true
}

variable "create_native_ingress_controller_policy" {
  description = "Create IAM policy statements for the OCI Native Ingress Controller workload identity."
  type        = bool
  default     = true
}

variable "create_database" {
  description = "Create a new Autonomous AI Database. Set false to use an existing database."
  type        = bool
  default     = false
}

variable "existing_autonomous_database_ocid" {
  description = "Existing Autonomous Database OCID when create_database is false."
  type        = string
  default     = null
}

variable "autonomous_database_name" {
  description = "Autonomous Database db_name. Must be unique in the tenancy."
  type        = string
  default     = "financialdb"
}

variable "autonomous_database_display_name" {
  description = "Autonomous Database display name."
  type        = string
  default     = "financialdb"
}

variable "autonomous_database_admin_password" {
  description = "ADMIN password for a newly created Autonomous AI Database."
  type        = string
  default     = null
  sensitive   = true
}

variable "autonomous_database_workload" {
  description = "Autonomous workload type: OLTP, DW, AJD, APEX, or LH."
  type        = string
  default     = "OLTP"
}

variable "autonomous_database_version" {
  description = "Autonomous AI Database version."
  type        = string
  default     = "26ai"
}

variable "autonomous_database_compute_model" {
  description = "Autonomous compute model. ECPU is the recommended model for new databases."
  type        = string
  default     = "ECPU"
}

variable "autonomous_database_compute_count" {
  description = "Autonomous compute count."
  type        = number
  default     = 2
}

variable "autonomous_database_storage_tbs" {
  description = "Autonomous storage in TB."
  type        = number
  default     = 1
}

variable "autonomous_database_mtls_required" {
  description = "Require mTLS wallet connections."
  type        = bool
  default     = true
}

variable "autonomous_database_auto_scaling" {
  description = "Enable Autonomous Database compute auto scaling."
  type        = bool
  default     = true
}

variable "freeform_tags" {
  description = "Free-form tags applied to directly created OCI resources."
  type        = map(string)
  default = {
    app = "financial-demo"
  }
}

variable "oke_freeform_tags" {
  description = "Per-resource free-form tags expected by the OKE module."
  type        = any
  default = {
    bastion           = { app = "financial-demo" }
    cluster           = { app = "financial-demo" }
    iam               = { app = "financial-demo" }
    network           = { app = "financial-demo" }
    operator          = { app = "financial-demo" }
    persistent_volume = { app = "financial-demo" }
    service_lb        = { app = "financial-demo" }
    workers           = { app = "financial-demo" }
  }
}
