# Infrastructure Provisioning Guide

This document covers the **Infrastructure as Code (IaC)** layer of Folio.io — everything that exists *before* the first `docker compose` command runs. There are two distinct phases:

1. **Phase 1 — Terraform**: Provisions the cloud compute and network resources on Oracle Cloud Infrastructure (OCI).
2. **Phase 2 — Ansible**: Configures the freshly-provisioned VM into a production-ready Docker host and performs the initial application deployment.

> **Audience**: This guide targets the operator performing an initial cluster build or a full disaster-recovery re-provisioning from scratch.

---

## 1. Cloud Provider & Region

The platform runs on **Oracle Cloud Infrastructure (OCI) Always Free tier**.

| Attribute | Value |
|---|---|
| Provider | Oracle Cloud Infrastructure (OCI) |
| Region | `af-casablanca-1` (Africa — Casablanca) |
| Availability Domain | `MooX:AF-CASABLANCA-1-AD-1` |
| Instance Shape | `VM.Standard.A1.Flex` (ARM64 / Ampere A1) |
| vCPUs | 2 |
| Memory | 12 GB |
| OS | Canonical Ubuntu 24.04 (ARM64) |

> **Why ARM64?** The `VM.Standard.A1.Flex` shape is the only shape eligible for OCI's Always Free tier with meaningful compute. All Docker images are therefore built for `linux/arm64` only (see `deployment.md`).

---

## 2. Phase 1: Terraform — Cloud Resource Provisioning

### Location
```
terraform-folio/
├── main.tf                  # All resource definitions
├── terraform.tfvars         # Variable values (non-secret; committed)
└── .terraform.lock.hcl      # Provider lock file
```

> **State**: `terraform.tfstate` and `terraform.tfstate.backup` are present in the directory. In a team context, state should be moved to a remote backend (OCI Object Storage or Terraform Cloud). Currently this is a single-operator project so local state is acceptable.

### Prerequisites

1. An OCI account with an API signing key generated at `~/.oci/oci_api_key.pem`.
2. Terraform `>= 1.5.0` installed locally.
3. The OCI Terraform provider `oracle/oci ~> 5.0` (locked in `.terraform.lock.hcl`).

```bash
cd terraform-folio
terraform init
terraform plan
terraform apply
```

### Resources Provisioned

#### Virtual Cloud Network (VCN)
- **Name**: `portfolio`
- **CIDR**: `10.0.0.0/16`
- **DNS label**: `portfolio`

#### Internet Gateway
- Attached to the VCN; enables outbound and inbound internet routing.

#### Route Table
- Single default route: `0.0.0.0/0` → Internet Gateway.

#### Security List (OCI-level Firewall)
The OCI Security List acts as the **outer firewall** (VCN-level). It is distinct from UFW, which operates at the OS level (see Ansible section).

| Direction | Protocol | Port / Type | Source | Purpose |
|---|---|---|---|---|
| Egress | All | All | `0.0.0.0/0` | Allow all outbound traffic |
| Ingress | ICMP Type 3 | — | `10.0.0.0/16` | Internal VCN ICMP (fragmentation) |
| Ingress | ICMP Type 3 Code 4 | — | `0.0.0.0/0` | Path MTU Discovery |
| Ingress | TCP | `22` | `0.0.0.0/0` | SSH access |
| Ingress | TCP | `80` | `0.0.0.0/0` | HTTP (Caddy redirect to HTTPS) |
| Ingress | TCP | `443` | `0.0.0.0/0` | HTTPS (Caddy TLS termination) |

> **Two-layer firewall**: OCI Security List (VCN) + UFW (OS). Both must allow a port for traffic to reach a service. The Security List is the first gate; UFW is the second.

#### Subnet
- **Name**: `portfolio-subnet`
- **CIDR**: `10.0.0.0/24`
- **Public subnet** (`prohibit_public_ip_on_vnic = false`)

#### Compute Instance
- **Name**: `portfolio-vm`
- **Shape**: `VM.Standard.A1.Flex` (2 OCPU, 12 GB RAM)
- **Image**: Ubuntu 24.04 LTS (ARM64), sourced dynamically by `data.oci_core_images.ubuntu_arm`
- **SSH Key**: Ed25519 key injected via instance metadata at provisioning time
- **Public IP**: Assigned automatically on VNIC creation

### Variable Reference

Variables are defined in `terraform.tfvars`. **Do not commit secrets** here — the current `tfvars` contains only non-secret identifiers (OCIDs, fingerprint, region).

| Variable | Description |
|---|---|
| `tenancy_ocid` | OCI tenancy OCID |
| `user_ocid` | OCI user OCID |
| `fingerprint` | API key fingerprint |
| `private_key_path` | Local path to OCI API signing key (default: `~/.oci/oci_api_key.pem`) |
| `region` | OCI region (`af-casablanca-1`) |
| `compartment_id` | OCI compartment OCID (currently same as tenancy for root compartment) |
| `availability_domain` | AD name (`MooX:AF-CASABLANCA-1-AD-1`) |

---

## 3. Phase 2: Ansible — Server Configuration & Initial Deployment

### Location
```
ansible/
├── ansible.cfg                   # Ansible configuration
├── inventory/
│   └── hosts.ini                 # VPS IP + SSH settings
├── group_vars/
│   └── all/
│       ├── vars.yml              # Plaintext variables (committed)
│       └── vault.yml             # Ansible Vault-encrypted secrets (committed as ciphertext)
├── templates/
│   └── env.j2                    # Jinja2 template for the VPS .env file
└── playbook.yml                  # Main provisioning playbook
```

### Inventory

The playbook targets the `folio_vm` host group defined in `ansible/inventory/hosts.ini`:

```ini
[folio_vm]
portfolio-vm ansible_host=<VPS_PUBLIC_IP> ansible_user=ubuntu ansible_ssh_private_key_file=~/.ssh/id_ed25519
```

The initial SSH user is `ubuntu` (the default for Ubuntu OCI images). After the playbook runs, the `deploy` user is created and used for all subsequent CI/CD operations.

### Running the Playbook

```bash
cd ansible

# First run: decrypt vault inline
ansible-playbook playbook.yml --ask-vault-pass

# Or use a vault password file
ansible-playbook playbook.yml --vault-password-file ~/.vault_pass
```

### Playbook Task Breakdown

The playbook runs as `root` (`become: true`) and executes the following tasks in order:

#### 1. Base System Update
- Runs `apt update` and `apt upgrade -y` with retry logic (10 retries, 15s delay) to handle transient OCI mirror failures.
- Installs base prerequisites: `curl`, `ca-certificates`, `gnupg`, `ufw`, `git`, `acl`.

#### 2. Docker Engine Installation
- Adds the official Docker APT repository using the **deb822 format** (the current recommended method, replacing the legacy `apt-key` approach).
- Automatically selects `arm64` or `amd64` architecture based on `ansible_facts['architecture']` — `aarch64` maps to `arm64`.
- Installs: `docker-ce`, `docker-ce-cli`, `containerd.io`, `docker-compose-plugin`.
- Enables and starts the Docker systemd service.

#### 3. Deploy User Setup
- Creates a `deploy` group and `deploy` user.
- Adds `deploy` to both `docker` and `sudo` groups (grants Docker socket access and passwordless sudo).
- Installs the operator's SSH public key (`~/.ssh/id_ed25519.pub`) for the `deploy` user.
- Writes a sudoers drop-in to `/etc/sudoers.d/deploy` for passwordless `sudo`.

#### 4. UFW Firewall Configuration
The playbook configures UFW with a **deny-all-inbound default policy**, then explicitly allows:

| Rule | Port/Service | Protocol |
|---|---|---|
| Allow SSH | OpenSSH | TCP |
| Allow HTTP | `80` | TCP |
| Allow HTTPS | `443` | TCP |

UFW is then enabled (`ufw enable`). All other inbound connections are dropped.

> **Combined with OCI Security List**: For a port to be reachable from the internet, it must be open in **both** the OCI Security List AND UFW. These are independently enforced layers.

#### 5. Application Directory & Repository
- Creates `/opt/folio` owned by `deploy:deploy` with `chmod 755`.
- Clones `https://github.com/IlyasUyidir/portfolio` (branch: `main`) into `/opt/folio`.

#### 6. Secrets — Ansible Vault
The `.env` file on the VPS is **never stored in plaintext in the repository**. Instead:
1. All secrets are stored in `ansible/group_vars/all/vault.yml`, encrypted with `ansible-vault`.
2. On deploy, Ansible decrypts the vault and renders `ansible/templates/env.j2` into `/opt/folio/.env` with `chmod 600`.

The Jinja2 template (`env.j2`) maps vault variables to their `.env` counterparts:

```jinja2
SPRING_DATASOURCE_URL={{ vault_db_url }}
SPRING_DATASOURCE_USERNAME={{ vault_db_username }}
SPRING_DATASOURCE_PASSWORD={{ vault_db_password }}
JWT_SECRET={{ vault_jwt_secret }}
GHCR_REPO={{ ghcr_repo | default('ilyasuyidir/portfolio') }}
APP_CORS_ALLOWED_ORIGINS={{ vault_app_cors_allowed_origins }}
GRAFANA_ADMIN_PASSWORD={{ vault_grafana_password }}
```

**Vault variable mapping** (vault key → .env key):

| Vault Variable | `.env` Variable |
|---|---|
| `vault_db_url` | `SPRING_DATASOURCE_URL` |
| `vault_db_username` | `SPRING_DATASOURCE_USERNAME` |
| `vault_db_password` | `SPRING_DATASOURCE_PASSWORD` |
| `vault_jwt_secret` | `JWT_SECRET` |
| `vault_app_cors_allowed_origins` | `APP_CORS_ALLOWED_ORIGINS` |
| `vault_grafana_password` | `GRAFANA_ADMIN_PASSWORD` |

> **Rotating a secret**: Decrypt the vault (`ansible-vault edit group_vars/all/vault.yml`), update the value, and re-run the playbook. The `.env` on the VPS is re-rendered and `chmod 600` is enforced automatically.

#### 7. Initial Application Deployment
- Uses the `community.docker.docker_compose_v2` Ansible module as the `deploy` user.
- Runs `docker compose -f docker-compose.prod.yml -f docker-compose.observability.yml up -d --remove-orphans` from `/opt/folio`.
- This constitutes the **first-ever** application deployment. All subsequent deploys are handled by the CI/CD pipeline (see `deployment.md`).

---

## 4. Relationship Between Terraform, Ansible, and CI/CD

```
terraform apply
        |
        v
VM provisioned (IP known, SSH accessible as ubuntu)
        |
        v
ansible-playbook playbook.yml
  - Docker + Compose installed
  - deploy user created (docker + sudo groups)
  - UFW enabled (allow 22, 80, 443 / deny all)
  - Repo cloned to /opt/folio
  - .env rendered from vault
  - First docker compose up (initial deploy)
        |
        v
git push main  ->  GitHub Actions CI/CD
  - Test -> Build ARM64 image -> SSH as deploy user
  - cd /opt/folio && git pull && docker compose up
  - All subsequent deploys follow this path
```

Terraform and Ansible are **one-time operations** for initial provisioning. The CI/CD pipeline handles all subsequent deployments automatically.

---

## 5. Disaster Recovery Runbook

To fully rebuild the stack from zero after a VM loss:

```bash
# Step 1: Re-provision the VM
cd terraform-folio
terraform apply
# Note the new public IP from the output

# Step 2: Update inventory with the new VPS IP
#   Edit ansible/inventory/hosts.ini:
#   ansible_host=<NEW_PUBLIC_IP>

# Step 3: Re-configure the VM
cd ../ansible
ansible-playbook playbook.yml --vault-password-file ~/.vault_pass

# Step 4: Trigger a fresh deployment
#   Option A: Push a commit to main
#   Option B: Manually re-run the 'deploy-production' workflow in GitHub Actions
```

> **Data Loss Warning**: PostgreSQL data lives in the `postgres_data` Docker volume on the VPS disk. It will be **permanently lost** if the VM is destroyed without a prior backup. Always take a `pg_dump` backup before terminating or destroying the instance (see `database.md`).

---

## 6. Security Checklist

- [ ] **Terraform state**: `terraform.tfstate` is `.gitignore`-excluded — verify this before any commit.
- [ ] **OCI API Key**: `~/.oci/oci_api_key.pem` must have `chmod 600` and must never be committed.
- [ ] **Ansible Vault password**: Store at `~/.vault_pass` with `chmod 600`. Never commit.
- [ ] **VPS .env**: Verify `chmod 600 /opt/folio/.env` after every Ansible run (`ls -la /opt/folio/.env`).
- [ ] **UFW active**: After provisioning verify: `ssh deploy@<IP> 'sudo ufw status verbose'`.
- [ ] **SSH Key rotation**: If `id_ed25519` is compromised, update `deploy_ssh_public_key_path` in `vars.yml` and re-run the playbook to replace the authorized key.
