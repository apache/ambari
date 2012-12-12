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

App.MainServiceItemView = Em.View.extend({
  templateName: require('templates/main/service/item'),
  maintenance: function(){
    var options = [];
    var service = this.get('controller.content');
    switch(service.get('serviceName')) {
//      case 'HDFS':
//        options.push({action: 'runRebalancer', 'label': Em.I18n.t('services.service.actions.run.rebalancer')});
//        break;
//      case 'HBASE':
//        options.push({action: 'runCompaction', 'label': Em.I18n.t('services.service.actions.run.compaction')});
//        break;
      default:
        options.push({action: 'runSmokeTest', 'label': Em.I18n.t('services.service.actions.run.smoke')});
    }
    return options;
  }.property('controller.content'),
  hasMaintenanceControl: function(){
    return this.get("controller.content.isMaintained");
  }.property('controller.content.isMaintained'),
  hasConfigTab: function(){
    return this.get("controller.content.isConfigurable");
  }.property('controller.content.isConfigurable')
});

App.MainServiceItemOperations = Em.View.extend({
  content: null,
  classNames: ['background-operations'],
  classNameBindings: ['isOpen'],
  isOpen: false,
  logDetails: null,
  isOpenShowLog: false,
  iconClass: function(){
    return this.get('isOpen') ? 'icon-minus' : 'icon-plus';
  }.property('isOpen'),
  openDetails: function(){
    this.set('isOpen', !this.get('isOpen'))
  },
  showOperationLog:function(){
    this.set('isOpenShowLog', !this.get('isOpenShowLog'))
  }
});