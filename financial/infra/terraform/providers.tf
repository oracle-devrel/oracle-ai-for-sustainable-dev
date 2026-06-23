provider "oci" {
  auth                = var.auth
  config_file_profile = var.config_file_profile
  tenancy_ocid        = var.tenancy_ocid
  user_ocid           = var.user_ocid
  fingerprint         = var.fingerprint
  private_key_path    = var.private_key_path
  region              = var.region
}

provider "oci" {
  alias               = "home"
  auth                = var.auth
  config_file_profile = var.config_file_profile
  tenancy_ocid        = var.tenancy_ocid
  user_ocid           = var.user_ocid
  fingerprint         = var.fingerprint
  private_key_path    = var.private_key_path
  region              = coalesce(var.home_region, var.region)
}
