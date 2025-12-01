# Keycloak integration

This document describes how Cilantro authenticates and authorizes users with Keycloak and how to expose the staff metadata required by Rubikon providers.

## Goals

- Use Keycloak as the single IdP for all Cilantro users (browser and API clients).
- Map Cilantro and Rubikon roles/flags to Keycloak roles.
- Embed staff attributes and feature flags in the Keycloak access token so they are available to Cilantro and downstream Rubikon providers.

## Realm and client setup

1. Create (or reuse) a realm dedicated to Cilantro, for example `cilantro`.
2. Create a **confidential** client named `cilantro` with the following settings:
   - Access type: `confidential`.
   - Standard flow enabled (Authorization Code).
   - Valid redirect URIs: `https://<cilantro-host>/login/oauth2/code/*`.
   - Web origins: `https://<cilantro-host>` (adjust for non-production URLs).
   - Add **Client Scopes** `profile`, `email`, `roles`, and a custom scope `staff` (see below).
3. Create a **Service Account** for the `cilantro` client if machine-to-machine access is needed and assign the required roles.

## Role model

Define consistent roles in Keycloak so Cilantro (and Rubikon) can authorize users:

- Realm roles capture global privileges (e.g., `administrator`, `security_auditor`).
- Client roles inside the `cilantro` client capture app-specific permissions (e.g., `teller`, `branch_manager`, `rubikon_operator`).
- Use **Role Composites** to bundle permissions (e.g., `branch_manager` includes `teller`).

The `KeycloakUserAuthoritiesConverter` maps both realm roles and `cilantro` client roles to Spring Security authorities with the `ROLE_` prefix.

## Staff attributes required in the token

Expose the following staff attributes in an **access token** so Cilantro and Rubikon can read them without additional lookups:

| Claim path              | Example value         | Purpose                                  |
|-------------------------|-----------------------|------------------------------------------|
| `preferred_username`    | `jane.doe`            | Login identifier displayed in the UI     |
| `email`                 | `jane.doe@bank.tz`    | Contact info & audit trails              |
| `feature_flags`         | `["bulk_upload"]`    | Toggle features per user/segment         |
| `staff.staffId`         | `EMP12345`            | Internal staff number                    |
| `staff.branchCode`      | `BR001`               | Branch scoping for dashboards/limits     |
| `staff.department`      | `Retail`              | Department level scoping                 |
| `staff.employmentType`  | `Permanent`           | HR/entitlement context                   |

### Protocol mappers

Create **Protocol Mappers** on the `cilantro` client (or on the `staff` client scope):

- **User Attribute Mapper** for each staff attribute above, mapped to a claim under `staff.*` (JSON type `String`).
- **User Realm Role Mapper** to add realm roles to the `realm_access.roles` claim.
- **User Client Role Mapper** to add `cilantro` client roles to the `resource_access.cilantro.roles` claim.
- **Script Mapper** (optional) for `feature_flags` if it is built from user groups or attributes. Emit it as a JSON array claim.

## Token consumption in Cilantro

- When `keycloak.enabled=true`, Cilantro validates JWTs using the Keycloak issuer (`application-keycloak.properties`). Override the defaults via the following environment variables so the app talks to the correct realm/client (for example `http://172.21.2.32:9443/realms/internal`): `KEYCLOAK_ISSUER_URI`, `KEYCLOAK_CLIENT_ID`, and `KEYCLOAK_CLIENT_SECRET`.
- Authorities are assembled from `realm_access.roles` and `resource_access.cilantro.roles` and normalized to `ROLE_*` by `KeycloakUserAuthoritiesConverter`.
- Staff metadata can be extracted from the JWT with `KeycloakUserDetailsExtractor.fromJwt(jwt)` for downstream services or auditing.
- Sessions are stateless; APIs expect the `Authorization: Bearer <token>` header. Browser logins use OAuth2 login backed by the same client configuration.

## Rubikon readiness

- Define Rubikon-specific roles in Keycloak (e.g., `rubikon_operator`, `rubikon_admin`) so they flow into `resource_access.cilantro.roles`.
- If Rubikon needs custom flags, add them under the `feature_flags` claim or create a dedicated `rubikon` claim namespace via a protocol mapper.
- Downstream Rubikon providers can rely on the same Keycloak token: all required staff identifiers, branch/department scoping, and roles are already present.

