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
require('views/main/admin/user/create');

describe('App.MainAdminUserCreateView', function () {

  var view = App.MainAdminUserCreateView.create({
    userForm: Em.Object.create({
      isValid: function () {
        return this.get('mockIsValid');
      },
      getField: function () {
        return Em.Object.create();
      },
      isWarn: Em.K,
      propertyDidChange: Em.K,
      mockIsValid: false
    })
  });

  describe('#create()', function () {

    it('form is invalid', function () {
      view.set('userForm.mockIsValid', false);
      expect(view.create()).to.be.false;
    });
    it('form is valid', function () {
      view.set('userForm.mockIsValid', true);
      sinon.stub(view, 'identifyRoles', Em.K);
      sinon.stub(App.ajax, 'send', Em.K);

      expect(view.create()).to.be.true;
      expect(view.identifyRoles.calledOnce).to.be.true;
      expect(App.ajax.send.calledOnce).to.be.true;

      view.identifyRoles.restore();
      App.ajax.send.restore();
    });
  });

  describe('#identifyRoles()', function () {
    var mock = Em.Object.create();
    var form = Em.Object.create({
      getField: function () {
        return mock;
      }
    });

    it('admin is false', function () {
      mock.set('value', false);

      expect(view.identifyRoles(form)).to.equal('user');
      expect(mock.get('value')).to.equal('user');
    });
    it('admin is true', function () {
      mock.set('value', true);

      expect(view.identifyRoles(form)).to.equal('admin,user');
      expect(mock.get('value')).to.equal('admin,user');
    });
  });

  describe('#createUserSuccessCallback()', function () {

    it('', function () {
      var mock = {
        persistKey: function () {
          return 'persists';
        },
        postUserPref: Em.K
      };
      var params = {
        form: {
          getField: function () {
            return Em.Object.create({
              value: 'user_name_value'
            })
          },
          save: Em.K
        }
      };
      sinon.stub(App.ModalPopup, 'show', Em.K);
      sinon.stub(App.router, 'get', function () {
        return mock;
      });
      sinon.spy(mock, 'persistKey');
      sinon.spy(mock, 'postUserPref');
      sinon.spy(params.form, 'save');
      sinon.stub(App.router, 'transitionTo', Em.K);

      view.createUserSuccessCallback({}, {}, params);
      expect(App.ModalPopup.show.calledOnce).to.be.true;
      expect(mock.persistKey.calledWith('user_name_value')).to.be.true;
      expect(mock.postUserPref.calledWith('persists', true)).to.be.true;
      expect(params.form.save.calledOnce).to.be.true;
      expect(App.router.transitionTo.calledWith('allUsers')).to.be.true;

      App.ModalPopup.show.restore();
      App.router.get.restore();
      App.router.transitionTo.restore();
    });
  });

  describe('#createUserErrorCallback()', function () {

    it('', function () {
      sinon.stub(App.ModalPopup, 'show', Em.K);

      view.createUserErrorCallback();
      expect(App.ModalPopup.show.calledOnce).to.be.true;

      App.ModalPopup.show.restore();
    });
  });

  describe('#keyPress()', function () {

    beforeEach(function () {
      sinon.stub(view, 'create', Em.K);
    });
    afterEach(function () {
      view.create.restore();
    });

    it('not "Enter" button pressed', function () {
      expect(view.keyPress({keyCode: 99})).to.be.true;
      expect(view.create.called).to.be.false;
    });
    it('"Enter" button pressed', function () {
      expect(view.keyPress({keyCode: 13})).to.be.false;
      expect(view.create.calledOnce).to.be.true;
    });
  });

  describe('#passwordValidation()', function () {
    var mock = Em.Object.create();

    beforeEach(function () {
      sinon.stub(view.get('userForm'), 'getField', function () {
        return mock;
      });
      sinon.stub(view.get('userForm'), 'isValid', Em.K);
      sinon.stub(view.get('userForm'), 'isWarn', Em.K);
    });
    afterEach(function () {
      view.get('userForm').getField.restore();
      view.get('userForm').isValid.restore();
      view.get('userForm').isWarn.restore();
    });

    it('passwordValue is null, isPasswordDirty = false', function () {
      mock.set('value', null);
      view.set('isPasswordDirty', false);

      view.passwordValidation();
      expect(view.get('isPasswordDirty')).to.be.false;
      expect(view.get('userForm').isValid.called).to.be.false;
      expect(view.get('userForm').isWarn.called).to.be.false;
    });
    it('passwordValue is correct, isPasswordDirty = true', function () {
      mock.set('value', 'pass');
      view.set('isPasswordDirty', true);

      view.passwordValidation();
      expect(view.get('isPasswordDirty')).to.be.true;
      expect(view.get('userForm').isValid.calledOnce).to.be.true;
      expect(view.get('userForm').isWarn.calledOnce).to.be.true;
    });
    it('passwordValue is correct, isPasswordDirty = false', function () {
      mock.set('value', 'pass');
      view.set('isPasswordDirty', false);

      view.passwordValidation();
      expect(view.get('isPasswordDirty')).to.be.true;
      expect(view.get('userForm').isValid.calledOnce).to.be.true;
      expect(view.get('userForm').isWarn.calledOnce).to.be.true;
    });
  });

  describe('#didInsertElement()', function () {
    it('propertyDidChange function should be called', function () {
      sinon.spy(view.get('userForm'), 'propertyDidChange');

      view.didInsertElement();
      expect(view.get('userForm').propertyDidChange.calledWith('object')).to.be.true;

      view.get('userForm').propertyDidChange.restore();
    });
  });
});
