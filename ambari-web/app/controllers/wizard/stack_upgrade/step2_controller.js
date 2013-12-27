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

App.StackUpgradeStep2Controller = Em.Controller.extend({
  name: 'stackUpgradeStep2Controller',
  /**
   * check whether all services are running
   * @return {Array}
   */
  isAllServicesRunning: function(){
    var masterComponents = App.HostComponent.find().filterProperty('isMaster', true);
    return masterComponents.everyProperty('workStatus', 'STARTED');
  },
  /**
   * callback, which save step data and route to next step
   * @param event
   */
  upgradeAction: function(event){
    if(this.isAllServicesRunning()){
      App.router.send('next');
    } else {
      this.showWarningPopup();
    }
  },
  /**
   * show warning popup if not all services are running
   */
  showWarningPopup: function(){
    App.ModalPopup.show({
      header: Em.I18n.t('installer.step2.manualInstall.popup.header'),
      secondary: null,
      bodyClass: Ember.View.extend({
        template: Em.Handlebars.compile('{{t installer.stackUpgrade.step2.popup.body}}')
      })
    });
  }
});
