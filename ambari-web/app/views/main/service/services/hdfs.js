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

App.MainDashboardServiceHdfsView = App.MainDashboardServiceView.extend({
  templateName: require('templates/main/service/services/hdfs'),
  serviceName: 'HDFS',

  metricsNotAvailableObserver: function () {
    if(!this.get("service.metricsNotAvailable")) {
      App.tooltip($("[rel='tooltip']"));
    }
  }.observes("service.metricsNotAvailable"),

  willDestroyElement: function() {
    $("[rel='tooltip']").tooltip('destroy');
  },

  dataNodesDead: Em.computed.alias('service.dataNodesInstalled'),

  journalNodesLive: function () {
    return this.get('service.journalNodes').filterProperty("workStatus", "STARTED").get("length");
  }.property("service.journalNodes.@each.workStatus"),

  journalNodesTotal: Em.computed.alias('service.journalNodes.length'),

  dataNodeComponent: Em.Object.create({
    componentName: 'DATANODE'
  }),

  nfsGatewayComponent: Em.Object.create({
    componentName: 'NFS_GATEWAY'
  }),

  /**
   * Define if NFS_GATEWAY is present in the installed stack
   * @type {Boolean}
   */
  isNfsInStack: function () {
    return App.StackServiceComponent.find().someProperty('componentName', 'NFS_GATEWAY');
  }.property(),

  journalNodeComponent: Em.Object.create({
    componentName: 'JOURNALNODE'
  }),

  isDataNodeCreated: function () {
    return this.isServiceComponentCreated('DATANODE');
  }.property('App.router.clusterController.isComponentsStateLoaded'),

  isJournalNodeCreated: function () {
    return this.isServiceComponentCreated('JOURNALNODE');
  }.property('App.router.clusterController.isComponentsStateLoaded')
});
