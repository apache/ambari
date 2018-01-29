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
 * distributed under the License is distribut
 * ed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.topology;

/**
 * Provides blueprint validation.
 */
public interface BlueprintValidator {
  /**
   * Validate blueprint topology.
   *
   * @throws InvalidTopologyException if the topology is invalid
   */
  void validateTopology(Blueprint blueprint) throws InvalidTopologyException;

  /**
   * Validate that required properties are provided.
   * This doesn't include password properties.
   *
   * @throws InvalidTopologyException if required properties are not set in blueprint
   * @throws GPLLicenseNotAcceptedException ambari was configured to use gpl software, but gpl license is not accepted
   */
  void validateRequiredProperties(Blueprint blueprint) throws InvalidTopologyException, GPLLicenseNotAcceptedException;
}
