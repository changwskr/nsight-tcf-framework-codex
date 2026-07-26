# Optional per-module secrets for deploy.sh
#
# Naming: <module>-secret.yaml
# Examples:
#   eb-secret.yaml
#   sv-secret.yaml
#   ui-secret.yaml
#
# deploy.sh applies the file when present:
#   kubectl apply -f configs/<module>-secret.yaml -n <namespace>
#
# Do not commit real credentials. Prefer Sealed Secrets / External Secrets.
