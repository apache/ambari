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

import java.util.List;

import org.apache.ambari.groovy.client.AmbariClient;
import org.apache.ambari.shell.completion.Service;
import org.springframework.shell.core.Completion;
import org.springframework.shell.core.MethodTarget;

/**
 * Converter used to complete service names.
 */
public class ServiceConverter extends AbstractConverter<Service> {

  public ServiceConverter(AmbariClient client) {
    super(client);
  }

  @Override
  public boolean supports(Class<?> type, String s) {
    return Service.class.isAssignableFrom(type);
  }

  @Override
  public boolean getAllPossibleValues(List<Completion> completions, Class<?> targetType, String existingData, String optionContext, MethodTarget target) {
    return getAllPossibleValues(completions, getClient().getServicesMap().keySet());
  }
}
