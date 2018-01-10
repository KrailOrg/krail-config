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

package uk.q3c.krail.config.bind

import com.google.common.collect.Lists
import com.google.inject.*
import spock.lang.Specification
import uk.q3c.krail.config.IniFileConfig
import uk.q3c.krail.config.PathLocator
import uk.q3c.krail.eventbus.MessageBus
import uk.q3c.krail.i18n.Translate
import uk.q3c.krail.i18n.test.MockTranslate
import uk.q3c.krail.service.RelatedServiceExecutor

import static org.mockito.Mockito.*
/**
 * Created by David Sowerby on 15 Jan 2016
 */

class ApplicationConfigurationModuleTest extends Specification {

    class LocalTestModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(Translate.class).toInstance(new MockTranslate())
            bind(RelatedServiceExecutor.class).toInstance(mock(RelatedServiceExecutor))
            bind(PathLocator.class).toInstance(mock(PathLocator))
            bind(MessageBus.class).toInstance(mock(MessageBus))
        }
    }

    List<Module> modules

    def setup() {
        modules = Lists.newArrayList(new LocalTestModule())
    }


    def "configs"() {
        given:
        modules.add(new ApplicationConfigurationModule().addConfig("/home/x", 5, false).addConfig("/home/y", 6, true))
        when:
        Injector injector = Guice.createInjector(modules)
        TypeLiteral<Map<Integer, IniFileConfig>> mapLiteral = new TypeLiteral<Map<Integer, IniFileConfig>>() {}
        Key<Map<Integer, IniFileConfig>> key = Key.get(mapLiteral)
        Map<Integer, IniFileConfig> map = injector.getInstance(key)

        then:
        (map.get(5)).filename == "/home/x"
        !(map.get(5)).optional
        (map.get(6)).filename == "/home/y"
        (map.get(6)).optional

    }
}
