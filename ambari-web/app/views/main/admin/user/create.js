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
  create: function(event){
    var parent_controller=this.get("controller").controllers.mainAdminUserController;
    var form = this.get("userForm");
    if(form.isValid()) {
      form.getField("userName").set('value', form.getValues().userName.toLowerCase());
      if(form.getValues().admin === "" || form.getValues().admin == true) {
        form.getField("roles").set("value","admin,user");
        form.getField("admin").set("value","true");
      } else{
        form.getField("roles").set("value","user");
      }

      parent_controller.sendCommandToServer('/users/' + form.getValues().userName, "POST" , {
        Users: {
          password: form.getValues().password,
          roles: form.getValues().roles
        }
      }, function (success) {

        if (!success) {
          return;
        }

        form.save();

        App.router.transitionTo("allUsers");
      })
    }
  },

  userForm: App.CreateUserForm.create({}),

  didInsertElement: function(){
    this.get('userForm').propertyDidChange('object');
  }
});
