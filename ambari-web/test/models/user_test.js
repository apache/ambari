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
  },
  objectData = Em.Object.create({
    userName: 'name',
    isLdap: true
  });

describe('App.User', function () {

  beforeEach(function () {
    user = App.User.createRecord(userData);
  });

  afterEach(function () {
    modelSetup.deleteRecord(user);
  });

  describe('#id', function () {
    it('should take value from userName', function () {
      user.set('userName', 'name');
      expect(user.get('id')).to.equal('name');
    });
  });

  describe('#type', function () {
    it('should be LDAP', function () {
      user.set('isLdap', true);
      expect(user.get('type')).to.equal('LDAP');
    });
    it('should be Local', function () {
      user.set('isLdap', false);
      expect(user.get('type')).to.equal('Local');
    });
  });

});

describe('App.EditUserForm', function () {

  beforeEach(function () {
    form = App.EditUserForm.create();
  });

  describe('#object', function () {

    before(function () {
      sinon.stub(App.router, 'get', function (k) {
        if (k === 'mainAdminUserEditController.content') return userData;
        return Em.get(App.router, k);
      });
    });

    after(function () {
      App.router.get.restore();
    });

    it('should take data from controller', function () {
      expect(form.get('object')).to.eql(userData);
    });

  });

  describe('#disableUsername', function () {
    it('should update userName field', function () {
      form.set('object', userData);
      expect(form.get('field.userName.disabled')).to.equal('disabled');
    });
  });

  describe('#disableAdminCheckbox', function () {

    before(function () {
      sinon.stub(App, 'get', function(k) {
        switch (k) {
          case 'router':
            return {
              getLoginName: Em.K
            };
          default:
            return Em.get(App, k);
        }
      });
      sinon.stub(App.router, 'get', function (k) {
        if (k === 'mainAdminUserEditController.content') return objectData;
        return Em.get(App.router, k);
      });
    });

    after(function () {
      App.get.restore();
      App.router.get.restore();
    });

    it('should not disable', function () {
      expect(form.get('field.admin.disabled')).to.be.false;
    });

    it('should disable', function () {
      form.set('object', objectData);
      expect(form.get('field.admin.disabled')).to.be.false;
    });
  });

  describe('#isValid', function () {
    it('should be true as default', function () {
      expect(form.isValid()).to.be.true;
    });
    it('should be false', function () {
      form.set('field.new_password.isRequired', true);
      expect(form.isValid()).to.be.false;
    });
  });

  describe('#save', function () {

    before(function () {
      sinon.stub(App.router, 'get', function (k) {
        if (k === 'mainAdminUserEditController.content') return objectData;
        return Em.get(App.router, k);
      });
    });

    after(function () {
      App.router.get.restore();
    });

    it('should record form values to object', function () {
      form.set('field.userName.value', 'name');
      form.save();
      expect(form.get('object.userName')).to.equal('name');
    });
  });

});

describe('App.CreateUserForm', function () {

  beforeEach(function () {
    form = App.CreateUserForm.create();
  });

  describe('#object', function () {

    before(function () {
      sinon.stub(App.router, 'get', function (k) {
        if (k === 'mainAdminUserCreateController.content') return userData;
        return Em.get(App, k);
      });
    });

    after(function () {
      App.router.get.restore();
    });

    it('should take data from controller', function () {
      expect(form.get('object')).to.eql(userData);
    });

  });

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
