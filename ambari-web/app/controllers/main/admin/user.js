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

  /**
   * send request to the server to delete user if selected user is not current user
   * @param event
   */
  deleteRecord:function (event) {
    var self = this;
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

    App.ModalPopup.show({
      header:Em.I18n.t('admin.users.delete.header').format(event.context.get('userName')),
      body:Em.I18n.t('question.sure').format(''),
      primary:Em.I18n.t('yes'),
      secondary:Em.I18n.t('no'),

      onPrimary:function () {
        self.sendCommandToServer('/users/' +  event.context.get("userName"), "DELETE" ,{},
          function (success) {

            if (!success) {
              return;
            }

            event.context.deleteRecord();

            try {
              App.store.commit();
            } catch (err) {

            }
          })
        this.hide();
      },
      onSecondary:function () {
        this.hide();
      }
    });
  },

  /**
   * send request to the server and call callback function with true if request was success or false if request was failed
   * @param url
   * @param method
   * @param postData
   * @param callback
   */
  sendCommandToServer : function(url, method, postData, callback){
    if (App.testMode) {
      url = '/data/users/users.json';
      method = 'GET';
      postData = undefined;
    } else {
      url = App.apiPrefix + url;
    }

    $.ajax({
      type: method,
      url: url,
      data: JSON.stringify(postData),
      dataType: 'json',
      timeout: App.timeout,
      success: function(data){
          callback(true, '');
      },

      error: function (request, ajaxOptions, error) {
        var message = $.parseJSON(request.responseText).message;
        message = message.substr(message.lastIndexOf(':') + 1);
        callback(false, message);
        console.log(message);
      },

      statusCode: require('data/statusCodes')
    });
  }
})