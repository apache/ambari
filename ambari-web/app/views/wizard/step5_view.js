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
var lazyloading = require('utils/lazy_loading');

App.WizardStep5View = Em.View.extend({

  templateName:require('templates/wizard/step5'),

  didInsertElement:function () {
    this.get('controller').loadStep();
  },

  body: Em.I18n.t('installer.step5.body')

});

App.SelectHostView = Em.Select.extend({
  content:[],
  zId:null,
  selectedHost:null,
  componentName:null,
  attributeBindings:['disabled'],
  isLoaded: false,
  isLazyLoading: false,

  change: function () {
    this.get('controller').assignHostToMaster(this.get("componentName"), this.get("value"), this.get("zId"));
    this.set('selectedHost', this.get('value'));
    this.get('controller').set('componentToRebalance', this.get("componentName"));
    this.get('controller').incrementProperty('rebalanceComponentHostsCounter');
  },

  /**
   * recalculate available hosts
   */
  rebalanceComponentHosts: function () {
    if (this.get('componentName') === this.get('controller.componentToRebalance')) {
      this.get('content').clear();
      this.set('isLoaded', false);
      this.initContent();
    }
  }.observes('controller.rebalanceComponentHostsCounter'),

  /**
   * get available hosts
   * @multipleComponents component can be assigned to multiple hosts,
   * shared hosts among the same component should be filtered out
   * @return {*}
   */
  getAvailableHosts: function () {
    var hosts = this.get('controller.hosts').slice();
    var componentName = this.get('componentName');
    var multipleComponents = this.get('controller.multipleComponents');
    var occupiedHosts = this.get('controller.selectedServicesMasters')
      .filterProperty('component_name', componentName)
      .mapProperty('selectedHost').without(this.get('selectedHost'));
    if (multipleComponents.contains(componentName)) {
      return hosts.filter(function (host) {
        return !occupiedHosts.contains(host.get('host_name'));
      }, this);
    }
    return hosts;
  },
  /**
   * on click start lazy loading
   */
  click: function () {
    var source = [];
    var componentName = this.get('componentName');
    var availableHosts = this.getAvailableHosts();
    var selectedHost = this.get('selectedHost');

    if (!this.get('isLoaded') && this.get('isLazyLoading')) {
      //filter out hosts, which already pushed in select
      source = availableHosts.filter(function(_host){
        return !this.get('content').someProperty('host_name', _host.host_name);
      }, this).slice();
      lazyloading.run({
        destination: this.get('content'),
        source: source,
        context: this,
        initSize: 30,
        chunkSize: 200,
        delay: 50
      });
    }
  },

  didInsertElement: function () {
    //The lazy loading for select elements supported only by Firefox and Chrome
    var isBrowserSupported = $.browser.mozilla || ($.browser.safari && navigator.userAgent.indexOf('Chrome') !== -1);
    var isLazyLoading = isBrowserSupported && this.get('controller.hosts').length > 100;
    this.set('isLazyLoading', isLazyLoading);
    this.initContent();
    this.set("value", this.get("selectedHost"));
  },
  /**
   * extract hosts from controller,
   * filter out available to selection and
   * push them into Em.Select content
   */
  initContent: function () {
    var hosts = this.getAvailableHosts();
    if (this.get('isLazyLoading')) {
      //select need at least 30 hosts to have scrollbar
      var initialHosts = hosts.slice(0, 30);
      if (!initialHosts.someProperty('host_name', this.get('selectedHost'))) {
        initialHosts.unshift(hosts.findProperty('host_name', this.get('selectedHost')));
      }
      this.set("content", initialHosts);
    } else {
      this.set("content", hosts);
    }
  }
});

App.AddControlView = Em.View.extend({
  componentName:null,
  tagName:"span",
  classNames:["badge", "badge-important"],
  template:Ember.Handlebars.compile('+'),

  click:function () {
    this.get('controller').addComponent(this.get('componentName'));
  }
});

App.RemoveControlView = Em.View.extend({
  zId:null,
  componentName:null,
  tagName:"span",
  classNames:["badge", "badge-important"],
  template:Ember.Handlebars.compile('-'),

  click:function () {
    this.get('controller').removeComponent(this.get('componentName'), this.get("zId"));
  }
});
