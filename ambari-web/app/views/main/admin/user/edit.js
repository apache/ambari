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
  userId: false,
  edit: function(event){
    var parent_controller=this.get("controller").controllers.mainAdminUserController;
    var form = this.get("userForm");
    if(form.isValid()) {
      var Users={};
      if(form.getValues().admin === "" || form.getValues().admin == true) {
        form.getField("roles").set("value","admin,user");
        form.getField("admin").set("value","true");
      } else{
        form.getField("roles").set("value","user");
      }

      Users.roles = form.getValues().roles;

      if(form.getValues().new_password != "" && form.getValues().old_password != ""){
        Users.password=form.getValues().new_password;
        Users.old_password=form.getValues().old_password;
      }

      parent_controller.sendCommandToServer('/users/' + form.getValues().userName, "PUT" , {
       Users:Users
      }, function (success) {

        if (!success) {
          return;
        }

        form.save();

        App.router.transitionTo("allUsers");
      })
    }
  },

  userForm: App.EditUserForm.create({}),

  didInsertElement: function(){
    var form = this.get('userForm');
    if( form.getField("isLdap").get("value") )
    {
      form.getField("old_password").set("disabled",true);
      form.getField("new_password").set("disabled",true);
    }else{
      //debugger;
      form.getField("old_password").set("disabled",false);
      form.getField("new_password").set("disabled",false);
    }
    form.propertyDidChange('object');
  }
});