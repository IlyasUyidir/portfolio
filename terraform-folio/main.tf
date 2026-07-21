terraform {
  required_version = ">= 1.5.0"
  required_providers {
    oci = {
      source  = "oracle/oci"
      version = "~> 5.0"
    }
  }
}

provider "oci" {
  tenancy_ocid     = var.tenancy_ocid
  user_ocid        = var.user_ocid
  fingerprint      = var.fingerprint
  private_key_path = var.private_key_path
  region           = var.region
}

variable "tenancy_ocid" {}
variable "user_ocid" {}
variable "fingerprint" {}
variable "private_key_path" {}
variable "region" {}
variable "compartment_id" {}
variable "availability_domain" {}

resource "oci_core_vcn" "folio_vcn" {
  compartment_id = var.compartment_id
  cidr_block     = "10.0.0.0/16"
  display_name   = "portfolio"
  dns_label      = "portfolio"
}

resource "oci_core_internet_gateway" "folio_igw" {
  compartment_id = var.compartment_id
  vcn_id         = oci_core_vcn.folio_vcn.id
  display_name   = "portfolio-igw"
  enabled        = true
}

resource "oci_core_route_table" "folio_rt" {
  compartment_id = var.compartment_id
  vcn_id         = oci_core_vcn.folio_vcn.id
  display_name   = "Default Route Table for portfolio"
  route_rules {
    destination       = "0.0.0.0/0"
    destination_type  = "CIDR_BLOCK"
    network_entity_id = oci_core_internet_gateway.folio_igw.id
  }
}

resource "oci_core_security_list" "folio_sl" {
  compartment_id = var.compartment_id
  vcn_id         = oci_core_vcn.folio_vcn.id
  display_name   = "Default Security List for portfolio"

  egress_security_rules {
    destination      = "0.0.0.0/0"
    destination_type = "CIDR_BLOCK"
    protocol         = "all"
    stateless        = false
  }

  ingress_security_rules {
    protocol    = "1"
    source      = "10.0.0.0/16"
    source_type = "CIDR_BLOCK"
    stateless   = false
    icmp_options {
      code = -1
      type = 3
    }
  }
  ingress_security_rules {
    protocol    = "1"
    source      = "0.0.0.0/0"
    source_type = "CIDR_BLOCK"
    stateless   = false
    icmp_options {
      code = 4
      type = 3
    }
  }
  ingress_security_rules {
    protocol    = "6"
    source      = "0.0.0.0/0"
    source_type = "CIDR_BLOCK"
    stateless   = false
    tcp_options {
      max = 22
      min = 22
    }
  }
  ingress_security_rules {
    description = "HTTP"
    protocol    = "6"
    source      = "0.0.0.0/0"
    source_type = "CIDR_BLOCK"
    stateless   = false
    tcp_options {
      max = 80
      min = 80
    }
  }
  ingress_security_rules {
    description = "HTTPS"
    protocol    = "6"
    source      = "0.0.0.0/0"
    source_type = "CIDR_BLOCK"
    stateless   = false
    tcp_options {
      max = 443
      min = 443
    }
  }
  ingress_security_rules {
    description = "SSH"
    protocol    = "6"
    source      = "0.0.0.0/0"
    source_type = "CIDR_BLOCK"
    stateless   = false
    tcp_options {
      max = 22
      min = 22
    }
  }
}

resource "oci_core_subnet" "folio_subnet" {
  compartment_id             = var.compartment_id
  vcn_id                     = oci_core_vcn.folio_vcn.id
  cidr_block                 = "10.0.0.0/24"
  display_name               = "portfolio-subnet"
  dns_label                  = "portfoliosubnet"
  route_table_id             = oci_core_route_table.folio_rt.id
  security_list_ids          = [oci_core_security_list.folio_sl.id]
  prohibit_public_ip_on_vnic = false
}

data "oci_core_images" "ubuntu_arm" {
  compartment_id           = var.compartment_id
  operating_system         = "Canonical Ubuntu"
  operating_system_version = "24.04"
  shape                    = "VM.Standard.A1.Flex"
  sort_by                  = "TIMECREATED"
  sort_order               = "DESC"
}

resource "oci_core_instance" "folio_vm" {
  compartment_id      = var.compartment_id
  availability_domain = "MooX:AF-CASABLANCA-1-AD-1"
  display_name        = "portfolio-vm"
  shape               = "VM.Standard.A1.Flex"
  shape_config {
    ocpus         = 2
    memory_in_gbs = 12
  }
  create_vnic_details {
    subnet_id        = oci_core_subnet.folio_subnet.id
    assign_public_ip = true
  }
  source_details {
    source_type = "image"
    source_id           = "ocid1.image.oc1.af-casablanca-1.aaaaaaaay4udbkrr62bdhxmcfzsm54qg4snxynctxwjztfgowemu5kd4tu4q"
  }
}
