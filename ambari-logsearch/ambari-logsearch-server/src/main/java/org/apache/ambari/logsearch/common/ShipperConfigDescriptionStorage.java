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

import org.apache.ambari.logsearch.config.api.ShipperConfigElementDescription;
import org.apache.ambari.logsearch.model.response.ShipperConfigDescriptionData;
import org.reflections.Reflections;
import org.reflections.scanners.FieldAnnotationsScanner;

import javax.annotation.PostConstruct;
import javax.inject.Named;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Named
public class ShipperConfigDescriptionStorage {

  private static final String SHIPPER_CONFIG_PACKAGE = "org.apache.ambari.logsearch.config.zookeeper.model.inputconfig.impl";
  
  private final List<ShipperConfigDescriptionData> shipperConfigDescription = new ArrayList<>();

  @PostConstruct
  public void postConstruct() {
    Thread loadShipperConfigDescriptionThread = new Thread("load_shipper_config_description") {
      @Override
      public void run() {
        fillShipperConfigDescriptions();
      }
    };
    loadShipperConfigDescriptionThread.setDaemon(true);
    loadShipperConfigDescriptionThread.start();
  }

  public List<ShipperConfigDescriptionData> getShipperConfigDescription() {
    return shipperConfigDescription;
  }

  private void fillShipperConfigDescriptions() {
    Reflections reflections = new Reflections(SHIPPER_CONFIG_PACKAGE, new FieldAnnotationsScanner());
    Set<Field> fields = reflections.getFieldsAnnotatedWith(ShipperConfigElementDescription.class);
    for (Field field : fields) {
      ShipperConfigElementDescription description = field.getAnnotation(ShipperConfigElementDescription.class);
      shipperConfigDescription.add(new ShipperConfigDescriptionData(description.path(), description.description(),
          description.examples(), description.defaultValue()));
    }
    
    shipperConfigDescription.sort((o1, o2) -> o1.getPath().compareTo(o2.getPath()));
  }
}
