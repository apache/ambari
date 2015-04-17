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

export default Ember.Component.extend({
  tabClassNames : "fa queries-icon query-context-tab",
  openOverlayAction   : 'openOverlay',
  closeOverlayAction  : 'closeOverlay',

  tabs: [
    Ember.Object.create({
      iconClass: 'fa-code',
      action: 'setDefaultActive'
    }),
    Ember.Object.create({
      iconClass: 'fa-gear',
      action: 'toggleOverlay',
      template: 'settings',
      outlet: 'overlay',
      into: 'open-queries'
    }),
    Ember.Object.create({
      iconClass: 'fa-bar-chart',
      action: 'toggleOverlay',
      template: 'visual-explain',
      outlet: 'overlay',
      into: 'index'
    }),
    Ember.Object.create({
      iconClass: 'text-icon',
      text: 'TEZ',
      action: 'toggleOverlay',
      template: 'tez-ui',
      outlet: 'overlay',
      into: 'index'
    }),
    Ember.Object.create({
      iconClass: 'fa-envelope',
      action: 'toggleOverlay',
      template: 'messages',
      outlet: 'overlay',
      into: 'open-queries',
      badgeProperty: 'count'
    })
  ],

  setDefaultTab: function() {
    var defaultTab = this.get('tabs.firstObject');

    defaultTab.set('active', true);
    this.set('default', defaultTab);
    this.set('active', defaultTab);
  }.on('init'),

  setupTabsBadges: function() {
    var tabs = this.get('tabs');
    var self = this;

    tabs.map(function(tab) {
      if (tab.get('badgeProperty')) {
        var controller = self.container.lookup('controller:' + tab.get('template'));
        tab.set('controller', controller);

        Ember.oneWay(tab, 'badge', 'controller.count');
      }
    });
  }.on('init'),

  closeActiveOverlay: function() {
    this.sendAction('closeOverlayAction', this.get('active'));
  },

  openOverlay: function(tab) {
    this.closeActiveOverlay();
    this.set('active.active', false);
    tab.set('active', true);
    this.set('active', tab);
    this.sendAction('openOverlayAction', tab);
  },

  setDefaultActive: function() {
    var active     = this.get('active');
    var defaultTab = this.get('default');

    if (active !== defaultTab) {
      this.closeActiveOverlay();
      defaultTab.set('active', true);
      active.set('active', false);
      this.set('active', defaultTab);
    }
  },

  actions: {
    toggleOverlay: function(tab) {
      if (tab !== this.get('default') && tab.get('active')) {
        this.setDefaultActive();
      } else {
        this.openOverlay(tab);
      }
    },

    setDefaultActive: function() {
      this.setDefaultActive();
    }
  }
});
