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

var modelSetup = require('test/init_model_test');
require('models/user');

var user,
  form,
  userNameField,
  userData = {
    id: 'user'
  };

function getUser() {
  return App.User.createRecord(userData);
}

describe('App.User', function () {

  beforeEach(function () {
    user = getUser();
  });

  afterEach(function () {
    modelSetup.deleteRecord(user);
  });

  App.TestAliases.testAsComputedAlias(getUser(), 'id', 'userName', 'string');

  describe('#id', function () {
    it('should take value from userName', function () {
      user.set('userName', 'name');
      expect(user.get('id')).to.equal('name');
    });
  });

  describe('#isLdap', function() {
    it('User userType value is "LDAP" should return "true"', function() {
      user.set('userType', 'LDAP');
      expect(user.get('isLdap')).to.be.true;
    });
    it('User userType value is "LOCAL" should return "false"', function() {
      user.set('userType', 'LOCAL');
      expect(user.get('isLdap')).to.be.false;
    });
  });
});

function getForm() {
  return App.CreateUserForm.create();
}

describe('App.CreateUserForm', function () {

  beforeEach(function () {
    form = getForm();
  });

  App.TestAliases.testAsComputedAlias(getForm(), 'object', 'App.router.mainAdminUserCreateController.content', 'object');

  describe('#field.userName.toLowerCase', function () {
    it('should convert userName into lower case', function () {
      userNameField = form.getField('userName');
      userNameField.set('value', 'NAME');
      expect(userNameField.get('value')).to.equal('name');
    });
  });

  describe('#isValid', function () {
    it('should be false as default', function () {
      expect(form.isValid()).to.be.false;
    });
    it('should be true', function () {
      form.get('fields').forEach(function (item) {
        if (item.get('isRequired')) {
          item.set('value', 'value');
        }
      });
      expect(form.isValid()).to.be.true;
    });
  });

  describe('#isWarn', function () {
    it('should be false as default', function () {
      expect(form.isWarn()).to.be.false;
    });
    it('should be true', function () {
      form.getField('userName').set('value', '1');
      expect(form.isWarn()).to.be.true;
    });
    it('should be false', function () {
      form.getField('userName').set('value', 'name');
      expect(form.isWarn()).to.be.false;
    });
  });

});
