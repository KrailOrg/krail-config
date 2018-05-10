# Release notes for 0.12.0.0

A complete re-write using v2 of [Apache Commons Configuration](https://commons.apache.org/proper/commons-configuration/) in order to allow ``DefaultApplicationConfiguration`` to be ``Serializable``.

Uses a load on demand model (if the getPropertyValue()) methods are called, otherwise ``checkLoaded()`` must be invoked by the caller to ensure properties have been loaded from source.

Uses Apache [CombinedConfiguration] to merge potentially multiple sources



