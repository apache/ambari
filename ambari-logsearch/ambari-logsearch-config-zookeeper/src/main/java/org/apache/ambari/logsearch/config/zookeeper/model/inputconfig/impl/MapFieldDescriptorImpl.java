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

package org.apache.ambari.logsearch.config.zookeeper.model.inputconfig.impl;

import org.apache.ambari.logsearch.config.api.ShipperConfigTypeDescription;
import org.apache.ambari.logsearch.config.api.model.inputconfig.MapFieldDescriptor;

@ShipperConfigTypeDescription(
    name = "Post Map Values",
    description = "The Post Map Values element in the [filter](filter.md) field names as keys, the values are lists of sets of " +
                  "post map values, each describing one mapping done on a field named before obtained after filtering.\n" +
                  "\n" +
                  "Currently there are four kind of mappings are supported:"
  )
public abstract class MapFieldDescriptorImpl implements MapFieldDescriptor {
}
