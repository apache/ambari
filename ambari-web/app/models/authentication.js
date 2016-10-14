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

App.Authentication = DS.Model.extend({
  method:DS.attr('boolean'), // use LDAP
  primaryServer:DS.attr('string'),
  secondaryServer:DS.attr('string'),
  useSsl:DS.attr('boolean'),
  bindMethod:DS.attr('boolean'), // use credentials
  bindUser:DS.attr('string'),
  password:DS.attr('string'),
  passwordRetype:DS.attr('string'),
  searchBaseDn:DS.attr('string'),
  usernameAttribute:DS.attr('string')
});

App.Authentication.FIXTURES = [
  {
    id:1,
    method:0,
    primary_server:"",
    secondary_server:"",
    use_ssl:false,
    bind_method:0,
    bind_user:"",
    password:"",
    password_retype:"",
    search_base_dn:"",
    username_attribute:""
  }
];

App.AuthenticationForm = App.Form.extend({
  testResult:false,
  fieldsOptions:[
    { name:"method", displayName:"", isRequired:false, displayType:"select",
      values:[
        {value:0, label:Em.I18n.t("admin.authentication.form.method.database")},
        {value:1, label:Em.I18n.t("admin.authentication.form.method.ldap")}
      ]
    },
    { name:"primaryServer", displayName:Em.I18n.t("admin.authentication.form.primaryServer"), /*validator:'ipaddress',*/
      isRequired:function () {
        return this.get('form.field.method.value');
      }.property('form.field.method.value')
    },
    { name:"secondaryServer", displayName:Em.I18n.t("admin.authentication.form.secondaryServer"), /*validator:'ipaddress',*/ isRequired:false},
    { name:"useSsl", displayName:Em.I18n.t("admin.authentication.form.useSsl"), displayType:"checkbox", isRequired:false },
    { name:"bindMethod", displayName:'', displayType:"select", isRequired:false,
      values:[
        {value:0, label:Em.I18n.t("admin.authentication.form.bind.anonymously")},
        {value:1, label:Em.I18n.t("admin.authentication.form.bind.useCrenedtials")}
      ]},
    { name:"bindUser", displayName:Em.I18n.t('admin.authentication.form.bindUserDN'), isRequired:function () {
      return this.get('form.field.bindMethod.value');
    }.property('form.field.bindMethod.value')},
    { name:"password", displayName:Em.I18n.t('common.password'), displayType:"password",
      isRequired:function () {
        return this.get('form.field.bindMethod.value');
      }.property('form.field.bindMethod.value') },
    { name:"passwordRetype", displayName:Em.I18n.t('form.passwordRetype'), displayType:"password",
      validator: "passwordRetype",
      isRequired:function () {
        return this.get('form.field.bindMethod.value');
      }.property('form.field.bindMethod.value')},
    { name:"searchBaseDn", displayName:Em.I18n.t('admin.authentication.form.searchBaseDN'),
      isRequired:function () {
        return this.get('form.field.method.value');
      }.property('form.field.method.value')
    },
    { name:"usernameAttribute", displayName:Em.I18n.t('admin.authentication.form.usernameAttribute'),
      isRequired:function () {
        return this.get('form.field.method.value');
      }.property('form.field.method.value')
    },

    { name:"userDN", displayName:Em.I18n.t('admin.authentication.form.userDN') },
    { name:"userPassword", displayName:Em.I18n.t('common.password'), displayType:'password'}
  ],
  fields:[],
  testConfiguration:function () {
    this.set('testResult', parseInt(Math.random() * 2));
    return true;
  },
  testConfigurationMessage: Em.computed.ifThenElse('testResult', Em.I18n.t('admin.authentication.form.test.success'), Em.I18n.t('admin.authentication.form.test.fail')),

  testConfigurationClass: Em.computed.ifThenElse('testResult', 'text-success', 'text-danger')

});