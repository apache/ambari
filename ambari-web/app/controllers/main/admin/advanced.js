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

App.MainAdminAdvancedController = Em.Controller.extend({
  name:'mainAdminAdvancedController',
  uninstall: function(event){
    var params = event.context;
    App.ModalPopup.show({
      uninstallParams: params,
      header: Em.I18n.t('admin.advanced.popup.header'),
      bodyClass: App.MainAdminAdvancedPasswordView.reopen({}), // layout: Em.Handlebars.compile()
      onPrimary: function(){
        var form = this.getForm();
        if(form) {
          if(form.isValid()) {
            console.warn("TODO: request for cluster uninstall");
          }
        }
        this.onClose();
      },
      onSecondary: function(){
        this.onClose();
      },

      getForm: function(){
        var form = false;
        $.each(this.get('_childViews'), function(){
          if(this.get('path') == "bodyClass") {
            return form = this.get('_childViews')[0];
          }
        });

        return form;
      }
    })
  }
});