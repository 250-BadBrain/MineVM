# Development Validation

## Automated Commands

Run from the repository root with Java 17:

```powershell
.\gradlew.bat build --console=plain
.\gradlew.bat runGameTestServer --console=plain
.\gradlew.bat runQemuMediaIntegrationSelfTest '-PqemuSystemPath=D:/qemu/qemu-system-x86_64.exe' --console=plain
```

The opt-in QEMU test requires a complete QEMU installation including sibling `qemu-img.exe` and `qemu-io.exe`. It creates and cleans a temporary directory, not the configured player storage. GameTest uses `run-gametest`, isolated from the user's `run` world. Development gameplay is launched using `./gradlew.bat runClient --console=plain`.

The build includes profile, framebuffer, protocol, bounded executor, input, VM lifecycle, compatibility evidence, codec boundary, token budget, control lease and disk recovery self-tests. Forge GameTest checks placement, physical endpoint identity/removal, NBT ownership persistence, recipe/command loading and packet roundtrips. Dedicated-server startup additionally catches accidental client-class loading; it is not an end-to-end multiplayer rendering test.

## Multiplayer Design and Limits

- Network protocol 3: require matching development JARs, not old 1.0 clients.
- QEMU, QMP and VNC stay on the server/host. Only validated screen/input packets cross the Minecraft connection; do not expose QMP/VNC ports publicly.
- Nearby viewing is server-validated within 16 blocks. Input/control is validated within 8 blocks with loaded chunks, matching computer identity and physical keyboard/mouse wiring where required.
- Ownership is persisted in computer NBT. Owners and OPs manage; owners, OPs and explicitly trusted players control. This does not implement a complete claim/protection system against breaking blocks, explosions or other mods. Use the server's normal world protection for that.
- A controller renews its exclusive lease once per second; it expires after 6 seconds. Exit, invalid distance/device state, revocation and disconnect release recorded held keys and mouse buttons. Sharing the view does not share control.
- Up to four watched VMs per player. Clients renew view subscriptions once per second; unrenewed subscriptions expire after 3 seconds. Rendering an eligible screen requests its subscription.
- Full frames are scaled with aspect ratio preserved to at most 512 x 320. Default cap is 5 FPS and 524288 payload bytes/second per player with a two-second burst. A token budget may reduce delivered FPS. Configured FPS is a maximum; server ticks quantize intervals, and unchanged frames are not resent.
- Encoding uses two bounded worker threads; decoding uses one bounded worker. Busy queues drop frames, not unbounded work. Each frame is independently decodable, so the next complete frame recovers without a delta dependency.
- RAW and DEFLATE are built in. DEFLATE compresses exact pixels after the resolution reduction; it is not MJPEG or H.264. Local singleplayer rendering retains its local VNC path rather than unnecessarily round-tripping through the multiplayer codec.

## Two-Client Manual Checklist

This checklist still requires real gameplay and is not automatically marked passed:

1. Start a dedicated server with QEMU configured. Join using two clients with the same JAR; only the server has ISO and QEMU paths.
2. Owner A places/wires/powers a computer. B can watch under `sharedViewing = true` but cannot control, rewire A's devices or maintain A's disk.
3. A grants B control access. While A controls, B cannot acquire the same VM. A exits with Ctrl+Alt; B then acquires and drives the same guest.
4. Hold Ctrl/Shift or a mouse button, then disconnect or walk out of range. Confirm the guest receives release and a new controller can take over after expiry. Revoke B while B controls and confirm control ends.
5. Remove and replace a keyboard at identical coordinates. Input remains invalid until the replacement is deliberately reconnected. Replace a computer at identical coordinates and confirm an old monitor link does not control the new identity.
6. Test separate dimensions, chunk unload, world reopen, server restart, rapid screen selection, and multiple running guests. No VM should become controllable through stale coordinates or old leases.
7. Set shared viewing off and reconnect. Untrusted non-owner subscriptions must stop; explicitly trusted controllers must still see the guest. With RAW and DEFLATE, confirm both clients receive correctly framed images, cursor alignment and changing content without corrupt texture state.
8. Try 1, 5, 12 and 20 maximum FPS and the low/default bandwidth budgets. Confirm frames reduce smoothly under budget pressure, unchanged frames are skipped and server tick latency stays acceptable.
9. Shut down a disposable guest, back up, reset and restore. Confirm data and guest profile return; running maintenance must fail. Keep retained files until manual verification is complete.

## Profiling and Future Video Work

Use `/minevm status` on the server side and `/minevmclient stats` on the client. Compare two samples over a fixed interval: frame count, average merge/decode microseconds, uploads and average upload microseconds, network payload bytes, and QEMU cumulative CPU milliseconds. Counters are process/session-local, not a persistent benchmark database or an instantaneous FPS meter. Payload counters exclude Minecraft protocol overhead.

Transport and codecs now have separate interfaces (`ScreenTransport` and `FrameCodec`). VNC is the current capture implementation. H.264 would need a separately designed asynchronous stream transport, frame timing, process lifecycle and decoder integration; replacing the codec interface alone is not sufficient. No FFmpeg executable or Java decoder dependency has been added in this version. Measure which stage dominates first and retain a known-working fallback when adding it.

New hardware recipes, first-login guidance and new workflow messages exist in English/Chinese. Artwork remains placeholder/reused artwork until the final texture pass. Some legacy runtime diagnostics are still technical English.
