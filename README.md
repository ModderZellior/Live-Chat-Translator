# Universal Live Chat Translator

A client-side Minecraft chat translator for multiplayer servers. It translates incoming chat messages into your chosen language and can translate outgoing messages before sending them.

## Branches

Each supported Minecraft version is kept on its own branch:

| Loader | Minecraft version | Branch |
| --- | --- | --- |
| Fabric | 1.21.11 | `fabric/1.21.11` |
| Fabric | 26.1 | `fabric/26.1` |
| Fabric | 26.1.1 | `fabric/26.1.1` |
| Fabric | 26.1.2 | `fabric/26.1.2` |
| Fabric | 26.2 | `fabric/26.2` |
| Legacy Fabric | 1.8.9 | `legacy-fabric/1.8.9` |

## Features

- Translates incoming multiplayer chat locally on the client.
- `/tr chat <language>` sets the target language for incoming chat.
- `/tr on` and `/tr off` toggle incoming chat translation.
- `/tr msg <language> <message>` translates and sends an outgoing message.
- Language command suggestions include many Google Translate language names and codes.
- Uses Google Translate's public web endpoint, so no API key is required.

## Notes

- This is a client-side mod.
- The `1.8.9` build uses Legacy Fabric, not modern Fabric or Quilt.
- For `1.8.9`, install Legacy Fabric Loader and Legacy Fabric API.

## License

MIT License.
