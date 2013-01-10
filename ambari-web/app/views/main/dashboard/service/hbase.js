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

App.MainDashboardServiceHbaseView = App.MainDashboardServiceView.extend({
  templateName: require('templates/main/dashboard/service/hbase'),
  serviceName: 'hbase',

  masterServerHeapSummary: function () {
    var heapUsed = this.get('service').get('heapMemoryUsed');
    var heapMax = this.get('service').get('heapMemoryMax');
    var percent = heapMax > 0 ? 100 * heapUsed / heapMax : 0;
    var heapString = heapUsed > 0 ? heapUsed.bytesToSize(1, "parseFloat") : 0;
    var heapMaxString = heapMax > 0 ? heapMax.bytesToSize(1, "parseFloat") : 0;
    return this.t('dashboard.services.hbase.masterServerHeap.summary').format(heapString, heapMaxString, percent.toFixed(1));
  }.property('service.heapMemoryUsed', 'service.heapMemoryMax'),

  summaryHeader: function () {
    var avgLoad = this.get('service.averageLoad');
    if (avgLoad == null) {
      avgLoad = this.t("services.service.summary.unknown");
    }
    return this.t("dashboard.services.hbase.summary").format(this.get('service.regionServers.length'), avgLoad);
  }.property('service.regionServers', 'service.averageLoad'),

  hbaseMasterWebUrl: function () {
    return "http://" + this.get('service').get('master').get('publicHostName') + ":60010";
  }.property('service.master'),

  averageLoad: function () {
    var avgLoad = this.get('service.averageLoad');
    if (avgLoad == null) {
      avgLoad = this.t('services.service.summary.unknown');
    }
    return this.t('dashboard.services.hbase.averageLoadPerServer').format(avgLoad);
  }.property("service.averageLoad"),

  masterStartedTime: function () {
    var uptime = this.get('service').get('masterStartTime');
    if (uptime && uptime > 0) {
      var formatted = date.timingFormat((new Date().getTime() - uptime));
      return this.t('dashboard.services.uptime').format(formatted);
    }
    return this.t('services.service.summary.unknown');
  }.property("service.masterStartTime"),

  masterActivatedTime: function () {
    var uptime = this.get('service').get('masterActiveTime');
    if (uptime && uptime > 0) {
      var formatted = date.timingFormat((new Date().getTime() - uptime));
      return this.t('dashboard.services.uptime').format(formatted);
    }
    return this.t('services.service.summary.unknown');
  }.property("service.masterActiveTime"),

  regionServerComponent: function () {
    return App.Component.find().findProperty('componentName', 'HBASE_REGIONSERVER');
  }.property('components'),

  isCollapsed: false,

  toggleInfoView: function () {
    $('#hbase-info').toggle('blind', 200);
    this.set('isCollapsed', !this.isCollapsed);
  }
});