package uk.q3c.krail.config.i18n;

import uk.q3c.krail.i18n.EnumResourceBundle;

import static uk.q3c.krail.config.i18n.ConfigurationDescriptionKey.*;

/**
 * Created by David Sowerby on 22 Aug 2017
 */
public class ConfigurationDescriptions extends EnumResourceBundle<ConfigurationDescriptionKey> {

    @Override
    protected void loadMap() {
        put(Application_Configuration_Service, "This service loads the application configuration from krail.ini");
    }
}
