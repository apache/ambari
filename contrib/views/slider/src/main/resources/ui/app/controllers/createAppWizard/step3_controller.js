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
   * predefined settings of configuration properties
   */
  configSettings: {
    'site.global.metric_collector_host': {
    },
    'site.global.metric_collector_port': {
    },
    'site.global.metric_collector_lib': {
    }
  },

  /**
   * Convert configs to array of unique section names
   * @type {Array}
   */
  sectionKeys: function () {
    var configs = this.get('newAppConfigs') || {},
      k = ["general"];

    Object.keys(configs).forEach(function (key) {
      if (key.split('.')[0] == "site") {
        k.push(key.split('.')[1])
      }
    });
    k.push('custom');
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
   * config that describe configurations set
   */
  configsSet: [
    {
      name: 'ams_metrics',
      trigger: {value: false, label: Em.I18n.t('configs.enable.metrics'), viewType: 'checkbox'},
      isSet: true,
      section: 'global',
      configNames: ["site.global.metric_collector_host", "site.global.metric_collector_port", "site.global.metric_collector_lib"],
      configs: [],
      dependencies: []
    }
  ],

  /**
   * Load all data required for step
   * @method loadStep
   */
  loadStep: function () {
    this.clearStep();
    this.initConfigs(true);
  },

  /**
   * Format init value for <code>configs</code> property
   * @param {bool} setDefaults
   * @method initConfigs
   */
  initConfigs: function (setDefaults) {
    var self = this,
      newAppConfigs = this.get('newAppConfigs') || {},
      defaultConfigs = self.get('appWizardController.newApp.appType.configs');
      configs = Em.A(),
      configsSet = $.extend(true, [], this.get('configsSet')),
      allSetConfigs = {},
      configSettings = this.get('configSettings');

    configsSet.forEach(function (item) {
      item.configNames.forEach(function (configName) {
        allSetConfigs[configName] = item;
        if(!newAppConfigs[configName] && defaultConfigs && defaultConfigs[configName]){
          newAppConfigs[configName] = defaultConfigs[configName];
        }
      });
    });

    Object.keys(newAppConfigs).forEach(function (key) {
      var label = (!!key.match('^site.')) ? key.substr(5) : key;
      var configSetting = (configSettings[key]) ?
        $.extend({name: key, value: newAppConfigs[key], label: label}, configSettings[key]) :
        {name: key, value: newAppConfigs[key], label: label};

      if (key === 'java_home' && !!setDefaults && App.get('javaHome')) {
        configSetting.value = App.get('javaHome');
      }

      if (key === "site.global.metric_collector_host" && !!setDefaults && App.get('metricsHost')) {
        configSetting.value = App.get('metricsHost');
      }

      if (key === "site.global.metric_collector_port" && !!setDefaults && App.get('metricsPort')) {
        configSetting.value = App.get('metricsPort');
      }

      if (key === "site.global.metric_collector_lib" && !!setDefaults && App.get('metricsLibPath')) {
        configSetting.value = App.get('metricsLibPath');
      }

      if (allSetConfigs[key]) {
        allSetConfigs[key].configs.push(App.ConfigProperty.create(configSetting));
      } else {
        configs.push(App.ConfigProperty.create(configSetting));
      }
    });

    configsSet.forEach(function (configSet) {
      if (configSet.configs.length === configSet.configNames.length) {
        delete configSet.configNames;
        configSet.trigger = App.ConfigProperty.create(configSet.trigger);
        this.initConfigSetDependencies(configSet);
        configs.unshift(configSet);
      }
    }, this);

    this.set('configs', configs);
  },

  /**
   * initialize dependencies map for config set by name
   * configSet map changed by reference
   *
   * @param {object} configSet
   * @method initConfigSetDependencies
   */
  initConfigSetDependencies: function (configSet) {
    configSet.dependencies.forEach(function (item) {
      item.map = Em.get(item.name);
    });
  },

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
    var self = this,
      result = true,
      configs = this.addConfigSetProperties(this.get('configs')),
      configsObject = {};

    try {
      configs.forEach(function (item) {
        configsObject[item.name] = item.value;
      });
      self.set('configsObject', configsObject);
    } catch (e) {
      self.set('isError', true);
      result = false;
    }
    return result;
  },

  /**
   * add config properties from config sets to general configs array
   * @param configs
   * @return {Array}
   */
  addConfigSetProperties: function (configs) {
    var newConfigs = [];
    configs.filterBy('isSet').forEach(function (item) {
      if (item.trigger.value) {
        newConfigs.pushObjects(item.configs);
      }
    });
    return configs.filterBy('isSet', false).concat(newConfigs);
  },

  /**
   * Save converted configs to new App configs
   * @method saveConfigs
   */
  saveConfigs: function () {
    var configsToSet = this.get('configsObject');
    if (configsToSet['site.global.metrics_enabled']!=null) {
      if (configsToSet['site.global.metric_collector_host']!=null &&
          configsToSet['site.global.metric_collector_port']!=null) {
        configsToSet['site.global.metrics_enabled'] = "true";
      } else {
        configsToSet['site.global.metrics_enabled'] = "false";
      }
    }
    this.set('newAppConfigs', configsToSet);
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
