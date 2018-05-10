package uk.q3c.krail.config.i18n

import uk.q3c.krail.config.i18n.ConfigurationLabelKey.Application_Configuration_Service
import uk.q3c.krail.i18n.EnumResourceBundle

/**
 * Created by David Sowerby on 22 Aug 2017
 */
class ConfigurationLabels : EnumResourceBundle<ConfigurationLabelKey>() {


    override fun loadMap() {
        put(Application_Configuration_Service, "Anwendung Konfigurations-Service")
    }
}
