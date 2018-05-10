package uk.q3c.krail.config

import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Module
import io.mockk.mockk
import io.mockk.verify
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContainAll
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldThrow
import org.apache.commons.lang3.SerializationUtils
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import uk.q3c.krail.config.config.DefaultApplicationConfiguration
import uk.q3c.krail.config.config.DefaultApplicationConfigurationService
import uk.q3c.krail.eventbus.MessageBus
import uk.q3c.krail.i18n.Translate
import uk.q3c.krail.i18n.test.MockTranslate
import uk.q3c.util.guice.DefaultSerializationSupport
import uk.q3c.util.guice.InjectorLocator
import uk.q3c.util.guice.SerializationSupport
import java.io.File

/**
 * Created by David Sowerby on 03 May 2018
 */
object ApplicationConfigurationServiceTest : Spek({
    given("A DefaultApplicationConfiguration object with missing optional file") {
        lateinit var config: ApplicationConfiguration
        lateinit var injectorLocator: TestInjectorLocator
        val injector = Guice.createInjector(moduleSet(1))

        beforeEachTest {
            config = injector.getInstance(ApplicationConfiguration::class.java)
            injectorLocator = injector.getInstance(TestInjectorLocator::class.java)
            injectorLocator.put(injector)
        }

        on("loading set of files with overlapping properties, and retrieving without a default value") {
            val petsValue = config.getPropertyValue(List::class.java, "pets")

            val testValueAsString = config.getPropertyValue(String::class.java, "test")
            val dbUserValue = config.getPropertyValue(String::class.java, "dbUser")
            val item3Value = config.getPropertyValue(String::class.java, "item3")
            val item3AsInteger = config.getPropertyValue(Integer::class.java, "item3")
            val testValueAsBoolean = config.getPropertyValue(Boolean::class.java, "test")
            val doubleValueA: Double = config.getPropertyValue(Double::class.java, "double")
            val floatValue: Float = config.getPropertyValue(Float::class.java, "floater")


            it("we get the expected end values for each property") {
                testValueAsBoolean.shouldBeTrue()
                testValueAsString.shouldBeEqualTo("true")
                dbUserValue.shouldBeEqualTo("python") // overridden
                item3Value.shouldBeEqualTo("3")
                item3AsInteger.shouldBe(3)
                petsValue.shouldContainAll(listOf("Cat", "Dog", "Goldfish"))
                doubleValueA.shouldEqual(0.2345)
                floatValue.shouldEqual(1.37f)
            }
        }

        on("loading set of files with overlapping properties, and retrieving with a default value") {
            val petsValue: List<String> = config.getPropertyValue("pets", listOf())

            val testValueAsString = config.getPropertyValue("test", "false")
            val dbUserValue = config.getPropertyValue("dbUser", "rubbish")
            val item3Value = config.getPropertyValue("item3", "more rubbish")
            val item3AsInteger = config.getPropertyValue("item3", 12)
            val testValueAsBoolean = config.getPropertyValue("test", false)
            val doubleValueA: Double = config.getPropertyValue("double", Double.MAX_VALUE)
            val floatValue: Float = config.getPropertyValue("floater", 2.07f)


            it("we get the expected end values for each property") {
                testValueAsBoolean.shouldBeTrue()
                testValueAsString.shouldBeEqualTo("true")
                dbUserValue.shouldBeEqualTo("python") // overridden
                item3Value.shouldBeEqualTo("3")
                item3AsInteger.shouldBe(3)

                petsValue.shouldContainAll(listOf("Cat", "Dog", "Goldfish"))
                doubleValueA.shouldEqual(0.2345)
                floatValue.shouldEqual(1.37f)
            }
        }

        on("requesting a property of unknown name, without a default value") {
            val result = { config.getPropertyValue(String::class.java, "rubbish") }
            it("throws an exception") {
                result.shouldThrow(ConfigurationPropertyNotFoundException::class)
            }
        }

        on("requesting a property of unknown name, with a default value") {
            val result = config.getPropertyValue("rubbish", "defaultValue")
            it("returns the default") {
                result.shouldBeEqualTo("defaultValue")
            }
        }

        on("clearing the configuration") {
            config.clear()
            val result = config.combinedConfiguration.getString("test", "defaultValue")
            it("shows as not loaded") {
                config.loaded.shouldBeFalse()
            }
            it("does not contain properties") {
                config.combinedConfiguration.configurations.shouldBeEmpty()
            }
        }

        on("clearing the configuration then accessing via getPropertyValue()") {
            config.clear()
            val testValueAsString = config.getPropertyValue(String::class.java, "test")

            it("has reloaded from source") {
                testValueAsString.shouldBeEqualTo("true")
                config.combinedConfiguration.configurations.size.shouldBe(3)
            }
        }

        on("serialising and deserialising") {
            val output = SerializationUtils.serialize(config)
            val newConfig: DefaultApplicationConfiguration = SerializationUtils.deserialize(output)

            it("has reloaded correctly") {
                val testValueAsString = newConfig.getPropertyValue(String::class.java, "test")
                testValueAsString.shouldBeEqualTo("true")
                newConfig.combinedConfiguration.configurations.size.shouldBe(3)
            }
        }


    }
})

fun moduleSet(set: Int): List<Module> {
    return when (set) {
        1 -> listOf(ConfigTestModule(), TestFileModule())
        2 -> listOf(ConfigTestModule(), TestFileModule2())
        else -> {
            throw UnsupportedOperationException("Invalid test configuration")
        }
    }
}


class TestPathLocator : PathLocator {
    override fun configurationDirectory(): File {
        val currentDir = File(".").canonicalFile
        val configDir = File(currentDir, "src/test/kotlin/uk/q3c/krail/config")
        return configDir
    }

    override fun applicationDirectory(): File {
        val currentDir = File(".").canonicalFile
        return File(currentDir, "src/test/kotlin")
    }

}

private lateinit var injector: Injector

class TestInjectorLocator : InjectorLocator {

    override fun get(): Injector {
        return injector
    }

    override fun put(nInjector: Injector) {
        injector = nInjector
    }
}

class ConfigTestModule : AbstractModule() {

    val mockMessageBus: MessageBus = mockk(relaxed = true)

    override fun configure() {
        bind(PathLocator::class.java).to(TestPathLocator::class.java)
        bind(ApplicationConfiguration::class.java).to(DefaultApplicationConfiguration::class.java)
        bind(ApplicationConfigurationService::class.java).to(DefaultApplicationConfigurationService::class.java)
        bind(Translate::class.java).toInstance(MockTranslate())
        bind(SerializationSupport::class.java).to(DefaultSerializationSupport::class.java)
        bind(InjectorLocator::class.java).to(TestInjectorLocator::class.java)
        bind(MessageBus::class.java).toInstance(mockMessageBus)
    }

}

open class TestFileModule : ConfigurationFileModule() {
    override fun define() {
        addConfig("krail.ini", 100)
        addConfig("test.krail.ini", 90)
        addConfig("test.yml", 80)
    }
}

class TestFileModule2 : TestFileModule() {
    override fun define() {
        addConfig("missing.xml", 34, false)
    }
}


object ApplicationConfigurationServiceTest2 : Spek({
    given("A DefaultApplicationConfiguration object with missing, non-optional file") {
        lateinit var config: ApplicationConfiguration
        lateinit var injectorLocator: TestInjectorLocator
        val injector = Guice.createInjector(moduleSet(2))

        beforeEachTest {
            config = injector.getInstance(ApplicationConfiguration::class.java)
            injectorLocator = injector.getInstance(TestInjectorLocator::class.java)
            injectorLocator.put(injector)
        }

        on("try to get a property") {
            val result = { val testValueAsString = config.getPropertyValue(String::class.java, "test") }

            it("should throw exception") {
                result.shouldThrow(ApplicationConfigurationException::class)
            }
        }
    }
})

object ApplicationConfigurationServiceTest3 : Spek({
    given("A default Service") {
        lateinit var service: ApplicationConfigurationService
        val translate: Translate = mockk(relaxed = true)
        val messageBus: MessageBus = mockk(relaxed = true)
        val applicationConfiguration: ApplicationConfiguration = mockk(relaxed = true)
        val serialisationSupport: SerializationSupport = mockk()
        beforeEachTest {
            service = DefaultApplicationConfigurationService(translate = translate, messageBus = messageBus, pathLocator = TestPathLocator(), sources = mapOf(), applicationConfiguration = applicationConfiguration, serializationSupport = serialisationSupport)
        }

        on("starting then stopping the service") {
            service.start()
            service.stop()

            it("clears the configuration") {
                verify {
                    applicationConfiguration.clear()
                }
            }
        }

    }
})




