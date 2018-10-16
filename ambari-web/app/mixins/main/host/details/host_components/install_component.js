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

App.InstallComponent = Em.Mixin.create({

  installHostComponentCall: function (hostName, component) {
    const self = this;
    const dfd = $.Deferred();
    
    this.updateAndCreateServiceComponent(component).done(function () {
      return App.ajax.send({
        name: 'host.host_component.add_new_component',
        sender: self,
        data: {
          hostName: hostName,
          component: component,
          data: JSON.stringify({
            RequestInfo: {
              "context": Em.I18n.t('requestInfo.installHostComponent') + " " + component.get('displayName')
            },
            Body: {
              host_components: [
                {
                  HostRoles: {
                    component_name: component.get('componentName'),
                    service_name: component.get('serviceName'),
                    service_group_name: component.get('serviceGroupName')
                  }
                }
              ]
            }
          })
        },
        success: 'addNewComponentSuccessCallback',
        error: 'ajaxErrorCallback'
      }).then(dfd.resolve, dfd.reject);
    });

    return dfd.promise();
  },

  /**
   * Success callback for add host component request
   * @param {object} data
   * @param {object} opt
   * @param {object} params
   * @return {$.ajax}
   * @method addNewComponentSuccessCallback
   */
  addNewComponentSuccessCallback: function (data, opt, params) {
    return App.ajax.send({
      name: 'common.host.host_component.update',
      sender: App.router.get('mainHostDetailsController'),
      data: {
        hostName: params.hostName,
        componentId: params.component.get('componentId'),
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
    App.ajax.defaultErrorHandler(request, opt.url, opt.type);
  },

  /**
   *
   * @param componentName
   * @returns {*}
   */
  updateAndCreateServiceComponent: function (component) {
    var self = this;
    var dfd = $.Deferred();
    var updater = App.router.get('updateController');
    updater.updateComponentsState(function () {
      updater.updateServiceMetric(function () {
        self.createServiceComponent(component, dfd);
      });
    });
    return dfd.promise();
  },

  /**
   *
   * @param {object} component
   * @param {$.Deferred} dfd
   * @returns {$.ajax|null}
   */
  createServiceComponent: function (component, dfd) {
    let componentExists = false;

    App.Service.find().forEach(service => {
      if (service.get('serviceName') === component.get('serviceName')
        && service.get('serviceGroupName') === component.get('serviceGroupName')
        && service.get('serviceComponents').contains(component.get('componentName'))) {
        componentExists = true;
      }
    });

    if (componentExists) {
      dfd.resolve();
      return null;
    } else {
      return App.ajax.send({
        name: 'common.create_component',
        sender: this,
        data: {
          componentName: component.get('componentName'),
          serviceName: component.get('serviceName'),
          serviceGroupName: component.get('serviceGroupName') //TODO: service group hard coded for now
        }
      }).complete(function () {
        dfd.resolve();
      });
    }
  }

});