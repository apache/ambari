/*
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

package org.apache.ambari.server.upgrade;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * Upgrade catalog for version 2.4.0.
 */
public class UpgradeCatalog2402 extends AbstractUpgradeCatalog {

  @Inject
  ClusterDAO clusterDAO;

  @Inject
  Configuration config;

  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(UpgradeCatalog2402.class);

  // ----- Constructors ------------------------------------------------------

  /**
   * Don't forget to register new UpgradeCatalogs in {@link SchemaUpgradeHelper.UpgradeHelperModule#configure()}
   *
   * @param injector Guice injector to track dependencies and uses bindings to inject them.
   */
  @Inject
  public UpgradeCatalog2402(Injector injector) {
    super(injector);
    injector.injectMembers(this);
  }

  // ----- UpgradeCatalog ----------------------------------------------------

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTargetVersion() {
    return "2.4.0.2";
  }

  // ----- AbstractUpgradeCatalog --------------------------------------------

  /**
   * {@inheritDoc}
   */
  @Override
  public String getSourceVersion() {
    return "2.4.0";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executeDDLUpdates() throws AmbariException, SQLException {

  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executePreDMLUpdates() throws AmbariException, SQLException {

  }

  @Override
  protected void executeDMLUpdates() throws AmbariException, SQLException {
    updateKafkaWidgetDefinition();
  }


  protected void updateKafkaWidgetDefinition() throws AmbariException {
    LOG.info("Updating Kafka widget definition.");

    Map<String, List<String>> widgetMap = new HashMap<>();
    Map<String, String> sectionLayoutMap = new HashMap<>();

    List<String> kafkaSummaryWidgets = new ArrayList<>(Arrays.asList("Active Controller Count"));
    widgetMap.put("KAFKA_SUMMARY", kafkaSummaryWidgets);
    sectionLayoutMap.put("KAFKA_SUMMARY", "default_kafka_dashboard");

    updateWidgetDefinitionsForService("KAFKA", widgetMap, sectionLayoutMap);
  }

}
