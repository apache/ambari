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
var blueprintUtils = require('utils/blueprint');
var dataManipulation = require('utils/data_manipulation');

App.BlueprintMixin = Em.Mixin.create({

  /**
   * returns blueprint for all currently installed master, slave and client components
   * @method getCurrentMasterSlaveBlueprint
   */
  getCurrentMasterSlaveBlueprint: function () {
    var components = App.HostComponent.find();
    var hosts = components.mapProperty("hostName").uniq();
    var mappedComponents = dataManipulation.groupPropertyValues(components, 'hostName');

    var res = {
      blueprint: { host_groups: [] },
      blueprint_cluster_binding: { host_groups: [] }
    };

    hosts.forEach(function (host, i) {
      var group_name = 'host-group-' + (i + 1);

      res.blueprint.host_groups.push({
        name: group_name,
        components: mappedComponents[host] ? mappedComponents[host].map(function (c) {
          return { name: Em.get(c, 'componentName') };
        }) : []
      });

      res.blueprint_cluster_binding.host_groups.push({
        name: group_name,
        hosts: [
          { fqdn: host }
        ]
      });
    });
    return res;
  },

  /**
   * Returns blueprint for all currently installed slave and client components
   */
  getCurrentSlaveBlueprint: function () {
    var self = this;
    var fullBlueprint = self.getCurrentMasterSlaveBlueprint();

    var components = App.StackServiceComponent.find().filter(function (c) {
      return c.get('isSlave') || c.get('isClient');
    }).mapProperty("componentName");

    return blueprintUtils.filterByComponents(fullBlueprint, components);
  }
});