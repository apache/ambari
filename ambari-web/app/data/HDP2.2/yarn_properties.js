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
var App = require('app');

var yarnPropsToCategory = {

  'FaultTolerance': [
    'yarn.nodemanager.recovery.enabled',
    'yarn.resourcemanager.recovery.enabled',
    'yarn.resourcemanager.work-preserving-recovery.enabled',
    'yarn.resourcemanager.zk-address',
    'yarn.resourcemanager.connect.retry-interval.ms',
    'yarn.resourcemanager.connect.max-wait.ms',
    'yarn.resourcemanager.ha.enabled'
  ],

  'Isolation': [
    'yarn.nodemanager.linux-container-executor.group',
    'yarn.nodemanager.container-executor.class',
    'yarn.nodemanager.linux-container-executor.resources-handler.class',
    'yarn.nodemanager.linux-container-executor.cgroups.hierarchy',
    'yarn.nodemanager.linux-container-executor.cgroups.mount',
    'yarn.nodemanager.linux-container-executor.cgroups.strict-resource-usage'
  ],

  'CapacityScheduler': [
    'yarn.nodemanager.resource.cpu-vcores',
    'yarn.nodemanager.resource.percentage-physical-cpu-limit'
  ]
};

var yarnProps = [];

for (var category in yarnPropsToCategory) {
  yarnProps = yarnProps.concat(App.config.generateConfigPropertiesByName(yarnPropsToCategory[category],
    { category: category, serviceName: 'YARN', filename: 'yarn-site.xml'}));
}

module.exports = yarnProps;
