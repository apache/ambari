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
var validator = require('utils/validator');

App.User = DS.Model.extend({
  userName:DS.attr('string'),
  id:function(){
    return this.get('userName');
  }.property('userName'),
  isLdap:DS.attr('boolean'),
  type: function(){
    if(this.get('isLdap')){
      return 'LDAP';
    }
    return 'Local';
  }.property('isLdap'),
  auditItems:DS.hasMany('App.ServiceAudit'),
  admin: DS.attr('boolean'),
  operator: DS.attr('boolean'),
  /**
   * List of permissions assigned to user
   *  Available permissions:
   *    AMBARI.ADMIN
   *    CLUSTER.READ
   *    CLUSTER.OPERATE
   *    VIEW.USE
   * @property {Array} permissions
   **/
  permissions: DS.attr('array')
});

App.EditUserForm = App.Form.extend({
  className:App.User,
  object:function () {
    return App.router.get('mainAdminUserEditController.content');
  }.property('App.router.mainAdminUserEditController.content'),

  fieldsOptions:[
    { name:"userName", displayName:"Username" },
    { name:"old_password", displayName:"Current Password", displayType:"password", isRequired: false },
    { name:"new_password", displayName:"New Password", displayType:"password",  isRequired: false },
    { name:"new_passwordRetype", displayName:"Retype New Password", displayType:"password", isRequired: false },
    { name:"admin", displayName:"Admin", displayType:"checkbox", isRequired:false },
    { name:"isLdap", displayName:"Type", isRequired:false, isHidden:true }
  ],
  fields:[],
  disableUsername:function () {
    this.getField("userName").set("disabled", "disabled");
  }.observes('object'),
  disableAdminCheckbox:function () {
    var object = this.get('object');
    if (object) {
      if (object.get('userName') == App.get('router').getLoginName()) {
        this.getField("admin").set("disabled", true);
      } else {
        this.getField("admin").set("disabled", false);
      }
    }
  }.observes('object'),

  isValid:function () {

    var isValid = this._super();
    var thisForm = this;

    var newPass = this.get('field.new_password');
    var oldPass = this.get('field.old_password');
    var passRetype = this.get('field.new_passwordRetype');

    if (!validator.empty(newPass.get('value'))) {
      if(validator.empty(oldPass.get('value'))){
        oldPass.set('errorMessage', this.t('admin.users.editError.requiredField'));
        isValid = false;
      }
      if (newPass.get('value') != passRetype.get('value')) {
        passRetype.set('errorMessage', this.t('admin.users.createError.passwordValidation'));
        isValid = false;
      }
    }

    return isValid;
  },

  save: function () {
    var object = this.get('object');
    var formValues = {};
    $.each(this.get('fields'), function () {
      formValues[this.get('name')] = this.get('value');
    });

    $.each(formValues, function (k, v) {
      object.set(k, v);
    });

    //App.store.commit();
    this.set('result', 1);

    return true;
  }
});
App.CreateUserForm = App.Form.extend({
  className:App.User,
  object:function () {
    return App.router.get('mainAdminUserCreateController.content');
  }.property('App.router.mainAdminUserCreateController.content'),

  fieldsOptions:[
    { name:"userName", displayName:"Username", toLowerCase: function(){var v = this.get('value'); this.set('value', v.toLowerCase())}.observes('value') },
    { name:"password", displayName:"Password", displayType:"password", isRequired: true },
    { name:"passwordRetype", displayName:"Retype Password", displayType:"password", validator:"passwordRetype", isRequired: true },
    { name:"admin", displayName:"Admin", displayType:"checkbox", isRequired:false, defaultValue: true}
  ],
  fields:[],

  isValid:function () {
    var isValid = this._super();

    var passField = this.get('field.password');
    var passRetype = this.get('field.passwordRetype');

    if (!validator.empty(passField.get('value'))) {
      if (passField.get('value') != passRetype.get('value')) {
        passRetype.set('errorMessage', this.t('admin.users.createError.passwordValidation'));
        isValid = false;
      }
    }

    if (isValid) {
      var users = App.User.find();
      var userNameField = this.getField('userName');
      var userName = userNameField.get('value');

      if (users.mapProperty('userName').contains(userName)) {
        userNameField.set('errorMessage', this.t('admin.users.createError.userNameExists'));
        return isValid = false;
      }
    }

    return isValid;
  },

  isWarn: function() {
    var isWarn = false;
    var userNameField = this.getField('userName');
    userNameField.set('warnMessage', '');
    var userName = userNameField.get('value');

    if (this.isValid() && !validator.isValidUserName(userName)) {
      userNameField.set('warnMessage', this.t('users.userName.validationFail'));
      isWarn = true;
    }
    return isWarn;
  },

  save: function () {

    var object = this.get('object');
    var formValues = {};
    $.each(this.get('fields'), function () {
      formValues[Ember.String.decamelize(this.get('name'))] = this.get('value');
    });

    if (this.get('className')) {
      App.store.load(this.get('className'), App.dateTime(), formValues);
    }
    else {
      console.log("Please define class name for your form " + this.constructor);
    }

    this.set('result', 1);

    return true;
  }
});
App.User.FIXTURES = [];

