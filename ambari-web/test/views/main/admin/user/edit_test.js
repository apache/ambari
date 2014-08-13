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
require('views/main/admin/user/edit');

describe('App.MainAdminUserEditView', function () {

  var view = App.MainAdminUserEditView.create({
    userForm: Em.Object.create({
      getField: function (property) {
        return this.get(property) || Em.Object.create();
      },
      isValid: Em.K,
      isWarn: Em.K,
      propertyDidChange: Em.K
    })
  });

  describe('#edit()', function () {

    beforeEach(function () {
      sinon.stub(App.ajax, 'send', Em.K);
    });
    afterEach(function () {
      view.get('userForm').isValid.restore();
      App.ajax.send.restore();
    });

    it('form is invalid', function () {
      sinon.stub(view.get('userForm'), 'isValid', function () {
        return false;
      });

      expect(view.edit()).to.be.false;
      expect(App.ajax.send.called).to.be.false;
    });
    it('form is valid', function () {
      sinon.stub(view.get('userForm'), 'isValid', function () {
        return true;
      });
      sinon.stub(view, 'setPassword', Em.K);


      expect(view.edit()).to.be.true;
      expect(App.ajax.send.calledOnce).to.be.true;
      expect(view.setPassword.calledOnce).to.be.true;

      view.setPassword.restore();
    });
  });

  describe('#setPassword()', function () {
    var form = Em.Object.create({
      getField: function (property) {
        return this.get(property);
      },
      new_password: Em.Object.create({
        value: 'pass'
      }),
      old_password: Em.Object.create({
        value: 'pass'
      })
    });

    it('new_password value is empty', function () {
      var Users = {};
      form.set('new_password.value', "");

      expect(view.setPassword(Users, form)).to.be.false;
      expect(Users.password).to.be.undefined;
      expect(Users.old_password).to.be.undefined;
    });
    it('old_password value is empty', function () {
      var Users = {};
      form.set('old_password.value', "");

      expect(view.setPassword(Users, form)).to.be.false;
      expect(Users.password).to.be.undefined;
      expect(Users.old_password).to.be.undefined;
    });
    it('old_password and new_password values are correct', function () {
      var Users = {};
      form.set('old_password.value', "old_pass");
      form.set('new_password.value', "pass");

      expect(view.setPassword(Users, form)).to.be.true;
      expect(Users.password).to.equal('pass');
      expect(Users.old_password).to.equal('old_pass');
    });
  });

  describe('#editUserSuccessCallback()', function () {
    it('', function () {
      var params = {
        form: {
          save: Em.K
        }
      };
      sinon.spy(params.form, 'save');
      sinon.stub(App.router, 'transitionTo', Em.K);

      view.editUserSuccessCallback({}, {}, params);
      expect(params.form.save.calledOnce).to.be.true;
      expect(App.router.transitionTo.calledWith('allUsers')).to.be.true;

      App.router.transitionTo.restore();
    });
  });

  describe('#editUserErrorCallback()', function () {
    it('show popup', function () {
      sinon.stub(App.ModalPopup, 'show', Em.K);
      sinon.stub(view, 'parseErrorMessage', Em.K);

      view.editUserErrorCallback({});
      expect(App.ModalPopup.show.calledOnce).to.be.true;

      App.ModalPopup.show.restore();
      view.parseErrorMessage.restore();
    });
  });

  describe('#parseErrorMessage()', function () {
    it('":" is not present in response', function () {
      var request = {
        responseText: JSON.stringify({
          message: 'content'
        })
      };

      expect(view.parseErrorMessage(request)).to.equal('content');
    });
    it('one ":" is  present in response', function () {
      var request = {
        responseText: JSON.stringify({
          message: 'content : b'
        })
      };

      expect(view.parseErrorMessage(request)).to.equal(' b');
    });
    it('several ":" are  present in response', function () {
      var request = {
        responseText: JSON.stringify({
          message: 'content : b: a'
        })
      };

      expect(view.parseErrorMessage(request)).to.equal(' a');
    });
  });

  describe('#keyPress()', function () {

    beforeEach(function () {
      sinon.stub(view, 'edit', Em.K);
    });
    afterEach(function () {
      view.edit.restore();
    });

    it('not "Enter" button pressed', function () {
      expect(view.keyPress({keyCode: 99})).to.be.true;
      expect(view.edit.called).to.be.false;
    });
    it('"Enter" button pressed', function () {
      expect(view.keyPress({keyCode: 13})).to.be.false;
      expect(view.edit.calledOnce).to.be.true;
    });
  });

  describe('#didInsertElement()', function () {

    beforeEach(function () {
      sinon.stub(view.get('userForm'), 'propertyDidChange', Em.K);
    });
    afterEach(function () {
      view.get('userForm').propertyDidChange.restore();
    });
    view.set('userForm.old_password', Em.Object.create());
    view.set('userForm.new_password', Em.Object.create());
    view.set('userForm.new_passwordRetype', Em.Object.create());

    it('isLdap value is true', function () {
      view.set('userForm.isLdap', Em.Object.create({
        value: true
      }));

      view.didInsertElement();
      expect(view.get('userForm').propertyDidChange.calledWith('object')).to.be.true;
      expect(view.get('userForm.old_password.disabled')).to.be.true;
      expect(view.get('userForm.new_password.disabled')).to.be.true;
      expect(view.get('userForm.new_passwordRetype.disabled')).to.be.true;
    });
    it('isLdap value is false', function () {
      view.set('userForm.isLdap', Em.Object.create({
        value: false
      }));

      view.didInsertElement();
      expect(view.get('userForm').propertyDidChange.calledWith('object')).to.be.true;
      expect(view.get('userForm.old_password.disabled')).to.be.false;
      expect(view.get('userForm.new_password.disabled')).to.be.false;
      expect(view.get('userForm.new_passwordRetype.disabled')).to.be.false;
    });
  });
});
