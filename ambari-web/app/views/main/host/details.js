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
var date = require('utils/date');

App.MainHostDetailsView = Em.View.extend({
  templateName: require('templates/main/host/details'),

  content: function(){
    return App.router.get('mainHostDetailsController.content');
  }.property('App.router.mainHostDetailsController.content'),

  maintenance: function(){
    var options = [{action: 'deleteHost', 'label': 'Delete Host'}];
    return options;
  }.property('controller.content'),

  healthToolTip: function(){
    var hostComponents = this.get('content.hostComponents').filter(function(item){
      if(item.get('workStatus') !== App.HostComponentStatus.started){
        return true;
      }
    });
    var output = '';
    switch (this.get('content.healthClass')){
      case 'health-status-DEAD':
        hostComponents = hostComponents.filterProperty('isMaster', true);
        output = Em.I18n.t('hosts.host.healthStatus.mastersDown');
        hostComponents.forEach(function(hc, index){
          output += (index == (hostComponents.length-1)) ? hc.get('displayName') : (hc.get('displayName')+", ");
        }, this);
        break;
      case 'health-status-DEAD-YELLOW':
        output = Em.I18n.t('hosts.host.healthStatus.heartBeatNotReceived');
        break;
      case 'health-status-DEAD-ORANGE':
        hostComponents = hostComponents.filterProperty('isSlave', true);
        output = Em.I18n.t('hosts.host.healthStatus.slavesDown');
        hostComponents.forEach(function(hc, index){
          output += (index == (hostComponents.length-1)) ? hc.get('displayName') : (hc.get('displayName')+", ");
        }, this);
        break;
    }
    return output;
  }.property('content.healthClass')
});
