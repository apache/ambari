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

App.MainDashboardServiceHealthView = Em.View.extend({
  classNameBindings: ["healthStatus", "healthStatusClass"],
  //template: Em.Handlebars.compile(""),
  blink: false,
  tagName: 'span',
  attributeBindings:['rel', 'title','data-original-title'],
  rel: 'HealthTooltip',
  'data-original-title': '',

  updateToolTip: function () {
    this.set('data-original-title', this.get('service.toolTipContent'));
  }.observes('service.toolTipContent'),

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
    var isClientOnlyService = App.get('services.clientOnly').contains(this.get('service.serviceName'));
    if (isClientOnlyService) {
      return 'icon-laptop';
    }
    if (this.get('service.passiveState') != 'OFF') {
      return 'icon-medkit';
    }
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
  }.property('service.healthStatus','service.passiveState','service.serviceName'),

  healthStatusClass: function () {
    if (this.get('service.passiveState') != 'OFF' || App.get('services.clientOnly').contains(this.get('service.serviceName')))
      return '';
    switch (this.get('service.healthStatus')) {
      case 'green':
      case 'green-blinking':
        return App.healthIconClassGreen;
        break;
      case 'red':
      case 'red-blinking':
        return App.healthIconClassRed;
        break;
      case 'yellow':
        return App.healthIconClassYellow;
      default:
        return '';
    }
  }.property('service.healthStatus','service.passiveState','service.serviceName'),

  didInsertElement: function () {
    this.updateToolTip();
    App.tooltip($("[rel='HealthTooltip']"));
    this.doBlink(); // check for blink availability
  }
});

App.ComponentLiveTextView =  Em.View.extend({
  classNameBindings: ['color:service-summary-component-red-dead:service-summary-component-green-live'],
  liveComponents: null,
  totalComponents: null,
  color: function() {
    return this.get("liveComponents") === 0 && this.get('totalComponents') !== 0;
  }.property("liveComponents", 'totalComponents')
});

App.MainDashboardServiceViewWrapper = Em.Mixin.create({
  layoutName: require('templates/main/service/service'),
  isFullWidth: false
});

App.MainDashboardServiceView = Em.View.extend(App.MainDashboardServiceViewWrapper, {
  classNames: ['service', 'clearfix'],

  data: function () {
    return this.get('controller.data.' + this.get('serviceName'));
  }.property('controller.data'),

  dashboardMasterComponentView: Em.View.extend({
    didInsertElement: function() {
      App.tooltip($('[rel=SummaryComponentHealthTooltip]'));
    },
    templateName: require('templates/main/service/info/summary/master_components'),
    mastersComp: function () {
      return this.get('parentView.parentView.mastersObj');
    }.property("parentView.parentView.mastersObj"),
    willDestroyElement: function() {
      $('[rel=SummaryComponentHealthTooltip]').tooltip('destroy');
    }
  }),

  formatUnavailable: function(value){
    return (value || value == 0) ? value : this.t('services.service.summary.notAvailable');
  },

  alertsCount: function () {
    return this.get('service.alertsCount');
  }.property('service.alertsCount'),

  hasCriticalAlerts: function () {
    return this.get('service.hasCriticalAlerts');
  }.property('service.hasCriticalAlerts'),

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
