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
var date = require('utils/date/date');

App.MainHostDetailsView = Em.View.extend({
  templateName: require('templates/main/host/details'),

  /**
   * flag identify whether current host exist and loaded to model
   */
  isLoaded: false,

  content: function(){
    return App.router.get('mainHostDetailsController.content');
  }.property('App.router.mainHostDetailsController.content'),

  clients: function () {
    return this.get('content.hostComponents').filterProperty('isClient');
  }.property('content.hostComponents.@each'),

  clientsWithConfigs: function() {
    return this.get('content.hostComponents').filterProperty('isClient').filter(function(client) {
      return !App.get('services.noConfigTypes').contains(client.get('service.serviceName'));
    });
  }.property('content.hostComponents.@each'),

  isActive: function() {
    return this.get('controller.content.passiveState') === "OFF";
  }.property('controller.content.passiveState'),

  maintenance: function(){
    var onOff = this.get('isActive') ? "On" : "Off";
    return [
      {action: 'startAllComponents', liClass: (this.get('controller.content.isNotHeartBeating')?'disabled':'enabled'), cssClass: 'icon-play', 'label': this.t('hosts.host.details.startAllComponents')},
      {action: 'stopAllComponents', liClass: (this.get('controller.content.isNotHeartBeating')?'disabled':'enabled'), cssClass: 'icon-stop', 'label': this.t('hosts.host.details.stopAllComponents')},
      {action: 'restartAllComponents', liClass: (this.get('controller.content.isNotHeartBeating')?'disabled':'enabled'), cssClass: 'icon-repeat', 'label': this.t('hosts.host.details.restartAllComponents')},
      {action: 'onOffPassiveModeForHost', liClass:'', cssClass: 'icon-medkit', active:this.get('isActive'), 'label': this.t('passiveState.turn' + onOff)},
      {action: 'deleteHost', liClass:'', cssClass: 'icon-remove', 'label': this.t('hosts.host.details.deleteHost')},
      {action: 'setRackId', liClass:'', cssClass: 'icon-gear', 'label': this.t('hosts.host.details.setRackId')}
    ];
  }.property('controller.content','isActive', 'controller.content.isNotHeartBeating'),
  didInsertElement: function() {
    var self = this;
    var host = self.get('content');

    this.set('isLoaded', App.Host.find(host.get('id')).get('isLoaded'));
    App.router.get('updateController').updateHost(function () {
      self.set('isLoaded', true);
      App.tooltip($("[rel='HealthTooltip']"));
      if (!host.get('isLoaded')) {
        //if host is not existed then route to list of hosts
        App.router.transitionTo('main.hosts.index');
      }
    });
  }
});
