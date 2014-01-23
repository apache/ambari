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

App.MainAdminAuthenticationController = Em.Controller.extend({
  name:'mainAdminAuthenticationController',
  /**
   * save user form after editing
   * @param event
   */
  save:function (event) {
    var form = event.context;
    if (form.isValid()) {
      form.save();
      App.ModalPopup.show({
        header:Em.I18n.t('admin.authentication.form.testConfiguration'),
        body:form.get('resultText'),
        secondary:false,
        onPrimary:function () {
          this.hide();
        }
      });
    }
  },
  content:App.Authentication.find(1)
});