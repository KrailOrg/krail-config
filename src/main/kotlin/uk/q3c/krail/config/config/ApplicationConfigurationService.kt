package uk.q3c.krail.config.config

import com.google.inject.Inject
import com.google.inject.Singleton
import org.apache.commons.configuration2.Configuration
import org.apache.commons.configuration2.JSONConfiguration
import org.apache.commons.configuration2.YAMLConfiguration
import org.apache.commons.configuration2.builder.fluent.Configurations
import org.apache.commons.io.FilenameUtils
import uk.q3c.krail.config.ApplicationConfiguration
import uk.q3c.krail.config.ApplicationConfigurationException
import uk.q3c.krail.config.ApplicationConfigurationService
import uk.q3c.krail.config.ConfigFile
import uk.q3c.krail.config.ConfigFileType
import uk.q3c.krail.config.ConfigFileType.*
import uk.q3c.krail.config.Index
import uk.q3c.krail.config.PathLocator
import uk.q3c.krail.config.i18n.ConfigurationDescriptionKey
import uk.q3c.krail.eventbus.MessageBus
import uk.q3c.krail.i18n.Translate
import uk.q3c.krail.service.AbstractService
import uk.q3c.util.guice.SerializationSupport
import java.io.File
import java.util.*
import javax.annotation.concurrent.ThreadSafe

/**
 * Created by David Sowerby on 02 May 2018
 */
@Singleton
@ThreadSafe
class DefaultApplicationConfigurationService @Inject constructor(

        translate: Translate,
        messageBus: MessageBus,
        serializationSupport: SerializationSupport,
        private val pathLocator: PathLocator,
        private val sources: Map<Index, ConfigFile>,
        private val applicationConfiguration: ApplicationConfiguration)

    : AbstractService(translate, messageBus, serializationSupport), ApplicationConfigurationService {

    private val lock: Array<Any> = arrayOf()

    init {
        descriptionKey = ConfigurationDescriptionKey.Application_Configuration_Service
    }

    /**
     * Sources with lower index override properties of the same FQN in higher index sources, see https://commons.apache.org/proper/commons-configuration/userguide/howto_hierarchical.html#Accessing_structured_properties
     */
    override fun load(): SortedMap<Index, Configuration> {
        synchronized(lock) {
            start()  // reentrant call - this will just ensure that service state is correct
            val keyOrder: List<Index> = sources.keys.sortedDescending()

            //use an ordered map to collate configs instead of adding them to combined config, then return the map
            val configSet: SortedMap<Index, Configuration> = sortedMapOf()
            for (key in keyOrder) {
                val source = sources[key]
                if (source != null) {  // not sure that it could be, we want a non-null param
                    addConfigFromFile(source, key, configSet)
                }
            }

            return configSet
        }
    }

    private fun addConfigFromFile(configFile: ConfigFile, index: Index, combinedConfiguration: SortedMap<Index, Configuration>) {
        val fileType = actualFileType(configFile)
        val configs = Configurations()
        val file = File(pathLocator.configurationDirectory(), configFile.filename)
        try {
            val config = when (fileType) {  // AUTO has been replaced
                INI -> configs.ini(file)
                YAML -> configs.fileBased(YAMLConfiguration::class.java, file)
                XML -> configs.xml(file)
                JSON -> configs.fileBased(JSONConfiguration::class.java, file)
                else -> {
                    throw ApplicationConfigurationException("Invalid file type $fileType")
                }
            }
            combinedConfiguration[index] = config

        } catch (e: Exception) {
            if (!configFile.isOptional) {
                throw ApplicationConfigurationException("Unable to load application configuration file ${file.absolutePath}", e)
            }
            // otherwise it is an invalid file but optional, so we don't care
        }
    }

    private fun actualFileType(configFile: ConfigFile): ConfigFileType {
        return if (configFile.fileType == AUTO) {
            val ext = FilenameUtils.getExtension(configFile.filename).toLowerCase()
            when (ext) {
                "ini" -> INI
                "xml" -> XML
                "yml" -> YAML
                "yaml" -> YAML
                "json" -> JSON
                else -> {
                    throw  UnsupportedOperationException("File ${configFile.filename} is not a supported file type")
                }
            }
        } else {
            configFile.fileType
        }
    }


    /**
     * nothing specific to do, [load] is called when needed
     */
    override fun doStart() {
    }

    override fun doStop() {
        synchronized(lock) {
            applicationConfiguration.clear()
        }
    }


}