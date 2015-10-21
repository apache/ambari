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
var userSettingsController;

describe('App.UserSettingsController', function () {

  beforeEach(function () {
    userSettingsController = App.UserSettingsController.create();
  });

  describe('#userSettingsKeys', function () {
    it('should not be empty', function () {
      expect(Object.keys(userSettingsController.get('userSettingsKeys'))).to.have.length.gt(0);
    });
  });

  describe('#showSettingsPopup', function() {
    var dataToShowRes = {};

    beforeEach(function () {
      sinon.stub(App.ModalPopup, 'show', function(dataToShow){
        dataToShowRes = dataToShow;
      });
      sinon.stub(App, 'isAccessible').returns(true);
      var emulatorClass = function() {};
      emulatorClass.prototype.done = function(func) {
        if (func) {
          func();
        }
      };
      var emulator = new emulatorClass();
      sinon.stub(userSettingsController, 'dataLoading').returns(emulator);
    });
    afterEach(function () {
      App.isAccessible.restore();
      App.ModalPopup.show.restore();
      userSettingsController.dataLoading.restore();
    });
    it ('Should show settings popup', function() {
      userSettingsController.showSettingsPopup();
      dataToShowRes = JSON.parse(JSON.stringify(dataToShowRes));
      expect(dataToShowRes).to.eql({
        "header": "User Settings",
        "primary": "Save"
      });
    });
  });

  describe('#getUserPrefErrorCallback', function() {
    it ('Should set currentPrefObject', function() {
      applicationController.getUserPrefErrorCallback({status: 404}, {}, {});
      expect(applicationController.get('currentPrefObject')).to.be.true;
    });
  });

  describe('#getUserPrefSuccessCallback', function() {
    it ('Should set currentPrefObject', function() {
      applicationController.getUserPrefSuccessCallback({status: 200}, {}, {});
      expect(applicationController.get('currentPrefObject')).to.be.eql({status: 200});
    });
  });

  describe('#updateUserPrefWithDefaultValues', function () {

    beforeEach(function () {
      sinon.stub(userSettingsController, 'postUserPref', Em.K);
    });

    afterEach(function () {
      userSettingsController.postUserPref.restore();
    });

    it('should update user pref with default values', function () {
      userSettingsController.updateUserPrefWithDefaultValues(null, true);
      expect(userSettingsController.postUserPref.called).to.be.false;
    });

    it('should not update user pref with default values', function () {
      userSettingsController.updateUserPrefWithDefaultValues(null, false);
      expect(userSettingsController.postUserPref.called).to.be.true;
    });

  });

});