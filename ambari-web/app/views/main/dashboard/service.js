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
var uiEffects = require('utils/ui_effects');

require('models/alert');

App.MainDashboardServiceHealthView = Em.View.extend({
  classNameBindings: ["healthStatus"],
  //template: Em.Handlebars.compile(""),
  blink: false,
  tagName: 'span',
  
  /**
   * When set to true, extending classes should
   * show only tabular rows as they will be 
   * embedded into other tables.
   */
  showOnlyRows: false,

  startBlink: function () {
    this.set('blink', true);
  },

  doBlink: function () {
    var self = this;
    if (this.get('blink') && (this.get("state") == "inDOM")) {
      uiEffects.pulsate(self.$(), 1000, function(){
        self.doBlink();
      });
    }
  }.observes('blink'),

  stopBlink: function () {
    this.set('blink', false);
  },

  healthStatus: function () {
    var status = this.get('service.healthStatus');
    switch (status) {
      case 'green':
        status = App.Service.Health.live;
        this.stopBlink();
        break;
      case 'green-blinking':
        status = App.Service.Health.live;
        this.startBlink();
        break;
      case 'red-blinking':
        status = App.Service.Health.dead;
        this.startBlink();
        break;
      case 'yellow':
        status = App.Service.Health.unknown;
        break;
      default:
        status = App.Service.Health.dead;
        this.stopBlink();
        break;
    }

    return 'health-status-' + status;
  }.property('service.healthStatus'),

  didInsertElement: function () {
    this.doBlink(); // check for blink availability
  }
});

App.MainDashboardServiceView = Em.View.extend({
  classNames: ['service', 'clearfix'],

  data: function () {
    return this.get('controller.data.' + this.get('serviceName'));
  }.property('controller.data'),

  formatUnavailable: function(value){
    return (value || value == 0) ? value : this.t('services.service.summary.notAvailable');
  },

  criticalAlertsCount: function () {
    var alerts = App.router.get('clusterController.alerts');
    return alerts.filterProperty('serviceType', this.get('service.id')).filterProperty('isOk', false).filterProperty('ignoredForServices', false).length;
  }.property('App.router.clusterController.alerts'),

  isCollapsed: false,

  toggleInfoView: function () {
    this.$('.service-body').toggle('blind', 200);
    this.set('isCollapsed', !this.isCollapsed);
  },

  masters: function(){
    return this.get('service.hostComponents').filterProperty('isMaster', true);
  }.property('service'),

  clients: function(){
    var clients = this.get('service.hostComponents').filterProperty('isClient', true);
    var len = clients.length;
    var template = 'dashboard.services.{0}.client'.format(this.get('serviceName').toLowerCase());
    if(len > 1){
      template += 's';
    }

    return {
      title: this.t(template).format(len),
      component: clients.objectAt(0)
    };
  }.property('service')

});