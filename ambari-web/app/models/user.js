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
    { name:"password", displayName:"Password", displayType:"password", disableRequiredOnExistent:true },
    { name:"passwordRetype", displayName:"Retype Password", displayType:"passwordRetype", disableRequiredOnExistent:true },
    { name:"admin", displayName:"Admin", displayType:"checkbox", isRequired:false }
  ],
  fields:[],
  disableUsername:function () {
    var field = this.getField("userName");
    if (field) field.set("disabled", this.get('isObjectNew') ? false : "disabled");

  }.observes('isObjectNew'),
  disableAdminCheckbox:function () {
    var object = this.get('object');
    var field = this.getField("admin");
    if (field) {
      field.set("disabled", object.get('userName') == App.get('router').getLoginName() ? "disabled" : false);
    }

  }.observes('isObjectNew')
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