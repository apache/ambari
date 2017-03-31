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
  id: Em.computed.alias('userName'),
  userType: DS.attr('string'),
  auditItems:DS.hasMany('App.ServiceAudit'),
  admin: DS.attr('boolean'),
  operator: DS.attr('boolean'),
  clusterUser: DS.attr('boolean'),

  /**
   * List of permissions assigned to user
   *  Available permissions:
   *    AMBARI.ADMINISTRATOR
   *    CLUSTER.USER
   *    CLUSTER.ADMINISTRATOR
   *    VIEW.USER
   * @property {Array} permissions
   **/
  permissions: DS.attr('array'),

  /**
   * @type {Boolean}
   */
  isLdap: Em.computed.equal('userType', 'LDAP')
});

App.CreateUserForm = App.Form.extend({
  className:App.User,
  object: Em.computed.alias('App.router.mainAdminUserCreateController.content'),

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
      App.store.safeLoad(this.get('className'), App.dateTime(), formValues);
    }

    this.set('result', 1);

    return true;
  }
});
App.User.FIXTURES = [];

