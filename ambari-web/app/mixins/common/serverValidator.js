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
var blueprintUtils = require('utils/blueprint');

App.ServerValidatorMixin = Em.Mixin.create({

  /**
   * defines if we use validation and recommendation on wizards
   * depend on this flag some properties will be taken from different places
   * @type {boolean}
   */
  isWizard: function() {
    return this.get('wizardController') && ['addServiceController', 'installerController'].contains(this.get('wizardController.name'));
  }.property('wizardController.name'),

  /**
   * recommendation configs loaded from server
   * (used only during install)
   * @type {Object}
   */
  recommendationsConfigs: null,

  /**
   * Collection of all config validation errors
   *
   * @type {Object[]}
   */
  configErrorList: [],

  /**
   * Map with allowed error types
   *
   * @type {Object}
   */
  errorTypes: {
    ERROR: 'ERROR',
    WARN: 'WARN',
    GENERAL: 'GENERAL'
  },

  /**
   * by default loads data from model otherwise must be overridden as computed property
   * refer to \assets\data\stacks\HDP-2.1\recommendations_configs.json to learn structure
   * (shouldn't contain configurations filed)
   * @type {Object}
   */
  hostNames: function() {
    return this.get('isWizard')
        ? Object.keys(this.get('content.hosts'))
        : App.get('allHostNames');
  }.property('isWizard', 'content.hosts', 'App.allHostNames'),

  /**
   * by default loads data from model otherwise must be overridden as computed property
   * @type {Array} - of strings (serviceNames)
   */
  serviceNames: function() {
    // When editing a service we validate only that service's configs.
    // However, we should pass the IDs of services installed, or else,
    // default value calculations will alter.
    return this.get('isWizard') ? this.get('allSelectedServiceNames') : App.Service.find().mapProperty('serviceName');
  }.property('isWizard', 'allSelectedServiceNames'),

  /**
   * by default loads data from model otherwise must be overridden as computed property
   * can be used for service|host configs pages
   * @type {Array} of strings (hostNames)
   */
  hostGroups: function() {
    return this.get('content.recommendationsHostGroups') || blueprintUtils.generateHostGroups(App.get('allHostNames'));
  }.property('content.recommendationsHostGroups', 'App.allHostNames', 'App.componentToBeAdded', 'App.componentToBeDeleted'),

  /**
   * controller that is child of this mixin has to contain stepConfigs
   * @type {Array}
   */
  stepConfigs: null,

  serverSideValidation: function () {
    var deferred = $.Deferred(),
      self = this,
      primary = function() { deferred.resolve(); },
      secondary = function() { deferred.reject('invalid_configs'); };
    this.set('configErrorList', []);

    this.runServerSideValidation().done(function() {
      if (self.get('configErrorList.length')) {
        App.showConfigValidationPopup(self.get('configErrorList'), primary, secondary);
      } else {
        deferred.resolve();
      }
    }).fail(function() {
      App.showConfigValidationFailedPopup(primary, secondary);
    });

    return deferred.promise();
  },

  /**
   * @method serverSideValidation
   * send request to validate configs
   * @returns {*}
   */
  runServerSideValidation: function () {
    var self = this;
    var recommendations = this.get('hostGroups');
    var stepConfigs = this.get('stepConfigs');
    var dfd = $.Deferred();

    this.getBlueprintConfigurations().done(function(blueprintConfigurations) {
      recommendations.blueprint.configurations = blueprintConfigurations;
      App.ajax.send({
        name: 'config.validations',
        sender: self,
        data: {
          stackVersionUrl: App.get('stackVersionURL'),
          hosts: self.get('hostNames'),
          services: self.get('serviceNames'),
          validate: 'configurations',
          recommendations: recommendations
        },
        success: 'validationSuccess',
        error: 'validationError'
      }).done(dfd.resolve).fail(dfd.reject);
    });
    return dfd.promise();
  },

  /**
   * Return JSON for blueprint configurations
   * @returns {*}
   */
  getBlueprintConfigurations: function () {
    var dfd = $.Deferred();
    var stepConfigs = this.get('stepConfigs');

    // check if we have configs from 'cluster-env', if not, then load them, as they are mandatory for validation request
    if (!stepConfigs.findProperty('serviceName', 'MISC')) {
      App.config.getClusterEnvConfigs().done(function(clusterEnvConfigs){
        stepConfigs = stepConfigs.concat(Em.Object.create({
          serviceName: 'MISC',
          configs: clusterEnvConfigs
        }));
        dfd.resolve(blueprintUtils.buildConfigsJSON(stepConfigs));
      });
    } else {
      dfd.resolve(blueprintUtils.buildConfigsJSON(stepConfigs));
    }
    return dfd.promise();
  },

  /**
   * Creates config validation error object
   *
   * @param type - error type, see <code>errorTypes<code>
   * @param property - config property object
   * @param messages - array of messages
   * @returns {{type: String, isError: boolean, isWarn: boolean, isGeneral: boolean, messages: Array}}
   */
  createErrorMessage: function (type, property, messages) {
    var errorTypes = this.get('errorTypes');
    var error = {
      type: type,
      isError: type === errorTypes.ERROR,
      isWarn: type === errorTypes.WARN,
      isGeneral: type === errorTypes.GENERAL,
      messages: Em.makeArray(messages)
    };

    Em.assert('Unknown config error type ' + type, error.isError || error.isWarn || error.isGeneral);
    if (property) {
      error.id = Em.get(property, 'id');
      error.serviceName = Em.get(property, 'serviceDisplayName') || App.StackService.find(Em.get(property, 'serviceName')).get('displayName');
      error.propertyName = Em.get(property, 'name');
      error.filename = Em.get(property, 'filename');
      error.value = Em.get(property, 'value');
      error.description = Em.get(property, 'description');
    }
    return error;
  },


  /**
   * Parse data from server to
   *  <code>configErrorsMap<code> and
   *  <code>generalErrors<code>
   *
   * @param data
   * @returns {{configErrorsMap: {}, generalErrors: Array}}
   */
  parseValidation: function(data) {
    var configErrorsMap = {},  generalErrors = [];

    data.resources.forEach(function(r) {
      r.items.forEach(function(item){
        if (item.type == "configuration") {
          var configId = (item['config-name'] && item['config-type']) && App.config.configId(item['config-name'], item['config-type']);
          if (configId) {
            if (configErrorsMap[configId]) {
              configErrorsMap[configId].messages.push(item.message);
            } else {
              configErrorsMap[configId] = {
                type: item.level,
                messages: [item.message],
                name: item['config-name'],
                filename: item['config-type']
              }
            }
          } else {
            generalErrors.push({
              type: this.get('errorTypes').GENERAL,
              messages: [item.message]
            });
          }
        }
      }, this);
    }, this);

    return {
      configErrorsMap: configErrorsMap,
      generalErrors: generalErrors
    }
  },

  /**
   * Generates list of all config errors that should be displayed in popup
   *
   * @param configErrorsMap
   * @param generalErrors
   * @returns {Array}
   */
  collectAllIssues: function(configErrorsMap, generalErrors)  {
    var errorTypes = this.get('errorTypes');
    var configErrorList = [];

    this.get('stepConfigs').forEach(function(service) {
      service.get('configs').forEach(function(property) {
        if (property.get('isVisible') && !property.get('hiddenBySection')) {
          var serverIssue = configErrorsMap[property.get('id')];
          if (serverIssue) {
            configErrorList.push(this.createErrorMessage(serverIssue.type, property, serverIssue.messages));
          } else if (property.get('warnMessage')) {
            configErrorList.push(this.createErrorMessage(errorTypes.WARN, property, [property.get('warnMessage')]));
          }
        }
      }, this);
    }, this);

    generalErrors.forEach(function(serverIssue) {
      configErrorList.push(this.createErrorMessage(errorTypes.GENERAL, null, serverIssue.messages));
    }, this);

    Em.keys(configErrorsMap).forEach(function (id) {
      if (!configErrorList.someProperty('id', id)) {
        var serverIssue = configErrorsMap[id],
          filename = Em.get(serverIssue, 'filename'),
          service = App.config.get('serviceByConfigTypeMap')[filename],
          property = {
            id: id,
            name: Em.get(serverIssue, 'name'),
            filename: App.config.getOriginalFileName(filename),
            serviceDisplayName: service && Em.get(service, 'displayName')
          };
        configErrorList.push(this.createErrorMessage(serverIssue.type, property, serverIssue.messages));
      }
    }, this);

    return configErrorList;
  },

  /**
   * @method validationSuccess
   * success callback after getting response from server
   * go through the step configs and set warn and error messages
   * @param data
   */
  validationSuccess: function(data) {
    var parsed = this.parseValidation(data);
    this.set('configErrorList', this.collectAllIssues(parsed.configErrorsMap, parsed.generalErrors));
  },

  validationError: Em.K
});
