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

App.CreateAppWizardStep3Controller = Ember.ObjectController.extend({

  needs: "createAppWizard",

  appWizardController: Ember.computed.alias("controllers.createAppWizard"),

  newAppConfigs: Ember.computed.alias("appWizardController.newApp.configs"),

  /**
   * Configs entered in TextFields
   * @type Array
   */
  configs: Em.A(),

  /**
   * Convert configs to array of uniq section names
   * @return {Array}
   */
  sectionKeys:function () {
    var configs = this.get('newAppConfigs') || {},
        k = ["general"];

    Object.keys(configs).forEach(function (key) {
      if (key.split('.')[0] == "site") {
        k.push(key.split('.')[1])
      }
    });

    return k.uniq();
  }.property('newAppConfigs'),

  /**
   * Defines if <code>configs</code> are properly key-value formatted
   * @type {Boolean}
   */
  isError: false,

  /**
   * Config object converted from <code>configs</code>
   * @type {Object}
   */
  configsObject: {},

  /**
   * Load all data required for step
   * @method loadStep
   */
  loadStep: function () {
    this.clearStep();
    this.initConfigs();
  },

  /**
   * Format init value for <code>configs</code> property
   * @method initConfigs
   */
  initConfigs: function() {
    var configs = this.get('newAppConfigs') || {},
        c = Em.A();

    Object.keys(configs).forEach(function (key) {
      var label = (!!key.match('^site.'))?key.substr(5):key;
      c.push({name:key,value:configs[key],label:label})
    });

    this.set('configs', c);
  }.observes('newAppConfigs'),

  /**
   * Clear all initial data
   * @method clearStep
   */
  clearStep: function () {
    this.set('isError', false);
  },

  /**
   * Validate <code>configs</code> to be key-value formatted amd convert it to object
   * @return {Boolean}
   * @method validateConfigs
   */
  validateConfigs: function () {
    var self = this;
    var result = true;
    var configs = this.get('configs');
    var configsObject = {};

    try {
      configs.forEach(function (item) {
        configsObject[item.name] = item.value;
      })
      self.set('configsObject', configsObject);
    } catch (e) {
      self.set('isError', true);
      result = false;
    }
    return result;
  },

  /**
   * Save converted configs to new App configs
   * @method saveConfigs
   */
  saveConfigs: function () {
    this.set('newAppConfigs', this.get('configsObject'));
  },

  actions: {
    /**
     * If <code>configs</code> is valid, than save it and proceed to the next step
     */
    submit: function () {
      if (this.validateConfigs()) {
        this.saveConfigs();
        this.get('appWizardController').nextStep();
      }
    }
  }
});
