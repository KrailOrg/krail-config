package uk.q3c.krail.config.i18n

import uk.q3c.krail.config.i18n.ConfigurationDescriptionKey.Application_Configuration_Service
import uk.q3c.krail.i18n.EnumResourceBundle

/**
 * Created by David Sowerby on 22 Aug 2017
 */
class ConfigurationDescriptions_de : EnumResourceBundle<ConfigurationDescriptionKey>() {

    override fun loadMap() {
        put(Application_Configuration_Service, "Dieser Service lädt die Anwendungs-Konfiguration aus krail.ini")
    }
}
