---
contentRetrieval: patch
---

Fixed the module being rejected at install time. Its signature had been left on the 8.2 line after the version moved to 8.3, so Jahia refused the bundle and the module manager reported that it could not find a bundle for the key.
