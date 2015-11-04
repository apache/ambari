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

package org.apache.ambari.server.topology;

public enum ConfigRecommendationStrategy {
  /**
   *  Configuration recommendations are always applied, overriding stack defaults and
   *  configuration defined by the user in the Blueprint and/or Cluster Creation Template.
   */
  ALWAYS_APPLY,
  /**
   * Configuration recommendations are ignored with this option, both for stack defaults
   * and configuration defined by the user in the Blueprint and/or Cluster Creation Template.
   */
  NEVER_APPLY,
  /**
   *  Configuration recommendations are always applied for properties listed as stack defaults,
   *  but not for configurations defined by the user in the Blueprint and/or Cluster Creation Template.
   */
  ONLY_STACK_DEFAULTS_APPLY;
}
