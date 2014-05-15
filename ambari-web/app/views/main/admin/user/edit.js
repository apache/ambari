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

App.MainAdminUserEditView = Em.View.extend({

  templateName: require('templates/main/admin/user/edit'),

  /**
   * @type {bool}
   */
  userId: false,

  /**
   * Form to edit existing user
   * @type {App.EditUserForm}
   */
  userForm: App.EditUserForm.create({}),

  /**
   * Edit existing user
   * @method edit
   * @return {Boolean}
   */
  edit: function () {
    var form = this.get("userForm");
    if (!form.isValid()) return false;

    var Users = {
      roles: this.identifyRoles(form)
    };

    this.setPassword(Users, form);

    return !!App.ajax.send({
      name: 'admin.user.edit',
      sender: this,
      data: {
        form: form,
        user: form.getField("userName").get('value'),
        data: JSON.stringify({
          Users: Users
        })
      },
      success: 'editUserSuccessCallback',
      error: 'editUserErrorCallback'
    });
  },

  /**
   * set password to query data if it's not empty string
   * @param Users
   * @param form
   * @return {Boolean}
   */
  setPassword: function (Users, form) {
    if (form.getField("new_password").get('value') != "" && form.getField("old_password").get('value') != "") {
      Users.password = form.getField("new_password").get('value');
      Users.old_password = form.getField("old_password").get('value');
      return true;
    }
    return false;
  },

  /**
   * identify roles of user by admin checkbox
   * @param form
   * @return {String}
   */
  identifyRoles: function (form) {
    var roles = (form.getField("admin").get('value') === true) ? 'admin,user' : 'user';
    form.getField("roles").set("value", roles);
    return roles;
  },

  /**
   * Success callback for edit user request
   * @param {object} data
   * @param {object} opt
   * @param {object} params
   * @method editUserSuccessCallback
   */
  editUserSuccessCallback: function (data, opt, params) {
    params.form.save();
    App.router.transitionTo("allUsers");
  },

  /**
   * Error callback for edit user request
   * @param {object} request
   * @method editUserErrorCallback
   */
  editUserErrorCallback: function (request) {
    App.ModalPopup.show({
      header: Em.I18n.t('admin.users.editButton'),
      body: this.parseErrorMessage(request),
      secondary: null
    });
  },

  /**
   * derive only valuable info from response,
   * return content after last ":"
   * @param request
   * @return {String}
   */
  parseErrorMessage: function (request) {
    var message = JSON.parse(request.responseText).message;
    return message.substr(message.lastIndexOf(':') + 1);
  },

  /**
   * Submit form by Enter-click
   * @param {object} event
   * @returns {bool}
   * @method keyPress
   */
  keyPress: function (event) {
    if (event.keyCode === 13) {
      this.edit();
      return false;
    }
    return true;
  },

  didInsertElement: function () {
    var form = this.get('userForm');
    var isLdapValue = form.getField("isLdap").get("value");
    form.getField("old_password").set("disabled", isLdapValue);
    form.getField("new_password").set("disabled", isLdapValue);
    form.getField("new_passwordRetype").set("disabled", isLdapValue);
    form.propertyDidChange('object');
  }
});
