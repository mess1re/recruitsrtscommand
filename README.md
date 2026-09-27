# Recruits RTS Command

Recruits RTS Command adds group selection and map orders to the tactical map in [Villager Recruits](https://modrinth.com/mod/villager-recruits).

## Features

- Select individual recruits, whole squads or saved control groups.
- Issue move, queued move, halt, facing and attack orders from the map.
- Set formations, spacing, combat behaviour, shield state and fire zones.
- Display squad positions, routes, health and compatible map objects.
- Keep the tactical map available when the server does not have RTS Command; command controls remain disabled.

## Dependency

- [Villager Recruits 1.15.2+](https://modrinth.com/mod/villager-recruits)

Minecraft 1.20.1 and Forge 47 are supported.

## Compatibility

[Siegeworks](https://github.com/mess1re/siegeworks) supports issuing orders to siege equipment from the tactical map.

## API

Release builds are available through JitPack:

```gradle
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation fg.deobf('com.github.mess1re:recruitsrtscommand:0.1.0-beta.1')
}
```

## License

Recruits RTS Command is licensed under [LGPL-3.0-only](LICENSE).
