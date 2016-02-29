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
require('/views/main/service/services/yarn');

function getView() {
  return App.MainDashboardServiceYARNView.create();
}

describe('App.MainDashboardServiceYARNView', function () {

  App.TestAliases.testAsComputedCountBasedMessage(getView(), 'nodeManagerText', 'service.nodeManagersTotal', '', Em.I18n.t('services.service.summary.viewHost'), Em.I18n.t('services.service.summary.viewHosts'));

  App.TestAliases.testAsComputedGt(getView(), 'hasManyYarnClients', 'service.installedClients', 1);

  App.TestAliases.testAsComputedFormatNa(getView(), '_nmActive', 'service.nodeManagersCountActive');
  App.TestAliases.testAsComputedFormatNa(getView(), '_nmLost', 'service.nodeManagersCountLost');
  App.TestAliases.testAsComputedFormatNa(getView(), '_nmUnhealthy', 'service.nodeManagersCountUnhealthy');
  App.TestAliases.testAsComputedFormatNa(getView(), '_nmRebooted', 'service.nodeManagersCountRebooted');
  App.TestAliases.testAsComputedFormatNa(getView(), '_nmDecom', 'service.nodeManagersCountDecommissioned');

  App.TestAliases.testAsComputedFormatNa(getView(), '_allocated', 'service.containersAllocated');
  App.TestAliases.testAsComputedFormatNa(getView(), '_pending', 'service.containersPending');
  App.TestAliases.testAsComputedFormatNa(getView(), '_reserved', 'service.containersReserved');

  App.TestAliases.testAsComputedFormatNa(getView(), '_appsSubmitted', 'service.appsSubmitted');
  App.TestAliases.testAsComputedFormatNa(getView(), '_appsRunning', 'service.appsRunning');
  App.TestAliases.testAsComputedFormatNa(getView(), '_appsPending', 'service.appsPending');
  App.TestAliases.testAsComputedFormatNa(getView(), '_appsCompleted', 'service.appsCompleted');
  App.TestAliases.testAsComputedFormatNa(getView(), '_appsKilled', 'service.appsKilled');
  App.TestAliases.testAsComputedFormatNa(getView(), '_appsFailed', 'service.appsFailed');

  App.TestAliases.testAsComputedFormatNa(getView(), '_queuesCountFormatted', 'service.queuesCount');

});