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
package org.apache.ambari.shell.converter;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.List;

import org.apache.ambari.groovy.client.AmbariClient;
import org.apache.ambari.shell.completion.AbstractCompletion;
import org.springframework.shell.core.Completion;
import org.springframework.shell.core.Converter;

/**
 * Base class of completion converters.
 *
 * @param <T> completion class type
 */
public abstract class AbstractConverter<T extends AbstractCompletion> implements Converter<T> {

  private AmbariClient client;

  protected AbstractConverter(AmbariClient client) {
    this.client = client;
  }

  @Override
  public T convertFromText(String value, Class<?> clazz, String optionContext) {
    try {
      Constructor<?> constructor = clazz.getDeclaredConstructor(String.class);
      return (T) constructor.newInstance(value);
    } catch (Exception e) {
      return null;
    }
  }

  public boolean getAllPossibleValues(List<Completion> completions, Collection<String> values) {
    for (String value : values) {
      completions.add(new Completion(value));
    }
    return true;
  }

  public AmbariClient getClient() {
    return client;
  }

}
