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

  templateName: require('templates/wizard/step5'),

  didInsertElement: function () {
    this.get('controller').loadStep();
  }

});

App.SelectHostView = Em.Select.extend({

  /**
   * List of avaiable host names
   * @type {string[]}
   */
  content: [],

  /**
   * Index for multiple component (like ZOOKEEPER_SERVER)
   * @type {number|null}
   */
  zId: null,

  /**
   * Selected host name for host component
   * @type {string}
   */
  selectedHost: null,

  /**
   * Host component name
   * @type {string}
   */
  componentName: null,

  attributeBindings: ['disabled'],

  /**
   * Is data loaded
   * @type {bool}
   */
  isLoaded: false,

  /**
   * Is lazy loading used
   * @type {bool}
   */
  isLazyLoading: false,

  /**
   * Handler for selected value change
   * @method change
   */
  change: function () {
    this.get('controller').assignHostToMaster(this.get("componentName"), this.get("value"), this.get("zId"));
    this.set('selectedHost', this.get('value'));
    this.get('controller').set('componentToRebalance', this.get("componentName"));
    this.get('controller').incrementProperty('rebalanceComponentHostsCounter');
  },

  /**
   * Recalculate available hosts
   * @method rebalanceComponentHosts
   */
  rebalanceComponentHosts: function () {
    if (this.get('componentName') === this.get('controller.componentToRebalance')) {
      this.get('content').clear();
      this.set('isLoaded', false);
      this.initContent();
    }
  }.observes('controller.rebalanceComponentHostsCounter'),

  /**
   * Get available hosts
   * multipleComponents component can be assigned to multiple hosts,
   * shared hosts among the same component should be filtered out
   * @return {string[]}
   * @method getAvailableHosts
   */
  getAvailableHosts: function () {
    var hosts = this.get('controller.hosts').slice(),
      componentName = this.get('componentName'),
      multipleComponents = this.get('controller.multipleComponents'),
      occupiedHosts = this.get('controller.selectedServicesMasters')
        .filterProperty('component_name', componentName)
        .mapProperty('selectedHost')
        .without(this.get('selectedHost'));

    if (multipleComponents.contains(componentName)) {
      return hosts.filter(function (host) {
        return !occupiedHosts.contains(host.get('host_name'));
      }, this);
    }
    return hosts;
  },

  /**
   * On click start lazy loading
   * @method click
   */
  click: function () {
    var source = [];
    var availableHosts = this.getAvailableHosts();

    if (!this.get('isLoaded') && this.get('isLazyLoading')) {
      //filter out hosts, which already pushed in select
      source = availableHosts.filter(function (_host) {
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
   * Extract hosts from controller,
   * filter out available to selection and
   * push them into Em.Select content
   * @method initContent
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
    }
    else {
      this.set("content", hosts);
    }
  }
});

App.AddControlView = Em.View.extend({

  /**
   * Current component name
   * @type {string}
   */
  componentName: null,

  tagName: "span",

  classNames: ["badge", "badge-important"],

  template: Em.Handlebars.compile('+'),

  /**
   * Onclick handler
   * Add selected component
   * @method click
   */
  click: function () {
    this.get('controller').addComponent(this.get('componentName'));
  }
});

App.RemoveControlView = Em.View.extend({

  /**
   * Index for multiple component
   * @type {number}
   */
  zId: null,

  /**
   * Current component name
   * @type {string}
   */
  componentName: null,

  tagName: "span",

  classNames: ["badge", "badge-important"],

  template: Em.Handlebars.compile('-'),

  /**
   * Onclick handler
   * Remove current component
   * @method click
   */
  click: function () {
    this.get('controller').removeComponent(this.get('componentName'), this.get("zId"));
  }
});
