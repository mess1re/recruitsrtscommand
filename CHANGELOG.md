# Changelog

## Unreleased

### Added

- Oriented tactical squad symbols when zoomed out, with infantry, ranged,
  cavalry and mixed-unit markings. Picking individual troops restores unit dots.

### Fixed

- Camera movement no longer depends on frame rate. Leaving a world clears control
  groups and command tools; closing the map cancels unfinished gestures and menus.
- Unit dots and symbols share relation colors: own green, allied blue, neutral
  ochre and enemy red. Symbols shrink rather than grow when zooming out.
- Cluster membership is rebuilt on snapshot updates, not every rendered frame.
- Server-side limits bound map-update requests, queued waypoints and fire zones;
  snapshot collections are validated before allocation.

### Compatibility

- Map snapshots now include unit headings (network protocol 3). Update both the
  server and clients together; the previous command protocol is not compatible.

## 0.1.0-beta.2

### Fixed

- Fire zones can be assigned to selected siege crews from the map. Drawing a zone no longer issues a move order to the selected engines.
