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

App.UserModel = Em.Object.extend({
  userName:null,
  id:0
});

App.User = DS.Model.extend({
  userName:DS.attr('string'),
  admin:DS.attr('boolean'),
  password:DS.attr('string'),
  auditItems:DS.hasMany('App.ServiceAudit')
});

App.UserForm = App.Form.extend({
  className:App.User,
  fieldsOptions:[
    { name:"userName", displayName:"Username" },
    { name:"password", displayName:"Password", displayType:"password", isRequired: function(){ return this.get('form.isObjectNew'); }.property('form.isObjectNew') },
    { name:"passwordRetype", displayName:"Retype Password", displayType:"password", validator:"passwordRetype", isRequired: false },
    { name:"admin", displayName:"Admin", displayType:"checkbox", isRequired:false }
  ],
  fields:[],
  disableUsername:function () {
    var field = this.getField("userName");
    if (field) field.set("disabled", this.get('isObjectNew') ? false : "disabled");

  }.observes('isObjectNew'),
  disableAdminCheckbox:function () {
    if (!this.get('isObjectNew')) {
      var object = this.get('object');
      var field = this.getField("admin");
      if (field) {
        field.set("disabled", object.get('userName') == App.get('router').getLoginName() ? "disabled" : false);
      }
    }
  }.observes('isObjectNew'),

  isValid: function(){
    var isValid = this._super();
    thisForm = this;

    var passField = this.get('field.password');
    var passRetype = this.get('field.passwordRetype');

    if(!validator.empty(passField.get('value'))) {
      if(passField.get('value') != passRetype.get('value')) {
        passRetype.set('errorMessage', "Passwords are different");
        isValid = false;
      }
    }

    if(isValid && this.get('isObjectNew')) {
      var users = App.User.find();
      var userNameField = this.getField('userName');
      var userName = userNameField.get('value');

      users.forEach(function(user){
        if(userName == user.get('userName')) {
          userNameField.set('errorMessage', 'User with the same name is already exists');
          return isValid = false;
        }
      });
    }
    
    return isValid;
  }
});

App.User.FIXTURES = [
  {
    id:1,
    user_name:'admin',
    password:'admin',
    admin:1
  },
  {
    id:2,
    user_name:'vrossi',
    admin:1
  },
  {
    id:3,
    user_name:'casey.stoner',
    admin:0
  },
  {
    id:4,
    user_name:'danip',
    admin:0
  }
];