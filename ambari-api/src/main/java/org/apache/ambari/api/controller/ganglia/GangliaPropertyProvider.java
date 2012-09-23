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

package org.apache.ambari.api.controller.ganglia;

import org.apache.ambari.api.controller.internal.PropertyIdImpl;
import org.apache.ambari.api.controller.spi.Predicate;
import org.apache.ambari.api.controller.spi.PropertyId;
import org.apache.ambari.api.controller.spi.PropertyProvider;
import org.apache.ambari.api.controller.spi.Request;
import org.apache.ambari.api.controller.spi.Resource;
import org.apache.ambari.api.controller.utilities.PredicateHelper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Property provider implementation for a Ganglia source.
 */
public class GangliaPropertyProvider implements PropertyProvider {

  /**
   * Map of property ids supported by this provider.
   */
  private static final Set<PropertyId> PROPERTY_IDS = new HashSet<PropertyId>();

  static {
    PROPERTY_IDS.add(new PropertyIdImpl("cpu_nice", null, true));
    PROPERTY_IDS.add(new PropertyIdImpl("cpu_nice", null, true));
    PROPERTY_IDS.add(new PropertyIdImpl("cpu_wio", null, true));
    PROPERTY_IDS.add(new PropertyIdImpl("cpu_user", null, true));
    PROPERTY_IDS.add(new PropertyIdImpl("cpu_idle", null, true));
    PROPERTY_IDS.add(new PropertyIdImpl("cpu_system", null, true));
    PROPERTY_IDS.add(new PropertyIdImpl("cpu_aidle", null, true));
    PROPERTY_IDS.add(new PropertyIdImpl("disk_free", null, true));
    PROPERTY_IDS.add(new PropertyIdImpl("disk_total", null, true));
    PROPERTY_IDS.add(new PropertyIdImpl("part_max_used", null, true));
    PROPERTY_IDS.add(new PropertyIdImpl("load_one", null, true));
    PROPERTY_IDS.add(new PropertyIdImpl("load_five", null, true));
    PROPERTY_IDS.add(new PropertyIdImpl("load_fifteen", null, true));
    PROPERTY_IDS.add(new PropertyIdImpl("swap_free", null, true));
    PROPERTY_IDS.add(new PropertyIdImpl("mem_cached", null, true));
    PROPERTY_IDS.add(new PropertyIdImpl("mem_free", null, true));
    PROPERTY_IDS.add(new PropertyIdImpl("mem_buffers", null, true));
    PROPERTY_IDS.add(new PropertyIdImpl("mem_shared", null, true));
    PROPERTY_IDS.add(new PropertyIdImpl("bytes_out", null, true));
    PROPERTY_IDS.add(new PropertyIdImpl("bytes_in", null, true));
    PROPERTY_IDS.add(new PropertyIdImpl("pkts_in", null, true));
    PROPERTY_IDS.add(new PropertyIdImpl("pkts_out", null, true));
    PROPERTY_IDS.add(new PropertyIdImpl("proc_run", null, true));
    PROPERTY_IDS.add(new PropertyIdImpl("proc_total", null, true));
  }

  /**
   * The Ganglia source.
   * //TODO : where do we get this from?
   */
  private static final String GANGLIA_SOURCE = "ec2-107-22-86-120.compute-1.amazonaws.com";


  /**
   * Map of Ganglia cluster names keyed by component type.
   */
  private static final Map<String, String> COMPONENT_MAP = new HashMap<String, String>();

  static {
    COMPONENT_MAP.put("NAMENODE", "HDPNameNode");
    COMPONENT_MAP.put("JOBTRACKER", "HDPJobTracker");
//        COMPONENT_MAP.put("???", "HDPSlaves");
  }

  @Override
  public Set<Resource> populateResources(Set<Resource> resources, Request request, Predicate predicate) {
    Set<Resource> keepers = new HashSet<Resource>();
    for (Resource resource : resources) {
      if (populateResource(resource, request, predicate)) {
        keepers.add(resource);
      }
    }
    return keepers;
  }

  @Override
  public Set<PropertyId> getPropertyIds() {
    return PROPERTY_IDS;
  }

  private boolean populateResource(Resource resource, Request request, Predicate predicate) {

    Set<PropertyId> ids = new HashSet<PropertyId>(request.getPropertyIds());
    if (ids == null || ids.isEmpty()) {
      ids = getPropertyIds();
    } else {
      if (predicate != null) {
        ids.addAll(PredicateHelper.getPropertyIds(predicate));
      }
      ids.retainAll(getPropertyIds());
    }

    String host = resource.getPropertyValue(new PropertyIdImpl("host_name", "HostRoles", false));
    String component = resource.getPropertyValue(new PropertyIdImpl("component_name", "HostRoles", false));

    // ---- TODO : HACK to fix host name that's been made all lower case... Ganglia doesn't like!!
    host = hackHostName(host);
    // -----

    String cluster = COMPONENT_MAP.get(component);

    if (cluster == null) {
      return true;
    }

    for (PropertyId propertyId : ids) {

      String property = (propertyId.getCategory() == null ? "" : propertyId.getCategory() + ".") +
          propertyId.getName();

      List<GangliaMetric> properties = GangliaHelper.getGangliaProperty(GANGLIA_SOURCE, cluster, host, property);

      double[][] dataPoints = properties.get(0).getDatapoints();

      resource.setProperty(propertyId, getTemporalValue(dataPoints));
    }
    return true;
  }

  private String getTemporalValue(double[][] dataPoints) {
    boolean first = true;
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("[");
    for (double[] m : dataPoints) {
      if (!first) {
        stringBuilder.append(",");
      }
      stringBuilder.append("[");
      stringBuilder.append(m[0]);
      stringBuilder.append(",");
      stringBuilder.append((long) m[1]);
      stringBuilder.append("]");
      first = false;
    }
    stringBuilder.append("]");
    return stringBuilder.toString();
  }

  private String hackHostName(String host) {
    int first_dash = host.indexOf('-');
    int first_dot = host.indexOf('.');
    String segment1 = host.substring(0, first_dash);
    if (segment1.equals("domu")) {
      segment1 = "domU";
    }
    String segment2 = host.substring(first_dash, first_dot).toUpperCase();
    host = segment1 + segment2 + host.substring(first_dot);
    return host;
  }
}

