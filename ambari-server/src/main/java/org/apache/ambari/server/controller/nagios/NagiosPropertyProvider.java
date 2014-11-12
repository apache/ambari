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
package org.apache.ambari.server.controller.nagios;

import java.util.HashSet;
import java.util.Set;

import org.apache.ambari.server.controller.internal.BaseProvider;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.PropertyProvider;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;

/**
 * Deprecated. To be removed for Ambari 2.0.0 after the web client has removed
 * references to {@code legacy_alerts}.
 */
@Deprecated
public class NagiosPropertyProvider extends BaseProvider implements PropertyProvider {

  private static final Set<String> NAGIOS_PROPERTY_IDS = new HashSet<String>();

  private static final String ALERT_DETAIL_PROPERTY_ID = "legacy_alerts/detail";
  private static final String ALERT_SUMMARY_PROPERTY_ID = "legacy_alerts/summary";

  static {
    NAGIOS_PROPERTY_IDS.add(ALERT_SUMMARY_PROPERTY_ID);
    NAGIOS_PROPERTY_IDS.add(ALERT_DETAIL_PROPERTY_ID);
  }

  @Deprecated
  public NagiosPropertyProvider() {
    super(NAGIOS_PROPERTY_IDS);
  }

  @Override
  public Set<Resource> populateResources(Set<Resource> resources,
      Request request, Predicate predicate) throws SystemException {
    return resources;
  }
}
