# Mixson (NeoForged Fork)

A simple lightweight library for modded Minecraft
that allows for .json resource files to be modified, created, or deleted at runtime through an event-based system.

---

## Downloading the Project

This project can be installed through jitpack. First, add the following to the *first* `repositories` section in the `build.gradle`:
```gradle
repositories {
    ...
    maven { url 'https://jitpack.io' }
    ...
}
```
After that is added,
the dependency can be added through inserting this into the `dependencies` section in the `build.gradle`:
```gradle

dependencies {
    ...
    modImplementation 'com.github.ramixin:mixson-neoforge:TAG'
    ...
}
```

The `TAG` in the above section is where the specific version of Mixson will go.
Either go to the wiki page below and copy the best version for your MC version, or
go to https://jitpack.io/#ramixin/mixson-neoforge.

## Usage

See the wiki for indepth usage instructions and examples: https://github.com/Ramixin/Mixson/wiki
## License

This project is under an MIT
