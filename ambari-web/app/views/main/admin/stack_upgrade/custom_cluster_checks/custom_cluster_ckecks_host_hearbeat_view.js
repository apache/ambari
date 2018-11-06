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

App.HostsHeartbeatView = Em.View.extend({
  templateName: require('templates/main/admin/stack_upgrade/custom_cluster_checks/custom_cluster_checks_hosts_heartbeat'),

  hosts: function () {
    var self = this;
    return App.Host.find().toArray().filter( function (host) {
      return self.get('check.failed_on').contains(host.get('id'));
    });
  }.property(''),

  removeHost: function (event) {
    var controller =  App.router.get('mainHostDetailsController');
    controller.set('content', event.context);
    controller.validateAndDeleteHost();
  }
});