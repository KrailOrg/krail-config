package uk.q3c.krail.config.service

import spock.lang.Specification
import uk.q3c.krail.config.ApplicationConfiguration
import uk.q3c.krail.config.IniFileConfig
import uk.q3c.krail.config.PathLocator
import uk.q3c.krail.config.config.DefaultApplicationConfiguration
import uk.q3c.krail.config.i18n.ConfigurationDescriptionKey
import uk.q3c.krail.config.i18n.ConfigurationLabelKey
import uk.q3c.krail.eventbus.MessageBus
import uk.q3c.krail.eventbus.MessageBusProvider
import uk.q3c.krail.i18n.Translate
import uk.q3c.krail.i18n.test.MockTranslate
import uk.q3c.krail.service.RelatedServiceExecutor
import uk.q3c.krail.service.Service
import uk.q3c.krail.service.ServiceStatus

import static com.google.common.base.Preconditions.*
import static org.assertj.core.api.Assertions.*
import static org.mockito.Mockito.*
/**
 * Created by David Sowerby on 22 Aug 2017
 */
class DefaultApplicationConfigurationServiceTest extends Specification {

    DefaultApplicationConfigurationService service
    Translate translate
    ApplicationConfiguration configuration
    Map<Integer, IniFileConfig> iniFiles
    MessageBusProvider globalBusProvider
    MessageBus globalBus
    PathLocator pathLocator
    RelatedServiceExecutor relatedServiceExecutor


    def setup() {
        relatedServiceExecutor = mock(RelatedServiceExecutor)
        pathLocator = mock(PathLocator)
        globalBus = mock(MessageBus)
        globalBusProvider = mock(MessageBusProvider)
        when(globalBusProvider.get()).thenReturn(globalBus)
        iniFiles = new HashMap<>()
        translate = new MockTranslate()
        configuration = new DefaultApplicationConfiguration()
        service = new DefaultApplicationConfigurationService(translate, configuration, iniFiles, globalBusProvider, pathLocator, relatedServiceExecutor)
        when(relatedServiceExecutor.execute(RelatedServiceExecutor.Action.START, Service.Cause.STARTED)).thenReturn(true)
        when(pathLocator.configurationDirectory()).thenReturn(new File("/home/david/git/krail-config/src/test/groovy/uk/q3c/krail/config/service"))
    }

    def cleanup() {
        File ff = new File(pathLocator.configurationDirectory(), "test.krail.ini")
        ff.setReadable(true)
    }

    def "load one file"() {
        given:
        addConfig("krail.ini", 0, false)

        when:
        service.start()

        then: "one configuration is the in memory one added automatically"
        assertThat(service.isStarted()).isTrue()
        assertThat(configuration.getNumberOfConfigurations()).isEqualTo(2)
        assertThat(configuration.getBoolean("test")).isTrue()
        assertThat(configuration.getString("dbUser")).isEqualTo("monty")

    }

    def "load two files"() {
        given:
        addConfig("krail.ini", 100, false)
        addConfig("test.krail.ini", 99, false)

        when:
        service.start()

        then: "one configuration is the in memory one added automatically"
        assertThat(configuration.getNumberOfConfigurations()).isEqualTo(3)
        assertThat(configuration.getBoolean("test")).isTrue()
        assertThat(configuration.getString("dbUser")).isEqualTo("python")
    }

    def "load file error"() {
        given:
        File ff = new File(pathLocator.configurationDirectory(), "test.krail.ini")
        ff.setReadable(false)
        addConfig("krail.ini", 100, false)
        addConfig("test.krail.ini", 99, false)

        when:
        service.start()

        then:
        ("service shows as failed because required config missing")
        assertThat(service.getState()).isEqualTo(Service.State.FAILED)

    }

    def "stop start"() {
        given:
        addConfig("krail.ini", 0, false)
        addConfig("test.krail.ini", 1, false)
        configuration.addProperty("in memory", "memory")

        when:
        service.start()

        then: "one configuration is the in memory one added automatically"
        assertThat(configuration.getNumberOfConfigurations()).isEqualTo(3)
        assertThat(service.getState()).isEqualTo(Service.State.RUNNING)
        assertThat(configuration.getString("in memory")).isEqualTo("memory")

        when:
        service.stop()

        then:
        assertThat(configuration.getNumberOfConfigurations()).isEqualTo(1)
        assertThat(service.getState()).isEqualTo(Service.State.STOPPED)
        assertThat(configuration.getString("in memory")).isNull()
    }


    def "file is missing but not optional"() {

        given:
        addConfig("rubbish.ini", 0, false)

        when:
        ServiceStatus status = service.start()

        then: "service fails to start, mandatory config file is missing"
        assertThat(status.getState()).isEqualTo(Service.State.FAILED)
        assertThat(status.cause).isEqualTo(Service.Cause.FAILED_TO_START)
        assertThat(configuration.getNumberOfConfigurations()).isEqualTo(1)
    }

    def "I18N keys"() {
        expect:
        assertThat(service.getNameKey()).isEqualTo(ConfigurationLabelKey.Application_Configuration_Service)
        assertThat(service.getName()).isEqualTo("Application Configuration Service")
        assertThat(service.getDescriptionKey()).isEqualTo(ConfigurationDescriptionKey.Application_Configuration_Service)
        assertThat(service.getDescription()).isEqualTo("Application Configuration Service")
    }

    protected void addConfig(String filename, int index, boolean optional) {
        checkNotNull(filename)
        IniFileConfig ifc = new IniFileConfig(filename, optional)
        iniFiles.put(index, ifc)
    }
}
