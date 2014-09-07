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

App.BlueprintMixin = Em.Mixin.create({
  /**
   * returns blueprint for all currenlty installed master, slave and client components
   */
  getCurrentMasterSlaveBlueprint: function () {
    var components = App.HostComponent.find();
    var hosts = components.mapProperty("hostName").uniq();

    var res = {
      blueprint: { host_groups: [] },
      blueprint_cluster_binding: { host_groups: [] }
    };

    hosts.forEach(function (host, i) {
      var group_name = 'host-group-' + (i+1);

      res.blueprint.host_groups.push({
        name: group_name,
        components: components.filterProperty("hostName", host).mapProperty("componentName").map(function (c) {
          return { name: c };
        })
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
   * returns blueprint for all currenlty installed slave and client components
   */
  getCurrentSlaveBlueprint: function () {
    var self = this;
    var fullBlueprint = self.getCurrentMasterSlaveBlueprint();

    var slaveComponents = App.StackServiceComponent.find().filterProperty("isSlave").mapProperty("componentName");
    var clientComponents = App.StackServiceComponent.find().filterProperty("isClient").mapProperty("componentName");
    var slaveAndClientComponents = slaveComponents.concat(clientComponents);

    return blueprintUtils.filterByComponents(fullBlueprint, slaveAndClientComponents);
  }
});