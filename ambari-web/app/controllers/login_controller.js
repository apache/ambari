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

App.LoginController = Em.Object.extend({

  name:'loginController',
  loginName:'',
  password:'',
  errorMessage:'',

  submit:function (e) {
    console.log('Login: ' + this.get('loginName') + ' Password: ' + this.get('password'));

    this.set('errorMessage', '');

    var userId = this.validateCredentials();
    if (userId) {
      App.get('router').login(this.get('loginName'), App.User.find(userId));
    } else {
      console.log('Failed to login as: ' + this.get('loginName'));
      this.set('errorMessage', Em.I18n.t('login.error'));
    }
  },

  /**
   *
   * @return {number} user by credentials || {undefined}
   */
  validateCredentials:function () {
    //TODO: REST api that validates the login
    var thisController = this;

    var user = App.store.filter(App.User, function (data) {
      return data.get('user_name') == thisController.get('loginName') && data.get('password') == thisController.get('password')
    });

    console.log(user, user.content[0]);

    return user.content[0];
  }

});