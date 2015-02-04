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

App.MainAdminStackServicesView = Em.View.extend({
  templateName: require('templates/main/admin/stack_upgrade/services'),

  /**
   * @type {Array}
   */
  services: function() {
    var services = App.supports.installGanglia ? App.StackService.find() : App.StackService.find().without(App.StackService.find('GANGLIA'));
    return services.map(function(s) {
      s.set('isInstalled', App.Service.find().someProperty('serviceName', s.get('serviceName')));
      return s;
    });
  }.property('App.router.clusterController.isLoaded'),

  /**
   * launch Add Service wizard
   * @param event
   */
  goToAddService: function (event) {
    if (event.context == "KERBEROS") {
      App.router.get('mainAdminKerberosController').checkAndStartKerberosWizard();
    } else {
      App.router.get('addServiceController').set('serviceToInstall', event.context);
      App.get('router').transitionTo('main.serviceAdd');
    }
  }
});
