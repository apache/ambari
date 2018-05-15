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

import Ember from 'ember';
import constants from 'hive/utils/constants';

export default Ember.Controller.extend({
  jobProgressService: Ember.inject.service(constants.namingConventions.jobProgress),
  openQueries   : Ember.inject.controller(constants.namingConventions.openQueries),
  notifyService: Ember.inject.service(constants.namingConventions.notify),
  index: Ember.inject.controller(),

  tabClassNames : "fa queries-icon query-context-tab",

  tabs: [
    Ember.Object.create({
      iconClass: 'text-icon',
      id: 'query-icon',
      text: 'SQL',
      action: 'showIndexTab',
      name: constants.namingConventions.index,
      tooltip: Ember.I18n.t('tooltips.query'),
    }),
    Ember.Object.create({
      iconClass: 'fa-gear',
      id: 'settings-icon',
      action: 'showSettingsTab',
      template: 'open-queries',
      outlet: 'overlay',
      into: 'open-queries',
      tooltip: Ember.I18n.t('tooltips.settings'),
      onTabOpen:'showSettingTab'
    }),
    Ember.Object.create({
      iconClass: 'fa-area-chart',
      id: 'visualization-icon',
      action: 'showVisualizationRoute',
      tooltip: Ember.I18n.t('tooltips.visualization'),
      into: 'index',
      outlet: 'overlay',
      template: 'visualization-ui',
      onTabOpen: 'onTabOpen'
    }),
    Ember.Object.create({
      iconClass: 'fa-link',
      id: 'visual-explain-icon',
      action: 'showVisualExplainRoute',
      template: 'visual-explain',
      outlet: 'overlay',
      into: 'index',
      onTabOpen: 'onTabOpen',
      tooltip: Ember.I18n.t('tooltips.visualExplain')
    }),
    Ember.Object.create({
      iconClass: 'text-icon',
      id: 'tez-icon',
      text: 'TEZ',
      action: 'showTezRoute',
      template: 'tez-ui',
      outlet: 'overlay',
      into: 'index',
      tooltip: Ember.I18n.t('tooltips.tez')
    }),
    Ember.Object.create({
      iconClass: 'fa-envelope',
      id: 'notifications-icon',
      action: 'showMessagesRoute',
      template: 'messages',
      outlet: 'overlay',
      into: 'index',
      badgeProperty: 'count',
      onTabOpen: 'markMessagesAsSeen',
      tooltip: Ember.I18n.t('tooltips.notifications')
    })
  ],

  init: function() {
    this.setupControllers();
    this.setDefaultTab();
    this.setupTabsBadges();
  },

  setupControllers: function() {
    var tabs = this.get('tabs');
    var self = this;

    tabs.map(function (tab) {
      var controller;

      if (tab.get('template')) {
        controller = self.container.lookup('controller:' + tab.get('template'));
        tab.set('controller', controller);
      }
    });
  },

  setDefaultTab: function () {
    var defaultTab = this.get('tabs.firstObject');

    defaultTab.set('active', true);

    this.set('default', defaultTab);
    this.set('activeTab', defaultTab);
  },

  setupTabsBadges: function () {
    var tabs = this.get('tabs').filterProperty('badgeProperty');

    tabs.map(function (tab) {
        Ember.oneWay(tab, 'badge', 'controller.' + tab.badgeProperty);
    });
  },

  onTabOpen: function (tab) {
    if (!tab.onTabOpen) {
      return;
    }

    var controller = this.container.lookup('controller:' + tab.template);
    controller.send(tab.onTabOpen, controller);
  },

  openOverlay: function (tab) {
    this.set('activeTab.active', false);
    tab.set('active', true);
    this.set('activeTab', tab);

    this.onTabOpen(tab);
    this.controllerFor('application').showQueryMenu();
  },

  setDefaultActive: function () {
    var activeTab = this.get('activeTab');
    var defaultTab = this.get('default');

    if (activeTab !== defaultTab) {
      defaultTab.set('active', true);
      activeTab.set('active', false);
      this.set('activeTab', defaultTab);
      var controller = this.container.lookup('controller:open-queries');
      controller.send('showQueryTab', controller);
    }
  },

  actions: {
    showMessagesRoute:function (tab) {
      this.transitionToRoute('messages');
      this.openOverlay(tab);
    },
    showVisualExplainRoute:function (tab) {
      this.transitionToRoute('visual-explain');
      this.openOverlay(tab);
    },
    showVisualizationRoute:function (tab) {
      this.transitionToRoute('visualization-ui');
      this.openOverlay(tab);
    },
    showTezRoute:function (tab) {
      this.transitionToRoute('tez-ui');
      this.openOverlay(tab);
    },
    showSettingsTab:function (tab) {
      this.transitionToRoute('index');
      this.openOverlay(tab);
    },
    showIndexTab:function (tab) {
      this.setDefaultActive();
      this.transitionToRoute('index');
      this.onTabOpen(tab);
      var controller = this.container.lookup('controller:open-queries');
      controller.send('showQueryTab', controller);
    },
  }
});
