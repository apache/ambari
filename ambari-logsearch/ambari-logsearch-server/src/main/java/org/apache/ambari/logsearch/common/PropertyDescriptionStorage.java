/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ambari.logsearch.common;

import org.apache.ambari.logsearch.config.api.LogSearchPropertyDescription;
import org.apache.ambari.logsearch.model.response.PropertyDescriptionData;
import org.reflections.Reflections;
import org.reflections.scanners.FieldAnnotationsScanner;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import javax.inject.Named;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.apache.ambari.logsearch.common.LogSearchConstants.LOGSEARCH_PROPERTIES_FILE;

@Named
public class PropertyDescriptionStorage {

  private final Map<String, List<PropertyDescriptionData>> propertyDescriptions = new ConcurrentHashMap<>();

  @Value("#{'${logsearch.doc.scan.prop.packages:org.apache.ambari.logsearch,org.apache.ambari.logfeeder}'.split(',')}")
  @LogSearchPropertyDescription(
    name = "logsearch.doc.scan.prop.packages",
    description = "Comma separated list of packages for scanning @LogSearchPropertyDescription annotations.",
    examples = {"org.apache.ambari.logsearch.mypackage"},
    defaultValue = "org.apache.ambari.logsearch,org.apache.ambari.logfeeder",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private List<String> packagesToScan;

  @PostConstruct
  public void postConstruct() {
    Thread loadPropertyDescriptionsThread = new Thread("load_property_descriptions") {
      @Override
      public void run() {
        fillPropertyDescriptions();
      }
    };
    loadPropertyDescriptionsThread.setDaemon(true);
    loadPropertyDescriptionsThread.start();
  }

  public Map<String, List<PropertyDescriptionData>> getPropertyDescriptions() {
    return propertyDescriptions;
  }

  private void fillPropertyDescriptions() {
    List<PropertyDescriptionData> propertyDescriptionsList = getPropertyDescriptions(packagesToScan);
    Map<String, List<PropertyDescriptionData>> mapToAdd = propertyDescriptionsList.stream()
      .sorted((o1, o2) -> o1.getName().compareTo(o2.getName()))
      .collect(Collectors.groupingBy(PropertyDescriptionData::getSource));
    propertyDescriptions.putAll(mapToAdd);
  }

  private List<PropertyDescriptionData> getPropertyDescriptions(List<String> packagesToScan) {
    List<PropertyDescriptionData> result = new ArrayList<>();
    for (String packageToScan : packagesToScan) {
      Reflections reflections = new Reflections(packageToScan, new FieldAnnotationsScanner(), new MethodAnnotationsScanner());
      Set<Field> fields = reflections.getFieldsAnnotatedWith(LogSearchPropertyDescription.class);
      for (Field field : fields) {
        LogSearchPropertyDescription propDescription = field.getAnnotation(LogSearchPropertyDescription.class);
        for (String source : propDescription.sources()) {
          result.add(new PropertyDescriptionData(propDescription.name(), propDescription.description(), propDescription.examples(), propDescription.defaultValue(), source));
        }
      }
      Set<Method> methods = reflections.getMethodsAnnotatedWith(LogSearchPropertyDescription.class);
      for (Method method : methods) {
        LogSearchPropertyDescription propDescription = method.getAnnotation(LogSearchPropertyDescription.class);
        for (String source : propDescription.sources()) {
          result.add(new PropertyDescriptionData(propDescription.name(), propDescription.description(), propDescription.examples(), propDescription.defaultValue(), source));
        }
      }
    }
    return result;
  }
}
