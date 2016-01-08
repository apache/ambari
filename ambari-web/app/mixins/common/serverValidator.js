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
   * @type {boolean} set true if at least one config has error
   */
  configValidationError: false,

  /**
   * @type {boolean} set true if at least one config has warning
   */
  configValidationWarning: false,

  /**
   * @type {boolean} set true if at least one config has warning
   */
  configValidationFailed: false,

  /**
   * @type {object[]} contains additional message about validation errors
   */
  configValidationGlobalMessage: [],

  /**
   * recommendation configs loaded from server
   * (used only during install)
   * @type {Object}
   */
  recommendationsConfigs: null,

  /**
   * array of cluster-env configs
   * used for validation request
   * @type {Array}
   */
  clusterEnvConfigs: [],

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
  }.property('content.recommendationsHostGroups', 'App.allHostNames'),

  /**
   * controller that is child of this mixin has to contain stepConfigs
   * @type {Array}
   */
  stepConfigs: null,

  serverSideValidation: function () {
    var deferred = $.Deferred();
    this.set('configValidationFailed', false);
    this.set('configValidationGlobalMessage', []);
    if (this.get('configValidationFailed')) {
      this.warnUser(deferred);
    } else {
      this.runServerSideValidation(deferred);
    }
    return deferred;
  },

  getAllHostsWithComponents: function() {
    return App.ajax.send({
      sender: this,
      name: 'common.hosts.all',
      data: {
        urlParams: 'fields=HostRoles/component_name,HostRoles/host_name'
      }
    });
  },

  /**
   * @method serverSideValidation
   * send request to validate configs
   * @returns {*}
   */
  runServerSideValidation: function (deferred) {
    var self = this;
    var recommendations = this.get('hostGroups');
    var stepConfigs = this.get('stepConfigs');

    return this.getBlueprintConfigurations().done(function(blueprintConfigurations){
      recommendations.blueprint.configurations = blueprintConfigurations;
      return App.ajax.send({
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
      }).complete(function () {
        self.warnUser(deferred);
      });
    });
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
      this.getClusterEnvConfigsForValidation().done(function(clusterEnvConfigs){
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

  getClusterEnvConfigsForValidation: function () {
    var dfd = $.Deferred();
    App.ajax.send({
      name: 'config.cluster_env_site',
      sender: this,
      error: 'validationError'
    }).done(function (data) {
      App.router.get('configurationController').getConfigsByTags([{
        siteName: data.items[data.items.length - 1].type,
        tagName: data.items[data.items.length - 1].tag
      }]).done(function (clusterEnvConfigs) {
        var configsObject = clusterEnvConfigs[0].properties;
        var configsArray = [];
        for (var property in configsObject) {
          if (configsObject.hasOwnProperty(property)) {
            configsArray.push(Em.Object.create({
              name: property,
              value: configsObject[property],
              filename: 'cluster-env.xml'
            }));
          }
        }
        dfd.resolve(configsArray);
      });
    });
    return dfd.promise();
  },

  /**
   * @method validationSuccess
   * success callback after getting response from server
   * go through the step configs and set warn and error messages
   * @param data
   */
  validationSuccess: function(data) {
    var self = this;
    var checkedProperties = [];
    var globalWarning = [];
    self.set('configValidationError', false);
    self.set('configValidationWarning', false);
    data.resources.forEach(function(r) {
      r.items.forEach(function(item){
        if (item.type == "configuration") {
          self.get('stepConfigs').forEach(function(service) {
            service.get('configs').forEach(function(property) {
              if ((property.get('filename') == item['config-type'] + '.xml') && (property.get('name') == item['config-name'])) {
                if (item.level == "ERROR") {
                  self.set('configValidationError', true);
                  property.set('errorMessage', item.message);
                  property.set('error', true);
                } else if (item.level == "WARN") {
                  self.set('configValidationWarning', true);
                  property.set('warnMessage', item.message);
                  property.set('warn', true);
                }
                // store property data to detect WARN or ERROR messages for missed property
                if (["ERROR", "WARN"].contains(item.level)) checkedProperties.push(item['config-type'] + '/' + item['config-name']);
              }
            });
          });
          // check if error or warn message detected for property that absent in step configs
          if (["ERROR", "WARN"].contains(item.level) && !checkedProperties.contains(item['config-type'] + '/' + item['config-name'])) {
            var message = {
              propertyName: item['config-name'],
              filename: item['config-type'],
              warnMessage: item.message
            };
            if (item['config-type'] === "" && item['config-name'] === "") {
              //service-independent validation
              message.isGeneral = true;
            } else {
              message.serviceName = App.StackService.find().filter(function(service) {
                return !!service.get('configTypes')[item['config-type']];
              })[0].get('displayName')
            }
            self.set(item.level == 'WARN' ? 'configValidationWarning' : 'configValidationError', true);
            globalWarning.push(message);
          }
        }
      });
    });
    self.set('configValidationGlobalMessage', globalWarning);
  },

  validationError: function (jqXHR, ajaxOptions, error, opt) {
    this.set('configValidationFailed', true);
  },


  /**
   * warn user if some errors or warning were
   * in setting up configs otherwise go to the nex operation
   * @param deferred
   * @returns {*}
   */
  warnUser: function(deferred) {
    var self = this;
    if (this.get('configValidationFailed')) {
      return App.ModalPopup.show({
        header: Em.I18n.t('installer.step7.popup.validation.failed.header'),
        primary: Em.I18n.t('common.proceedAnyway'),
        primaryClass: 'btn-danger',
        marginBottom: 200,
        onPrimary: function () {
          this.hide();
          deferred.resolve();
        },
        onSecondary: function () {
          this.hide();
          deferred.reject("invalid_configs"); // message used to differentiate types of rejections.
        },
        onClose: function () {
          this.hide();
          deferred.reject("invalid_configs"); // message used to differentiate types of rejections.
        },
        body: Em.I18n.t('installer.step7.popup.validation.request.failed.body')
      });
    } else if (this.get('configValidationWarning') || this.get('configValidationError')) {
      // Motivation: for server-side validation warnings and EVEN errors allow user to continue wizard
      var stepConfigs = self.get('name') === 'mainServiceInfoConfigsController'
        ? [self.get('selectedService')]
        : self.get('stepConfigs');
      var configsWithErrors = stepConfigs.some(function (step) {
        return step.get('configs').some(function(c) {
          return c.get('isVisible') && !c.get('hiddenBySection') && (c.get('warn') || c.get('error'));
        })
      });
      if (configsWithErrors) {
        return App.ModalPopup.show({
          header: Em. I18n.t('installer.step7.popup.validation.warning.header'),
          classNames: ['sixty-percent-width-modal','modal-full-width'],
          primary: Em.I18n.t('common.proceedAnyway'),
          primaryClass: 'btn-danger',
          marginBottom: 200,
          onPrimary: function () {
            this.hide();
            deferred.resolve();
          },
          onSecondary: function () {
            this.hide();
            deferred.reject("invalid_configs"); // message used to differentiate types of rejections.
          },
          onClose: function () {
            this.hide();
            deferred.reject("invalid_configs"); // message used to differentiate types of rejections.
          },
          bodyClass: Em.View.extend({
            controller: self,
            templateName: require('templates/common/modal_popups/config_recommendation_popup'),
            serviceConfigs: stepConfigs
          })
        });
      } else {
        deferred.resolve();
      }
    } else {
      deferred.resolve();
    }
  }
});
