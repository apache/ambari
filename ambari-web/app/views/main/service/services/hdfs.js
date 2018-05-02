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
var date = require('utils/date/date');
require('views/main/service/info/summary/hdfs/slaves');

App.MainDashboardServiceHdfsView = App.MainDashboardServiceView.extend({
  templateName: require('templates/main/service/services/hdfs'),
  serviceName: 'HDFS',

  slaveComponentsView: App.HDFSSlaveComponentsView.extend(),

  metricsNotAvailableObserver: function () {
    if(!this.get("service.metricsNotAvailable")) {
      App.tooltip($("[rel='tooltip']"));
    }
  }.observes("service.metricsNotAvailable"),

  willDestroyElement: function() {
    $("[rel='tooltip']").tooltip('destroy');
  }
});
