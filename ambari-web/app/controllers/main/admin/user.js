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

  name: 'mainAdminUserController',

  /**
   * Send request to the server to delete user if selected user is not current user
   * @param {object} event
   * @method deleteRecord
   */
  deleteRecord: function (event) {
    var self = this;
    if (event.context.get('userName') == App.get('router').getLoginName()) {
      App.ModalPopup.show({
        header: Em.I18n.t('admin.users.delete.yourself.header'),
        body: Em.I18n.t('admin.users.delete.yourself.message'),
        secondary: false
      });
      return;
    }

    App.ModalPopup.show({
      header: Em.I18n.t('admin.users.delete.header').format(event.context.get('userName')),
      body: Em.I18n.t('question.sure').format(''),
      primary: Em.I18n.t('yes'),
      secondary: Em.I18n.t('no'),

      onPrimary: function () {
        App.ajax.send({
          name: 'admin.user.delete',
          sender: self,
          data: {
            user: event.context.get("userName"),
            event: event
          },
          success: 'deleteUserSuccessCallback'
        });
        this.hide();
      }
    });
  },

  /**
   * Success callback for delete user request
   * @param {object} data
   * @param {object} opt
   * @param {object} params
   * @method deleteUserSuccessCallback
   */
  deleteUserSuccessCallback: function (data, opt, params) {
    params.event.context.deleteRecord();
    try {
      App.store.commit();
    } catch (err) {
    }
  }
});