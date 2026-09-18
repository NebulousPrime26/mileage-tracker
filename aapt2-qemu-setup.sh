#!/bin/sh
#
# aapt2-qemu-setup  —  Run AGP's x86-64 aapt2 on aarch64 Linux via QEMU
#
# Usage:  sh aapt2-qemu-setup.sh
#
# Environment variables (all optional):
#   AAPT2_EROFS       Path to FEX rootfs erofs image or plain sysroot dir
#                     (default: /usr/share/fex-emu/RootFS/default.erofs)
#   AAPT2_SYSROOT     Mount/target directory for the x86-64 sysroot
#                     (default: /tmp/aapt2-x86root)
#   AAPT2_WRAPPER_DIR Where to install the aapt2 wrapper script
#                     (default: $HOME/.local/share/aapt2-qemu)
#
# Prerequisites:  qemu-user, erofsfuse (from erofs-utils)
#
set -e

# ---- defaults ---------------------------------------------------------------
EROFS="${AAPT2_EROFS:-/usr/share/fex-emu/RootFS/default.erofs}"
SYSROOT="${AAPT2_SYSROOT:-/tmp/aapt2-x86root}"
WRAPPER_DIR="${AAPT2_WRAPPER_DIR:-$HOME/.local/share/aapt2-qemu}"
WRAPPER="$WRAPPER_DIR/aapt2"

# ---- checks -----------------------------------------------------------------
command -v qemu-x86_64 >/dev/null || {
  echo "ERROR: qemu-user is required." >&2
  echo "  Fedora: sudo dnf install qemu-user" >&2
  echo "  Debian: sudo apt install qemu-user-static" >&2
  echo "  Arch:   sudo pacman -S qemu-user-static" >&2
  exit 1
}

# ---- sysroot (x86-64 root filesystem for QEMU) ------------------------------
if [ -f "$EROFS" ]; then
  # erofs image — FUSE mount it
  command -v erofsfuse >/dev/null || {
    echo "ERROR: erofs-utils is required to mount the FEX rootfs." >&2
    echo "  Fedora: sudo dnf install erofs-utils" >&2
    exit 1
  }
  mkdir -p "$SYSROOT"
  mountpoint -q "$SYSROOT" || erofsfuse "$EROFS" "$SYSROOT"
elif [ -d "$EROFS" ]; then
  # plain directory — use directly
  SYSROOT="$EROFS"
else
  echo "ERROR: rootfs not found at $EROFS" >&2
  echo "Provide a custom path via AAPT2_EROFS, or install fex-emu." >&2
  exit 1
fi

# Verify the sysroot has the x86-64 dynamic linker
[ -f "$SYSROOT/lib64/ld-linux-x86-64.so.2" ] || {
  echo "ERROR: $SYSROOT does not contain a usable x86-64 sysroot." >&2
  echo "Expected to find lib64/ld-linux-x86-64.so.2" >&2
  exit 1
}

# ---- locate AGP's x86-64 aapt2 ----------------------------------------------
AAPT2=$(for d in "$HOME"/.gradle/caches/*/transforms/*/transformed/aapt2-*-linux; do
  [ -f "$d/aapt2" ] && file "$d/aapt2" | grep -q 'ELF.*x86-64' && echo "$d"
done | sort -V | tail -1)/aapt2

[ -n "$AAPT2" ] && [ -f "$AAPT2" ] || {
  echo "No x86-64 aapt2 found in Gradle cache." >&2
  echo "Run './gradlew assembleDebug' once so AGP downloads it, then re-run this script." >&2
  exit 1
}

# ---- write the QEMU wrapper -------------------------------------------------
# AGP calls aapt2 during build. The wrapper:
#   - Merges stderr->stdout for "version" only (AGP reads version from stdout,
#     but aapt2 prints it to stderr).
#   - Passes everything else transparently through qemu-x86_64.
#   - Leaves daemon stderr alone (AGP treats EOF on the stderr pipe as death).
mkdir -p "$WRAPPER_DIR"
cat > "$WRAPPER" <<EOF
#!/bin/sh
[ "\$1" = "version" ] && exec /usr/bin/qemu-x86_64 -L "$SYSROOT" "$AAPT2" "\$@" 2>&1
exec /usr/bin/qemu-x86_64 -L "$SYSROOT" "$AAPT2" "\$@"
EOF
chmod +x "$WRAPPER"

# ---- register with AGP ------------------------------------------------------
mkdir -p "$HOME/.gradle"
if grep -q '^android.aapt2FromMavenOverride=' "$HOME/.gradle/gradle.properties" 2>/dev/null; then
  sed -i 's#^android.aapt2FromMavenOverride=.*#android.aapt2FromMavenOverride='"$WRAPPER"'#' \
    "$HOME/.gradle/gradle.properties"
else
  printf '\nandroid.aapt2FromMavenOverride=%s\n' "$WRAPPER" >> "$HOME/.gradle/gradle.properties"
fi

echo "OK: aapt2 wrapper installed at $WRAPPER"
echo "    sysroot:      $SYSROOT"
echo "    aapt2 binary: $AAPT2"
echo "    gradle property: android.aapt2FromMavenOverride=$WRAPPER"
