package uk.q3c.krail.config.i18n;

import uk.q3c.krail.i18n.EnumResourceBundle;

import static uk.q3c.krail.config.i18n.ConfigurationLabelKey.*;

/**
 * Created by David Sowerby on 22 Aug 2017
 */
public class ConfigurationLabels extends EnumResourceBundle<ConfigurationLabelKey> {


    @Override
    protected void loadMap() {
        put(Application_Configuration_Service, "Anwendung Konfigurations-Service");
    }
}
