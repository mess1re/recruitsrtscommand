# Changelog

## Unreleased

### Added

- Oriented tactical squad symbols when zoomed out, with infantry, ranged,
  cavalry and mixed-unit markings. Picking individual troops restores unit dots.

### Fixed

- Queued moves wait for 80% of the troops to reach each waypoint instead of using
  the squad center. Stalled orders retry without resetting their progress timeout
  and report a blocked route rather than disappearing silently.
- Ranged attack positions preserve the intended distance from the target.
- Pursuit checks current scouting visibility for squads and map objects even
  while the map is closed, and stops when contact is lost.
- Selected troops and squad tooltips show movement, pursuit and failure states.
- Explicit move routes take priority over combat movement, including mounted
  charges and movement into formation. Combat behavior resumes after arrival or
  cancellation without changing the squad's aggression setting.
- Camera movement no longer depends on frame rate. Leaving a world clears control
  groups and command tools; closing the map cancels unfinished gestures and menus.
- Unit dots and symbols share relation colors: own green, allied blue, neutral
  ochre and enemy red. Symbols shrink rather than grow when zooming out.
- Cluster membership is rebuilt on snapshot updates, not every rendered frame.
- Escape cancels unfinished fire-zone gestures before a later mouse release can
  submit them. Empty selections no longer leave stale type-filter hit areas.
- Server-side limits bound map-update requests, queued waypoints and fire zones;
  snapshot collections are validated before allocation.

### Compatibility

- Map snapshots now include unit headings and order states (network protocol 4). Update both the
  server and clients together; the previous command protocol is not compatible.

### Internal

- Separate squad rendering, command bar, context menus, map objects and fire-zone
  interaction from the root overlay. Server snapshots, visibility and order
  execution now have independent responsibilities.

## 0.1.0-beta.2

### Fixed

- Fire zones can be assigned to selected siege crews from the map. Drawing a zone no longer issues a move order to the selected engines.
