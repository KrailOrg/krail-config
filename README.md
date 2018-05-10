# krail-config

The default implementation for the [API](https://github.com/KrailOrg/krail-config-api)

## Serialisation

The Apache Commons implementation does not support serialisation, but this is overcome by using a "Load on Demand" approach

## Load on Demand

The `getPropertyValue` methods automatically invoke `checkLoaded()` to ensure that the configuration is loaded before looking for a property value.

If you access properties via the `combinedConfiguration` object, you will need to invoke `checkLoaded()` first ensure values have been loaded.