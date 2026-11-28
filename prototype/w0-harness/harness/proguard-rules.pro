# Harness keep rules.
# - debug builds: no shrinking, no rules needed.
# - benchmark (H-6/F-H5): R8 on. kotlinx-serialization, Compose and androidx.webkit
#   ship consumer rules. The harness exposes no JS interfaces and no reflection
#   entry points, so nothing extra has been needed so far.
#   The session-log JSON model (@Serializable data classes) is reached only via
#   the generated serializers, which R8 keeps through its bundled rules.
