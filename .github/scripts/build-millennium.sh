#!/usr/bin/env bash
# Builds the Millennium UI library from source with Tubify's patch and publishes it to
# mavenLocal, which Gradle resolves before Maven Central. The patch removes Millennium's
# built-in "glass" overlay (shine, dark wash and rim drawn on hover, focus and press, e.g. on
# long press), which ignores the theme and cannot be turned off from the app.
set -euo pipefail
MILLENNIUM_COMMIT="c2f18c6ad6f789028d2b947cac45d6a0658546bd" # version 0.1.1
dir="${1:-millennium-src}"
git clone --quiet https://github.com/team-spotube/millennium.git "$dir"
cd "$dir"
git checkout --quiet "$MILLENNIUM_COMMIT"

interaction=millennium/src/commonMain/kotlin/org/spotube/millennium/theme/Interaction.kt
python3 - "$interaction" <<'PY'
import re, sys
p = sys.argv[1]
s = open(p).read()
pattern = r"internal fun millenniumInteractionLevels\(state: MillenniumControlState\): Pair<Float, Float> = when \(state\) \{.*?\n\}"
new = ("@Suppress(\"UNUSED_PARAMETER\")\n"
       "internal fun millenniumInteractionLevels(state: MillenniumControlState): Pair<Float, Float> = 0f to 0f")
s2, n = re.subn(pattern, new, s, flags=re.S)
if n != 1:
    sys.exit("Tubify patch: millenniumInteractionLevels not found")
open(p, "w").write(s2)
PY

# Local publication only: no signing keys here.
sed -i.bak '/signAllPublications()/d' millennium/build.gradle.kts
chmod +x gradlew
./gradlew :millennium:publishToMavenLocal --no-configuration-cache -q
