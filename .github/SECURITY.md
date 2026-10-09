# Security policy

## Reporting a vulnerability

Please report security problems privately: open the repository's **Security** tab and choose
**Report a vulnerability**. Please don't open a public issue for them.

Say what you found, which version and download it affects (the Windows installer or zip, or the
Linux `.deb` or `.tar.gz`), and how to reproduce it. I'll reply as soon as I can, and credit you
in the fix's release notes if you'd like.

## Supported versions

Only the latest release is fixed. Each package carries the Java runtime that was current when
it was built, and its release notes name it.

## What lite-type does on your computer

It reads none of your files, opens no network connections, and runs no other programs. It
writes one thing: your choices and the window's size and place, so the next launch opens the
same way. On Windows they're kept in the registry, under
`HKEY_CURRENT_USER\Software\JavaSoft\Prefs\dev\noahpn`, and on Linux in
`~/.java/.userPrefs/dev/noahpn`. On Linux, your graphics driver may also keep a shader cache in
`~/.cache`, as it does for every program that draws with OpenGL. Uninstalling removes
everything the installer put in place, but not your settings.
