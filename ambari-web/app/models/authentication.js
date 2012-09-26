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
  retypePassword:DS.attr('string'),
  searchBaseDn:DS.attr('string'),
  usernameAttribute:DS.attr('string')
});

App.Authentication.FIXTURES = [
  {
    id:1,
    method:false,
    primary_server:"1.2.3.4:78",
    secondary_server:"225.225.255.255:12",
    use_ssl:false,
    bind_method:false,
    bind_user:"hadoop\Administrator",
    password:"1234",
    retype_password:"1234",
    search_base_dn:"DC=hadoop,DC=abc,DC=com",
    username_attribute:"sAMAccountName"
  }
]

App.AuthenticationForm = App.Form.extend({
  testResult:false,
  isObjectNew:false,
  fieldsOptions:[
    { name:"method", displayName:"", isRequired:false, displayType:"select",
      values:[
        {value:0, label:Em.I18n.t("admin.authentication.form.method.database")},
        {value:1, label:Em.I18n.t("admin.authentication.form.method.ldap")}
      ]
    },
    { name:"primaryServer", displayName:Em.I18n.t("admin.authentication.form.primaryServer"), validator:'ipaddress'},
    { name:"secondaryServer", displayName:Em.I18n.t("admin.authentication.form.secondaryServer"), validator:'ipaddress'},
    { name:"useSsl", displayName:Em.I18n.t("admin.authentication.form.useSsl"), displayType:"checkbox", isRequired:false },
    { name:"bindMethod", displayName:'', displayType:"select", isRequired:false,
      values:[
        {value:0, label:Em.I18n.t("admin.authentication.form.bind.anonymously")},
        {value:1, label:Em.I18n.t("admin.authentication.form.bind.useCrenedtials")}
      ]},
    { name:"bindUser", displayName:Em.I18n.t('admin.authentication.form.bindUserDN')},
    { name:"password", displayName:Em.I18n.t('form.password'), displayType:"password" },
    { name:"passwordRetype", displayName:Em.I18n.t('form.passwordRetype'), displayType:"passwordRetype"},
    { name:"searchBaseDn", displayName:Em.I18n.t('admin.authentication.form.searchBaseDN')},
    { name:"usernameAttribute", displayName:Em.I18n.t('admin.authentication.form.usernameAttribute')},

    { name:"userDN", displayName:Em.I18n.t('admin.authentication.form.userDN') },
    { name:"userPassword", displayName:Em.I18n.t('admin.authentication.form.password'), displayType:'password'}
  ],
  fields:[],
  testConfiguration:function () {
    console.warn('Configuration test is randomized');
    this.set('testResult', parseInt(Math.random() * 2));
    return true;
  },
  testConfigurationMessage:function () {
    return this.get('testResult') ? Em.I18n.t('admin.authentication.form.test.success') : Em.I18n.t('admin.authentication.form.test.fail');
  }.property('testResult'),
  testConfigurationClass:function () {
    return this.get('testResult') ? "text-success" : "text-error";
  }.property('testConfigurationMessage')
});