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

App.HDFSLinksView = App.LinkDashboardWidgetView.extend({

  templateName: require('templates/main/dashboard/widgets/hdfs_links'),
  title: Em.I18n.t('dashboard.widgets.HDFSLinks'),
  id: '11',

  model_type: 'hdfs',

  port: '50070',

  componentName: 'DATANODE',

  modelField: 'nameNode',

  didInsertElement: function() {
    this._super();
    this.calc();
  },

  isHAEnabled: function() {
    return !this.get('model.snameNode');
  }.property('model.snameNode'),
  isActiveNNValid: function () {
    return this.get('model.activeNameNode') != null;
  }.property('model.activeNameNode'),
  isStandbyNNValid: function () {
    return this.get('model.standbyNameNode') != null;
  }.property('model.standbyNameNode'),
  isTwoStandbyNN: function () {
    return (this.get('model.standbyNameNode') != null && this.get('model.standbyNameNode2') != null);
  }.property('model.standbyNameNode', 'model.standbyNameNode2'),
  twoStandbyComponent: function () {
    return App.HostComponent.find().findProperty('componentName', 'NAMENODE');
  }.property()
});