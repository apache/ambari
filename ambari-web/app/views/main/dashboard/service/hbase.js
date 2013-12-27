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
var numberUtils = require('utils/number_utils');

App.MainDashboardServiceHbaseView = App.MainDashboardServiceView.extend({
  templateName: require('templates/main/dashboard/service/hbase'),
  serviceName: 'hbase',
  /**
   * All master components
   */
  masters: function () {
    return this.get('service.hostComponents').filterProperty('isMaster', true);
  }.property('service.hostComponents.@each'),
  /**
   * Passive master components
   */
  passiveMasters: function () {
    if(App.supports.multipleHBaseMasters){
      return this.get('masters').filterProperty('haStatus', 'false');
    }
    return [];
  }.property('masters'),


  liveRegionServers: function () {
    return App.HostComponent.find().filterProperty('componentName', 'HBASE_REGIONSERVER').filterProperty("workStatus","STARTED");
  }.property('service.hostComponents.@each'),

  regionServesText: function () {
    if (this.get('service.regionServers.length') == 0) {
      return '';
    } else if (this.get('service.regionServers.length') > 1) {
      return Em.I18n.t('services.service.summary.viewHosts');
    } else {
      return Em.I18n.t('services.service.summary.viewHost');
    }
  }.property("service"),

  regionServersLiveTextView: App.ComponentLiveTextView.extend({
    liveComponents: function() {
      return App.HostComponent.find().filterProperty('componentName', 'HBASE_REGIONSERVER').filterProperty("workStatus","STARTED").get('length');
    }.property("service.hostComponents.@each"),
    totalComponents: function() {
      return this.get("service.regionServers.length");
    }.property("service.regionServers.length")
  }),

  /**
   * One(!) active master component
   */
  activeMaster: function () {
    if(App.supports.multipleHBaseMasters){
      return this.get('masters').findProperty('haStatus', 'true');
    } else {
      return this.get('masters')[0];
    }
  }.property('masters'),

  activeMasterTitle: function(){
    if(App.supports.multipleHBaseMasters){
      return this.t('service.hbase.activeMaster');
    } else {
      return this.get('activeMaster.host.publicHostName');
    }
  }.property('activeMaster'),

  masterServerHeapSummary: function () {
    var heapUsed = this.get('service').get('heapMemoryUsed');
    var heapMax = this.get('service').get('heapMemoryMax');
    var percent = heapMax > 0 ? 100 * heapUsed / heapMax : 0;
    var heapString = numberUtils.bytesToSize(heapUsed, 1, "parseFloat");
    var heapMaxString = numberUtils.bytesToSize(heapMax, 1, "parseFloat");
    return this.t('dashboard.services.hbase.masterServerHeap.summary').format(heapString, heapMaxString, percent.toFixed(1));
  }.property('service.heapMemoryUsed', 'service.heapMemoryMax'),

  summaryHeader: function () {
    var avgLoad = this.get('service.averageLoad');
    if (isNaN(avgLoad)) {
      avgLoad = this.t("services.service.summary.unknown");
    }
    return this.t("dashboard.services.hbase.summary").format(this.get('service.regionServers.length'), avgLoad);
  }.property('service.regionServers', 'service.averageLoad'),

  hbaseMasterWebUrl: function () {
    if (this.get('activeMaster.host') && this.get('activeMaster.host').get('publicHostName')) {
      return "http://" + (App.singleNodeInstall ? App.singleNodeAlias : this.get('activeMaster.host').get('publicHostName')) + ":60010";
    }
  }.property('activeMaster'),

  averageLoad: function () {
    var avgLoad = this.get('service.averageLoad');
    if (isNaN(avgLoad)) {
      avgLoad = this.t('services.service.summary.notAvailable');
    }
    return this.t('dashboard.services.hbase.averageLoadPerServer').format(avgLoad);
  }.property("service.averageLoad"),

  masterStartedTime: function () {
    var uptime = this.get('service').get('masterStartTime');
    if (uptime && uptime > 0) {
      var diff = (new Date()).getTime() - uptime;
      if (diff < 0) {
        diff = 0;
      }
      var formatted = date.timingFormat(diff);
      return this.t('dashboard.services.uptime').format(formatted);
    }
    return this.t('services.service.summary.notRunning');
  }.property("service.masterStartTime"),

  masterActivatedTime: function () {
    var uptime = this.get('service').get('masterActiveTime');
    if (uptime && uptime > 0) {
      var diff = (new Date()).getTime() - uptime;
      if (diff < 0) {
        diff = 0;
      }
      var formatted = date.timingFormat(diff);
      return this.t('dashboard.services.uptime').format(formatted);
    }
    return this.t('services.service.summary.notRunning');
  }.property("service.masterActiveTime"),

  regionServerComponent: function () {
    return App.HostComponent.find().findProperty('componentName', 'HBASE_REGIONSERVER');
  }.property()

});