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
App.MainAdminSecurityController = Em.Controller.extend({
  name: 'mainAdminSecurityController',
  securityEnabledBinding: 'App.router.mainAdminController.securityEnabled',
  isSubmitDisabled: false,
  getAddSecurityWizardStatus: function () {
    return App.db.getSecurityWizardStatus();
  },
  setAddSecurityWizardStatus: function (status) {
    App.db.setSecurityWizardStatus(status);
  },
  notifySecurityOff: false,
  notifySecurityAdd: false,

  notifySecurityOffPopup: function () {
    var self = this;
    if (!this.get('isSubmitDisabled')) {
      App.ModalPopup.show({
        header: Em.I18n.t('admin.security.disable.popup.header'),
        primary: 'OK',
        secondary: null,
        onPrimary: function () {
          App.router.transitionTo('disableSecurity');
          self.set('isSubmitDisabled', true);
          this.hide();
        },
        bodyClass: Ember.View.extend({
          template: Ember.Handlebars.compile('<h5>{{t admin.security.disable.popup.body}}</h5>')
        })
      });
    }
  }

});


