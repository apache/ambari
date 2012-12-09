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

App.MainAdminUserController = Em.Controller.extend({
  name:'mainAdminUserController',
  deleteRecord:function (event) {
    if (event.context.get('userName') == App.get('router').getLoginName()) {
      App.ModalPopup.show({
        header:Em.I18n.t('admin.users.delete.yourself.header'),
        body:Em.I18n.t('admin.users.delete.yourself.message'),
        onPrimary:function (event) {
          this.hide();
        },
        secondary:false
      });

      return;
    }
    ;

    App.ModalPopup.show({
      itemToDelete:event.context,
      header:Em.I18n.t('admin.users.delete.header').format(event.context.get('userName')),
      body:Em.I18n.t('question.sure'),
      primary:Em.I18n.t('yes'),
      secondary:Em.I18n.t('no'),
      controller:this.controllers.mainAdminUserEditController,
      onPrimary:function (event) {

        //TODO: change sendCommandToServer parametrs for proper API request
        this.get('controller').sendCommandToServer('/users/delete/' + this.get('itemToDelete').context.get("userName"), {
            Users:{ /* password: form.getValues().password, roles: form.getValues().roles*/ }
          },
          function (requestId) {

            if (!requestId) {
              return;
            }

            this.get('itemToDelete').context.deleteRecord();
            try {
              App.store.commit()
            } catch (err) {
            }
            ;
          })
      },
      onSecondary:function (event) {
        this.hide();
      }
    });
  }
})