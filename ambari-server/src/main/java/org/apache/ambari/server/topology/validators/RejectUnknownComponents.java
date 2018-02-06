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
package org.apache.ambari.server.topology.validators;

import static java.util.stream.Collectors.joining;

import org.apache.ambari.server.controller.RootComponent;
import org.apache.ambari.server.topology.ClusterTopology;
import org.apache.ambari.server.topology.InvalidTopologyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RejectUnknownComponents implements TopologyValidator {

  private static final Logger LOG = LoggerFactory.getLogger(RejectUnknownComponents.class);

  @Override
  public void validate(ClusterTopology topology) throws InvalidTopologyException {
    String unknownComponents = topology.getComponentNames()
      .filter(c -> !RootComponent.AMBARI_SERVER.name().equals(c))
      .filter(c -> !topology.getStack().getComponents().contains(c))
      .collect(joining(", "));

    if (!unknownComponents.isEmpty()) {
      String msg = "The following components are not valid for the specified stacks: " + unknownComponents;
      LOG.info(msg);
      throw new InvalidTopologyException(msg);
    }
  }
}
