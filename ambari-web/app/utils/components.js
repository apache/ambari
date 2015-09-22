/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');

module.exports = {
  installHostComponent: function (hostName, component) {
    var self = this,
      componentName = component.get('componentName'),
      displayName = component.get('displayName');
    this.updateAndCreateServiceComponent(componentName).done(function () {
      App.ajax.send({
        name: 'host.host_component.add_new_component',
        sender: self,
        data: {
          hostName: hostName,
          component: component,
          data: JSON.stringify({
            RequestInfo: {
              "context": Em.I18n.t('requestInfo.installHostComponent') + " " + displayName
            },
            Body: {
              host_components: [
                {
                  HostRoles: {
                    component_name: componentName
                  }
                }
              ]
            }
          })
        },
        success: 'addNewComponentSuccessCallback',
        error: 'ajaxErrorCallback'
      });
    });
  },

  /**
   * Success callback for add host component request
   * @param {object} data
   * @param {object} opt
   * @param {object} params
   * @method addNewComponentSuccessCallback
   */
  addNewComponentSuccessCallback: function (data, opt, params) {
    console.log('Send request for ADDING NEW COMPONENT successfully');
    App.ajax.send({
      name: 'common.host.host_component.update',
      sender: App.router.get('mainHostDetailsController'),
      data: {
        hostName: params.hostName,
        componentName: params.component.get('componentName'),
        serviceName: params.component.get('serviceName'),
        component: params.component,
        "context": Em.I18n.t('requestInfo.installNewHostComponent') + " " + params.component.get('displayName'),
        HostRoles: {
          state: 'INSTALLED'
        },
        urlParams: "HostRoles/state=INIT"
      },
      success: 'installNewComponentSuccessCallback',
      error: 'ajaxErrorCallback'
    });
  },

  /**
   * Default error-callback for ajax-requests in current page
   * @param {object} request
   * @param {object} ajaxOptions
   * @param {string} error
   * @param {object} opt
   * @param {object} params
   * @method ajaxErrorCallback
   */
  ajaxErrorCallback: function (request, ajaxOptions, error, opt, params) {
    console.log('error on change component host status');
    App.ajax.defaultErrorHandler(request, opt.url, opt.method);
  },

  downloadClientConfigs: function (data) {
    var isForHost = typeof data.hostName !== 'undefined';
    var url = App.get('apiPrefix') + '/clusters/' + App.router.getClusterName() + '/' +
      (isForHost ? 'hosts/' + data.hostName + '/host_components/' : 'services/' + data.serviceName + '/components/') +
      data.componentName + '?format=client_config_tar';
    try {
      var self = this;
      $.fileDownload(url).fail(function (error) {
        var errorMessage = '';
        var isNoConfigs = false;
        if (error && $(error).text()) {
          var errorObj = JSON.parse($(error).text());
          if (errorObj && errorObj.message && errorObj.status) {
            isNoConfigs = errorObj.message.indexOf(Em.I18n.t('services.service.actions.downloadClientConfigs.fail.noConfigFile')) !== -1;
            errorMessage += isNoConfigs ? Em.I18n.t('services.service.actions.downloadClientConfigs.fail.noConfigFile') :
              Em.I18n.t('services.service.actions.downloadClientConfigs.fail.popup.body.errorMessage').format(data.displayName, errorObj.status, errorObj.message);
          } else {
            errorMessage += Em.I18n.t('services.service.actions.downloadClientConfigs.fail.popup.body.noErrorMessage').format(data.displayName);
          }
          errorMessage += isNoConfigs ? '' : Em.I18n.t('services.service.actions.downloadClientConfigs.fail.popup.body.question');
        } else {
          errorMessage += Em.I18n.t('services.service.actions.downloadClientConfigs.fail.popup.body.noErrorMessage').format(data.displayName) +
            Em.I18n.t('services.service.actions.downloadClientConfigs.fail.popup.body.question');
        }
        App.ModalPopup.show({
          header: Em.I18n.t('services.service.actions.downloadClientConfigs.fail.popup.header').format(data.displayName),
          bodyClass: Ember.View.extend({
            template: Em.Handlebars.compile(errorMessage)
          }),
          secondary: isNoConfigs ? false : Em.I18n.t('common.cancel'),
          onPrimary: function () {
            this.hide();
            if (!isNoConfigs) {
              self.downloadClientConfigs({
                context: Em.Object.create(data)
              })
            }
          }
        });
      });
    } catch (err) {
      var newWindow = window.open(url);
      newWindow.focus();
    }
  },
  /**
   * Check if all required components are installed on host.
   * Available options:
   *  scope: 'host' - dependency level `host`,`cluster` or `*`.
   *  hostName: 'example.com' - host name to search installed components
   *  installedComponents: ['A', 'B'] - names of installed components
   *
   * By default scope level is `*`
   * For host level dependency you should specify at least `hostName` or `installedComponents` attribute.
   *
   * @param {String} componentName
   * @param {Object} opt - options. Allowed options are `hostName`, `installedComponents`, `scope`.
   * @return {Array} - names of missed components
   */
  checkComponentDependencies: function (componentName, opt) {
    opt = opt || {};
    opt.scope = opt.scope || '*';
    var installedComponents;
    var dependencies = App.StackServiceComponent.find(componentName).get('dependencies');
    dependencies = opt.scope === '*' ? dependencies : dependencies.filterProperty('scope', opt.scope);
    if (dependencies.length == 0) return [];
    switch (opt.scope) {
      case 'host':
        Em.assert("You should pass at least `hostName` or `installedComponents` to options.", opt.hostName || opt.installedComponents);
        installedComponents = opt.installedComponents || App.HostComponent.find().filterProperty('hostName', opt.hostName).mapProperty('componentName').uniq();
        break;
      default:
        // @todo: use more appropriate value regarding installed components
        installedComponents = opt.installedComponents || App.HostComponent.find().mapProperty('componentName').uniq();
        break;
    }
    return dependencies.filter(function (dependency) {
      return !installedComponents.contains(dependency.componentName);
    }).mapProperty('componentName');
  },

  /**
   *
   * @param componentName
   * @returns {*}
   */
  updateAndCreateServiceComponent: function (componentName) {
    var self = this;
    var dfd = $.Deferred();
    var updater =  App.router.get('updateController');
    updater.updateComponentsState(function () {
      updater.updateServiceMetric(function () {
        self.createServiceComponent(componentName, dfd);
      });
    });
    return dfd.promise();
  },

  /**
   *
   * @param componentName
   * @param dfd
   * @returns {*}
   */
  createServiceComponent: function (componentName, dfd) {
    var allServiceComponents = [];
    var services = App.Service.find().mapProperty('serviceName');
    services.forEach(function (_service) {
      var _serviceComponents = App.Service.find(_service).get('serviceComponents');
      allServiceComponents = allServiceComponents.concat(_serviceComponents);
    }, this);
    if (allServiceComponents.contains(componentName)) {
      dfd.resolve();
    } else {
      App.ajax.send({
        name: 'common.create_component',
        sender: this,
        data: {
          componentName: componentName,
          serviceName: App.StackServiceComponent.find().findProperty('componentName', componentName).get('serviceName')
        }
      }).complete(function () {
        dfd.resolve();
      });
    }
  }
};