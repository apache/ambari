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

App.KerberosWizardStep3Controller = App.KerberosProgressPageController.extend({
  name: 'kerberosWizardStep3Controller',
  clusterDeployState: 'KERBEROS_DEPLOY',
  serviceName: 'KERBEROS',
  componentName: 'KERBEROS_CLIENT',
  ignore: undefined,

  commands: ['installKerberos', 'testKerberos'],

  loadStep: function () {
    this._super();
    this.enableDisablePreviousSteps();
  },

  installKerberos: function() {
    var self = this;
    this.getKerberosClientState().done(function(data) {
      if (data.ServiceComponentInfo.state === 'INIT') {
        App.ajax.send({
          name: 'common.services.update',
          sender: self,
          data: {
            context: Em.I18n.t('requestInfo.kerberosService'),
            ServiceInfo: {"state": "INSTALLED"},
            urlParams: "ServiceInfo/state=INSTALLED&ServiceInfo/service_name=KERBEROS"
          },
          success: 'startPolling',
          error: 'onTaskError'
        });
      } else {
        var hostNames = App.get('allHostNames');
        self.updateComponent('KERBEROS_CLIENT', hostNames, "KERBEROS", "Install");
      }
    });
  },

  getKerberosClientState: function() {
    return App.ajax.send({
      name: 'common.service_component.info',
      sender: this,
      data: {
        serviceName: this.serviceName,
        componentName: this.componentName,
        urlParams: "fields=ServiceComponentInfo/state"
      }
    });
  },

  testKerberos: function() {
    var self = this;
    App.ajax.send({
      'name': 'service.item.smoke',
      'sender': this,
      'success': 'startPolling',
      'error': 'onTestKerberosError',
      'kdcCancelHandler': function() {
        App.router.get(self.get('content.controllerName')).setStepsEnable();
        self.get('tasks').objectAt(self.get('currentTaskId')).set('status', 'FAILED');
      },
      'data': {
        'serviceName': this.serviceName,
        'displayName': App.format.role(this.serviceName, true),
        'actionName': this.serviceName + '_SERVICE_CHECK',
        'operationLevel': {
          "level": "CLUSTER",
          "cluster_name": App.get('clusterName')
        }
      }
    });
  },

  onTestKerberosError: function (jqXHR, ajaxOptions, error, opt) {
    App.ajax.defaultErrorHandler(jqXHR, opt.url, opt.type, jqXHR.status);
    this.onTaskError(jqXHR, ajaxOptions, error, opt);
  },

  /**
   * Enable or disable previous steps according to tasks statuses
   */
  enableDisablePreviousSteps: function () {
    var wizardController = App.router.get(this.get('content.controllerName'));
    if (this.get('tasks').someProperty('status', 'FAILED')) {
      wizardController.setStepsEnable();
    } else {
      wizardController.setLowerStepsDisable(3);
    }
  }.observes('tasks.@each.status'),

  /**
   * Show or hide warning to ignore errors and continue with the install
   */
  showIgnore: function() {
    return this.get('tasks').someProperty('showRetry', true);
  }.property('tasks.@each.showRetry'),

  /**
   * Enable or disable next button if ignore checkbox ticked
   */
  ignoreAndProceed: function() {
    if (this.get('showIgnore')) {
      this.set('isSubmitDisabled', !this.get('ignore'));
    }
  }.observes('ignore','showIgnore')
});

