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

App.MainAdminUserCreateView = Em.View.extend({
  templateName: require('templates/main/admin/user/create'),
  userId: false,
  isPasswordDirty: false,

  create: function() {
    var form = this.get("userForm");
    if(form.isValid()) {
      form.getField("userName").set('value', form.getField("userName").get('value').toLowerCase());
      if(form.getField("admin").get('value') === "" || form.getField("admin").get('value') == true) {
        form.getField("roles").set("value","admin,user");
        form.getField("admin").set("value","true");
      }
      else {
        form.getField("roles").set("value","user");
      }

      App.ajax.send({
        name: 'admin.user.create',
        sender: this,
        data: {
          user: form.getField("userName").get('value'),
          form: form,
          data: {
            Users: {
              password: form.getField("password").get('value'),
              roles: form.getField("roles").get('value')
            }
          }
        },
        success: 'createUserSuccessCallback',
        error: 'createUserErrorCallback'
      });
    }
  },

  createUserSuccessCallback: function(data, opts, params) {
    App.ModalPopup.show({
      header: Em.I18n.t('admin.users.addButton'),
      body: Em.I18n.t('admin.users.createSuccess'),
      secondary: null
    });
    var persists = App.router.get('applicationController').persistKey(params.form.getField("userName").get('value'));
    App.router.get('applicationController').postUserPref(persists, true);
    params.form.save();
    App.router.transitionTo("allUsers");
  },

  createUserErrorCallback: function () {
    App.ModalPopup.show({
      header: Em.I18n.t('admin.users.addButton'),
      body: Em.I18n.t('admin.users.createError'),
      secondary: null
    });
  },

  userForm: App.CreateUserForm.create({}),

  keyPress: function(event) {
    if (event.keyCode === 13) {
      this.create();
      return false;
    }
  },

  passwordValidation: function() {
    var passwordValue = this.get('userForm').getField('password').get('value');
    if (passwordValue && !this.get('isPasswordDirty')) {
      this.set('isPasswordDirty', true);
    }
    if (this.get('isPasswordDirty')) {
      this.get('userForm').isValid();
    }
  }.observes('userForm.fields.@each.value'),

  didInsertElement: function(){
    this.get('userForm').propertyDidChange('object');
  }
});
