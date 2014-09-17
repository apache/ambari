/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.shell.configuration;

import org.apache.ambari.groovy.client.AmbariClient;
import org.apache.ambari.shell.converter.BlueprintConverter;
import org.apache.ambari.shell.converter.ConfigTypeConverter;
import org.apache.ambari.shell.converter.HostConverter;
import org.apache.ambari.shell.converter.ServiceConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.converters.AvailableCommandsConverter;
import org.springframework.shell.converters.BigDecimalConverter;
import org.springframework.shell.converters.BigIntegerConverter;
import org.springframework.shell.converters.BooleanConverter;
import org.springframework.shell.converters.CharacterConverter;
import org.springframework.shell.converters.DateConverter;
import org.springframework.shell.converters.DoubleConverter;
import org.springframework.shell.converters.EnumConverter;
import org.springframework.shell.converters.FloatConverter;
import org.springframework.shell.converters.IntegerConverter;
import org.springframework.shell.converters.LocaleConverter;
import org.springframework.shell.converters.LongConverter;
import org.springframework.shell.converters.ShortConverter;
import org.springframework.shell.converters.SimpleFileConverter;
import org.springframework.shell.converters.StaticFieldConverterImpl;
import org.springframework.shell.converters.StringConverter;
import org.springframework.shell.core.Converter;

/**
 * Configures the converters used by the shell.
 */
@Configuration
public class ConverterConfiguration {

  @Autowired
  private AmbariClient client;

  @Bean
  Converter simpleFileConverter() {
    return new SimpleFileConverter();
  }

  @Bean
  Converter stringConverter() {
    return new StringConverter();
  }

  @Bean
  Converter availableCommandsConverter() {
    return new AvailableCommandsConverter();
  }

  @Bean
  Converter bigDecimalConverter() {
    return new BigDecimalConverter();
  }

  @Bean
  Converter bigIntegerConverter() {
    return new BigIntegerConverter();
  }

  @Bean
  Converter booleanConverter() {
    return new BooleanConverter();
  }

  @Bean
  Converter characterConverter() {
    return new CharacterConverter();
  }

  @Bean
  Converter dateConverter() {
    return new DateConverter();
  }

  @Bean
  Converter doubleConverter() {
    return new DoubleConverter();
  }

  @Bean
  Converter enumConverter() {
    return new EnumConverter();
  }

  @Bean
  Converter floatConverter() {
    return new FloatConverter();
  }

  @Bean
  Converter integerConverter() {
    return new IntegerConverter();
  }

  @Bean
  Converter localeConverter() {
    return new LocaleConverter();
  }

  @Bean
  Converter longConverter() {
    return new LongConverter();
  }

  @Bean
  Converter shortConverter() {
    return new ShortConverter();
  }

  @Bean
  Converter staticFieldConverterImpl() {
    return new StaticFieldConverterImpl();
  }

  @Bean
  Converter blueprintConverter() {
    return new BlueprintConverter(client);
  }

  @Bean
  Converter hostConverter() {
    return new HostConverter(client);
  }

  @Bean
  Converter serviceConverter() {
    return new ServiceConverter(client);
  }

  @Bean
  Converter configConverter() {
    return new ConfigTypeConverter(client);
  }
}
