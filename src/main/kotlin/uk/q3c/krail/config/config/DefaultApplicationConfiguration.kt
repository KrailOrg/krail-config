/*
 *
 *  * Copyright (c) 2016. David Sowerby
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 *  * the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *  * specific language governing permissions and limitations under the License.
 *
 */
package uk.q3c.krail.config.config

import com.google.inject.Inject
import com.google.inject.Provider
import com.google.inject.Singleton
import org.apache.commons.configuration2.CombinedConfiguration
import org.apache.commons.configuration2.Configuration
import uk.q3c.krail.config.ApplicationConfiguration
import uk.q3c.krail.config.ApplicationConfigurationService
import uk.q3c.krail.config.ConfigurationPropertyNotFoundException
import uk.q3c.krail.config.ConfigurationPropertyTypeNotKnown
import uk.q3c.util.guice.SerializationSupport
import java.io.IOException
import java.io.ObjectInputStream
import javax.annotation.concurrent.ThreadSafe


/**
 *
 * Uses Apache Commons Configuration to load configuration from potentially multiple sources
 *
 * As this is a Singleton, it is threadsafe.
 */
@Singleton
@ThreadSafe
class DefaultApplicationConfiguration @Inject constructor(
        @field:Transient private val configurationServiceProvider: Provider<ApplicationConfigurationService>, @field:Transient override val combinedConfiguration: CombinedConfiguration, val serializationSupport: SerializationSupport)

    : ApplicationConfiguration, Configuration by combinedConfiguration {


    private val lock: Array<Any> = arrayOf()
    override var loaded = false


    override fun <T : Any> getPropertyValue(valueClass: Class<T>, propertyName: String): T {
        synchronized(lock) {
            checkLoaded()
            val value: T? = getValue(valueClass, propertyName)
            if (value == null) {
                throw ConfigurationPropertyNotFoundException(propertyName)
            } else {
                return value
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> getValue(valueClass: Class<out T>, propertyName: String): T? {

        // Kotlin uses internal representations such as EmptyList which break the when clause
        if (List::class.java.isAssignableFrom(valueClass)) {
            return combinedConfiguration.getList(propertyName) as T
        }
        return when (valueClass) {
            String::class.java, java.lang.String::class.java -> combinedConfiguration.getString(propertyName) as T?
            Integer::class.java, java.lang.Integer::class.java -> combinedConfiguration.getInt(propertyName) as T?
            Boolean::class.java, java.lang.Boolean::class.java -> combinedConfiguration.getBoolean(propertyName) as T?
            Double::class.java, java.lang.Double::class.java -> combinedConfiguration.getDouble(propertyName) as T?
            Float::class.java, java.lang.Float::class.java -> combinedConfiguration.getDouble(propertyName) as T?

            else -> {
                throw ConfigurationPropertyTypeNotKnown(propertyName, valueClass.name)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> getValue(propertyName: String, defaultValue: T): T {
        val valueClass: Class<T> = defaultValue.javaClass
        // Kotlin uses internal representations such as EmptyList which break the when clause
        if (List::class.java.isAssignableFrom(valueClass)) {
            return combinedConfiguration.getList(propertyName) as T
        }
        return when (valueClass) {
            String::class.java, java.lang.String::class.java -> combinedConfiguration.getString(propertyName, defaultValue as String) as T
            Integer::class.java, java.lang.Integer::class.java -> combinedConfiguration.getInt(propertyName, defaultValue as Int) as T
            Boolean::class.java, java.lang.Boolean::class.java -> combinedConfiguration.getBoolean(propertyName, defaultValue as Boolean) as T
            Double::class.java, java.lang.Double::class.java -> combinedConfiguration.getDouble(propertyName, defaultValue as Double) as T
            Float::class.java, java.lang.Float::class.java -> combinedConfiguration.getFloat(propertyName, defaultValue as Float) as T

            else -> {
                throw ConfigurationPropertyTypeNotKnown(propertyName, valueClass.name)
            }
        }
    }

    override fun <T : Any> getPropertyValue(propertyName: String, defaultValue: T): T {
        synchronized(lock) {
            checkLoaded()
            try {
                return getValue(propertyName = propertyName, defaultValue = defaultValue)
            } catch (e: Exception) {
                throw ConfigurationPropertyNotFoundException(propertyName, e)
            }

        }
    }

    override fun checkLoaded() {
        if (!loaded) {
            combinedConfiguration.clear()
            val configSet = configurationServiceProvider.get().load()
            configSet.forEach({ (_, c) ->
                combinedConfiguration.addConfiguration(c)
            })

        }
        loaded = true
    }


    override fun clear() {
        synchronized(lock) {
            loaded = false
            combinedConfiguration.clear()
        }
    }

    @Throws(ClassNotFoundException::class, IOException::class)
    private fun readObject(inputStream: ObjectInputStream) {
        inputStream.defaultReadObject()
        serializationSupport.deserialize(this)
        loaded = false
    }
}
