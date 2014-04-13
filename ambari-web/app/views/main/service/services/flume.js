/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');
var date = require('utils/date');

App.MainDashboardServiceFlumeView = App.MainDashboardServiceView.extend({
  templateName: require('templates/main/service/services/flume'),
  serviceName: 'flume',

  summaryHeader : function() {
    var agents = App.FlumeService.find().objectAt(0).get('agents');//this.get('service.agents');
    var agentCount = agents.get('length');
    var hostCount = agents.mapProperty('host').uniq().get('length');
    var prefix = agentCount == 1 ? this.t("dashboard.services.flume.summary.single") :
      this.t("dashboard.services.flume.summary.multiple").format(agentCount);
    var suffix = hostCount == 1 ? this.t("dashboard.services.flume.summary.hosts.single") :
      this.t("dashboard.services.flume.summary.hosts.multiple").format(hostCount);
    return prefix + suffix;
  }.property('service.agents'),

  flumeServerComponent: function() {
    return App.HostComponent.find().findProperty('componentName', 'FLUME_SERVER');
  }.property(),
});
