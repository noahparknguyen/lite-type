#!/usr/bin/env bash
#
# Builds lite-type's native packages for the system this runs on, from the jars the build made:
# on Windows an installer (.exe) and a portable .zip, on Linux a .deb and a portable .tar.gz.
# Each carries a Java runtime of its own, JavaFX included, so nothing else needs installing.
# jpackage cannot build for another system, so CI runs this once on each (packages.yml), and a
# release takes its files from there (release.yml).
#
# From the repository root, after `./mvnw package`:
#
#     bash app/packaging/package.sh
#
# The packages land in app/target/packages, named after the version, as in
# lite-type-1.0.0-windows-x64.exe.
#
# Each system's package resources are in its own folder, linux/ or windows/: the icon, named
# after the app as jpackage expects, and on Linux the control file and the install and removal
# scripts.

set -euo pipefail

packaging=app/packaging
target=app/target

jars=("$target"/lite-type-app-*.jar)
cores=(core/target/lite-type-core-*.jar)
if [ ${#jars[@]} -ne 1 ] || [ ! -f "${jars[0]}" ] || [ ! -f "${cores[0]}" ]; then
    echo "Expected the app's and the core's jars: run ./mvnw package first." >&2
    exit 1
fi
jar=${jars[0]}
version=${jar##*/lite-type-app-}
version=${version%.jar}

# JavaFX comes from Gluon's jmods, the form jlink takes, so its native libraries sit inside the
# runtime instead of being unpacked into the user's home folder on first launch. Gluon doesn't
# sign them, so each download is pinned by its SHA-256, taken once every class and native
# library in it had matched OpenJFX's signed jars on Maven Central (2026-10-08). A new JavaFX in
# the POM stops this script until its jmods are checked the same way and pinned here.
javafx=27
case "$(uname -s)" in
    Linux)
        system=linux icon=lite-type.png
        jmods_zip=openjfx-${javafx}_linux-x64_bin-jmods.zip
        jmods_sha256=ea169b02460a38a2b44acf595e28c7abd3d1ab23075e4b03f324ebaf33920c5b
        ;;
    MINGW* | MSYS* | CYGWIN*)
        system=windows icon=lite-type.ico
        jmods_zip=openjfx-${javafx}_windows-x64_bin-jmods.zip
        jmods_sha256=0ad5830f31608363e1e239b84bb1ce875f5edf834f0ceda67feead9cfbaf2b41
        ;;
    *) echo "No packages are built for $(uname -s)." >&2; exit 1 ;;
esac
if [ "$(uname -m)" != x86_64 ]; then
    echo "Only x64 packages are built, since only x64 jmods are pinned." >&2
    exit 1
fi
pom_javafx=$(sed -n '/<artifactId>javafx-controls<\/artifactId>/{n;s/.*<version>\(.*\)<\/version>.*/\1/p;}' pom.xml)
if [ "$pom_javafx" != "$javafx" ]; then
    echo "The POM uses JavaFX $pom_javafx, but the jmods pinned here are $javafx's." >&2
    exit 1
fi

resources=$packaging/$system
name=lite-type-$version-$system-x64
input=$target/package-input
work=$target/package-work
out=$target/packages
rm -rf "$input" "$work" "$out"
mkdir -p "$input" "$work" "$out"
cp "$jar" "${cores[0]}" "$input"

# Every package carries the licences beside the jars: the MIT licence, and the font's, whose
# condition 2 asks for it. The jars' folder is the one place every package type keeps, since a
# .deb leaves out anything else at the image's top. Java's and JavaFX's own are in the runtime's
# legal folder.
cp LICENSE "$input"
cp -r licenses "$input"

curl --fail --silent --show-error --location --output "$work/$jmods_zip" \
    "https://download2.gluonhq.com/openjfx/$javafx/$jmods_zip"
echo "$jmods_sha256  $work/$jmods_zip" | sha256sum --check --quiet -
(cd "$work" && jar --extract --file "$jmods_zip")
jmods=$work/javafx-jmods-$javafx

# Said the same way by every package.
about=(
    --name lite-type
    --app-version "$version"
    --vendor "Noah Park-Nguyen"
    --copyright "Copyright (c) 2026 Noah Park-Nguyen"
    --description "A typing test, with real code to practise on"
)

# The app image: the launcher, the two jars, and a Java runtime cut down to the modules the app
# uses. jdeps finds java.base, java.prefs for the saved settings, and JavaFX, whose graphics
# module brings java.desktop and java.xml with it. The app's jars stay on the class path, so the
# main class is named. --enable-native-access lets JavaFX load its native code without Java
# warning about it. -XX:-UsePerfData stops Java leaving an hsperfdata folder in the system's
# temporary folder, which only monitoring tools read.
jpackage --type app-image "${about[@]}" \
    --icon "$resources/$icon" \
    --input "$input" \
    --main-jar "${jar##*/}" \
    --main-class dev.noahpn.litetype.app.LiteTypeApp \
    --module-path "$jmods" \
    --add-modules java.base,java.prefs,javafx.controls \
    --java-options -XX:-UsePerfData \
    --java-options --enable-native-access=javafx.graphics \
    --dest "$work"

# The portable archive and the installer are both made from that one image.
if [ $system = windows ]; then
    (cd "$work" && jar --create --no-manifest --file "../packages/$name.zip" lite-type)

    # Installs for the user alone, so it needs no administrator, always into
    # %LOCALAPPDATA%\lite-type. There is deliberately no folder chooser: jpackage's uninstaller
    # deletes the install folder and everything in it, and a chosen folder that already held
    # the user's files (it only warns, and lets them go ahead) would lose them all. The upgrade
    # code stays the same in every version, so a newer installer replaces an older install
    # instead of adding a second one beside it. The installer file's own icon comes from
    # --icon alone (jpackage's WinExeBundler), not the resource folder.
    jpackage --type exe "${about[@]}" \
        --app-image "$work/lite-type" \
        --icon "$resources/$icon" \
        --resource-dir "$resources" \
        --license-file LICENSE \
        --about-url https://github.com/noahparknguyen/lite-type \
        --win-per-user-install \
        --win-menu \
        --win-menu-group lite-type \
        --win-shortcut \
        --win-shortcut-prompt \
        --win-upgrade-uuid 33313cdb-1b83-4937-af33-41866f6cef25 \
        --dest "$work"
    mv "$work/lite-type-$version.exe" "$out/$name.exe"
else
    tar -czf "$out/$name.tar.gz" -C "$work" lite-type

    # The resource folder gives the package its icon, since a .deb built from an app image
    # ignores --icon, and its install and removal scripts: jpackage's own, except that a system
    # with no desktop, and so no menu to add lite-type to, does not fail them. Its control file
    # is jpackage's too, with the dependencies written out. jpackage lists every library the
    # runtime's native code loads, and every library those load in turn, by the names the
    # building system uses: on Ubuntu 24.04, names that Ubuntu 22.04 and Debian 12 don't have,
    # and Ubuntu's own for some that Debian names differently. Written out, the list holds the
    # package of each library a native file in the runtime loads itself, as Debian's own
    # dpkg-shlibdeps works it out, and apt brings the rest. A library renamed for 64-bit time
    # is listed by both names, the new one first.
    jpackage --type deb "${about[@]}" \
        --app-image "$work/lite-type" \
        --resource-dir "$resources" \
        --license-file LICENSE \
        --about-url https://github.com/noahparknguyen/lite-type \
        --linux-package-name lite-type \
        --linux-deb-maintainer noahparknguyen@gmail.com \
        --linux-shortcut \
        --linux-menu-group Education \
        --linux-app-category education \
        --dest "$work"
    mv "$work"/lite-type_"$version"_*.deb "$out/$name.deb"
fi

ls -l "$out"
