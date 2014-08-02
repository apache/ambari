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
  installHostComponent: function(hostName, component) {
    var self = this;
    var componentName = component.get('componentName');
    var displayName = component.get('displayName');
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
  }
};