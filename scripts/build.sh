#!/usr/bin/env bash
# Build helper for this Codespace (Linux, JDK 21, NO swap, ~7.8 GB RAM).
#
# Why this exists:
#  - gradle.properties must point org.gradle.java.home at a Linux JDK 21.
#  - The NeoForm decompile step ships "--include-classpath <path to a file>"
#    which makes Vineflower try to decompile the whole library set and OOM.
#    The fix is patching the cached NeoForm config to "--library=<file>".
#    NFRT re-extracts that config from the neoform zip, so the zip patch is
#    enough; this script re-applies it if the cache was wiped.
#  - Running with --no-daemon keeps RAM free for the 3 GB JVM tools.
#
# Usage:
#   scripts/build.sh              -> ./gradlew compileJava
#   scripts/build.sh build        -> ./gradlew build
#   scripts/build.sh <any tasks>  -> ./gradlew <tasks>

set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JAVA_HOME_LINUX="/usr/local/sdkman/candidates/java/21.0.10-ms"

export JAVA_HOME="${JAVA_HOME_LINUX}"
export PATH="${JAVA_HOME}/bin:${PATH}"

# --- 1) Ensure the cached NeoForm config stays pristine ----------------------
# (Earlier workarounds on the 7.8 GB box patched the decompile args; with the
# upgraded 15 GB environment that is no longer needed. Restore the pristine
# zip from the .bak if a patched one is found.)
python3 - "$HOME" << 'PYEOF'
import zipfile, json, os, glob, sys, shutil

home = sys.argv[1]
neof = os.path.join(home, ".gradle", "caches", "modules-2", "files-2.1", "net.neoforged", "neoform")
zips = glob.glob(os.path.join(neof, "*", "*", "neoform-*.zip"))
zips = [z for z in zips if not z.endswith(".bak")]
for zp in zips:
    bak = zp + ".bak"
    try:
        with zipfile.ZipFile(zp) as z:
            cfg = json.loads(z.read("config.json"))
        args = cfg["functions"]["decompile"]["args"]
        patched = "--library={libraries}" in args or ("--library" in args and "{libraries}" not in args)
        if patched:
            if os.path.exists(bak):
                shutil.copy(bak, zp)
                print(f"[build.sh] restored pristine NeoForm config from {os.path.basename(bak)}")
            else:
                print(f"[build.sh] WARN: {os.path.basename(zp)} looks patched but no .bak found")
    except Exception as e:
        print(f"[build.sh] WARN: could not check {zp}: {e}")
PYEOF

# --- 2) Run gradle ----------------------------------------------------------
TASKS=("$@")
if [ ${#TASKS[@]} -eq 0 ]; then
  TASKS=(compileJava)
fi

cd "${REPO_DIR}"
exec ./gradlew "${TASKS[@]}" --console=plain --no-daemon
