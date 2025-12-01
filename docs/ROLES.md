# Supported Cilantro Roles

The following roles are provisioned by default in Cilantro and should be mapped in your identity provider so they flow into the application and downstream Rubikon providers.

## Core application roles

These roles are defined in the sample Cilantro database (`roles` table) and power most UI modules and workflows:

- Super Super User
- SUPER USER
- RECON SUPERVISOR
- RECON USER
- TELLER
- IBD OPERATOR
- P&V OPERATOR
- BRANCH INITIATOR
- BRANCH APPROVER
- P&V APPROVER
- IT HELPDESK
- HQ INITIATOR
- HQ APPROVER
- TREASURY OPERATOR
- CREDIT
- AUDITOR  OFFICER
- EBANKING OFFICER
- CARD CENTER
- CUSTOMER SERVICE OFFICER

## Rubikon roles

Cilantro forwards Rubikon roles from Keycloak via the `KeycloakUserAuthoritiesConverter`, so they can be reused by Rubikon providers without additional token enrichment. Define at least the following client roles in the `cilantro` client and map them to users as needed:

- rubikon_operator
- rubikon_admin

Additional Rubikon-specific roles can be added following the same pattern.
