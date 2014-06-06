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

App.WizardStep5View = Em.View.extend({

  templateName: require('templates/wizard/step5'),

  didInsertElement: function () {
    this.get('controller').loadStep();
  }

});

App.SelectHostView = Em.TextField.extend({

  /**
   * Element of <code>controller.servicesMasters</code>
   * Binded from template
   * @type {object}
   */
  component: null,

  /**
   * List of avaiable host names
   * @type {string[]}
   */
  content: [],

  /**
   * Host component name
   * @type {string}
   */
  componentName: null,

  attributeBindings: ['disabled'],

  /**
   * Saved typeahead component
   * @type {$}
   */
  typeahead: null,

  /**
   * Handler for selected value change
   * Triggers <code>changeHandler</code> execution
   * @method change
   */
  change: function () {
    if ('destroyed' === this.get('state')) return;
    this.get('controller').toggleProperty('hostNameCheckTrigger');
  },

  /**
   * Add or remove <code>error</code> class from parent div-element
   * @param {bool} flag true - add class, false - remove
   * @method updateErrorStatus
   */
  updateErrorStatus: function(flag) {
    var parentBlock = this.$().parent('div');
    /* istanbul ignore next */
    if (flag) {
      parentBlock.removeClass('error');
    }
    else {
      parentBlock.addClass('error');
    }
  },

  /**
   * When <code>value</code> (hostname) is changed this method is triggered
   * If new hostname is valid, this host is assigned to master component
   * @method changeHandler
   */
  changeHandler: function() {
    if ('destroyed' === this.get('state')) return;
    var componentIsMultiple = this.get('controller.multipleComponents').contains(this.get("component.component_name"));
    this.get('controller').assignHostToMaster(this.get("component.component_name"), this.get("value"), this.get("component.zId"));
    if(componentIsMultiple) {
      this.get('controller').set('componentToRebalance', this.get("component.component_name"));
      this.get('controller').incrementProperty('rebalanceComponentHostsCounter');
    }
  }.observes('controller.hostNameCheckTrigger'),

  /**
   * If <code>component.isHostNameValid</code> was changed,
   * error status should be updated according to new value
   * @method isHostNameValidObs
   */
  isHostNameValidObs: function() {
    this.updateErrorStatus(this.get('component.isHostNameValid'));
  }.observes('component.isHostNameValid'),

  /**
   * Recalculate available hosts
   * This should be done only once per Ember loop
   * @method rebalanceComponentHosts
   */
  rebalanceComponentHosts: function () {
    Em.run.next(this, 'rebalanceComponentHostsOnce');
  }.observes('controller.rebalanceComponentHostsCounter'),

  /**
   * Recalculate available hosts
   * @method rebalanceComponentHostsOnce
   */
  rebalanceComponentHostsOnce: function() {
    if (this.get('component.component_name') === this.get('controller.componentToRebalance')) {
      this.initContent();
    }
  },

  /**
   * Get available hosts
   * multipleComponents component can be assigned to multiple hosts,
   * shared hosts among the same component should be filtered out
   * @return {string[]}
   * @method getAvailableHosts
   */
  getAvailableHosts: function () {
    var hosts = this.get('controller.hosts').slice(),
      componentName = this.get('component.component_name'),
      multipleComponents = this.get('controller.multipleComponents'),
      occupiedHosts = this.get('controller.selectedServicesMasters')
        .filterProperty('component_name', componentName)
        .mapProperty('selectedHost')
        .without(this.get('component.selectedHost'));

    if (multipleComponents.contains(componentName)) {
      return hosts.filter(function (host) {
        return !occupiedHosts.contains(host.get('host_name'));
      }, this);
    }
    return hosts;
  },

  didInsertElement: function () {
    this.initContent();
    this.set("value", this.get("component.selectedHost"));
    var content = this.get('content').mapProperty('host_name'),
      self = this,
      typeahead = this.$().typeahead({items: 10, source: content});
    typeahead.on('blur', function() {
      self.change();
    }).on('keyup', function(e) {
        self.set('value', $(e.currentTarget).val());
        self.change();
      });
    this.set('typeahead', typeahead);
  },

  /**
   * Update <code>source</code> property of <code>typeahead</code> with a new list of hosts
   * @param {string[]} hosts
   * @method updateTypeaheadData
   */
  updateTypeaheadData: function(hosts) {
    if (this.get('typeahead')) {
      this.get('typeahead').data('typeahead').source = hosts;
    }
  },

  /**
   * Extract hosts from controller,
   * filter out available to selection and
   * push them into Em.Select content
   * @method initContent
   */
  initContent: function () {
    var hosts = this.getAvailableHosts();
    this.set("content", hosts);
    this.updateTypeaheadData(hosts.mapProperty('host_name'));
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
