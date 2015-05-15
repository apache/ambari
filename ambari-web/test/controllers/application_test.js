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
require('models/cluster');

describe('App.ApplicationController', function () {

  var installerController = App.ApplicationController.create();

  describe('#showAboutPopup', function() {
    var dataToShowRes = {};
    beforeEach(function () {
      sinon.stub(App.ModalPopup, 'show', function(dataToShow){
        dataToShowRes = dataToShow;
      });
    });
    afterEach(function () {
      App.ModalPopup.show.restore();
    });
    it ('Should send correct data to popup', function() {
      installerController.showAboutPopup();
      dataToShowRes = JSON.parse(JSON.stringify(dataToShowRes));
      expect(dataToShowRes).to.eql({
        "header": "About",
        "secondary": false
      });
    });
  });

  describe('#clusterName', function() {
    beforeEach(function () {
      sinon.stub(App.router, 'get').returns('cl1');
    });
    afterEach(function () {
      App.router.get.restore();
    });
    it ('Should return cluster name', function() {
      expect(installerController.get('clusterName')).to.equal('cl1');
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
      sinon.stub(installerController, 'dataLoading').returns(emulator);
    });
    afterEach(function () {
      App.isAccessible.restore();
      App.ModalPopup.show.restore();
      installerController.dataLoading.restore();
    });
    it ('Should show settings popup', function() {
      installerController.showSettingsPopup();
      dataToShowRes = JSON.parse(JSON.stringify(dataToShowRes));
      expect(dataToShowRes).to.eql({
        "header": "User Settings",
        "primary": "Save"
      });
    });
  });

  describe('#startKeepAlivePoller', function() {
    it ('Should change run poller state', function() {
      installerController.set('isPollerRunning', false);
      installerController.startKeepAlivePoller();
      expect(installerController.get('isPollerRunning')).to.be.true;
    });
  });

  describe('#getUserPrefErrorCallback', function() {
    it ('Should set currentPrefObject', function() {
      installerController.getUserPrefErrorCallback({status: 404}, {}, {});
      expect(installerController.get('currentPrefObject')).to.be.true;
    });
  });

  describe('#getUserPrefSuccessCallback', function() {
    it ('Should set currentPrefObject', function() {
      installerController.getUserPrefSuccessCallback({status: 200}, {}, {});
      expect(installerController.get('currentPrefObject')).to.be.eql({status: 200});
    });
  });

  describe('#goToAdminView', function() {
    var result;
    beforeEach(function () {
      sinon.stub(App.router, 'route', function(data) {
        result = data;
        return false;
      });
    });
    afterEach(function () {
      App.router.route.restore();
    });
    it ('Should call route once', function() {
      installerController.goToAdminView();
      expect(result).to.be.equal('adminView');
    });
  });

  describe('#dataLoading', function() {
    beforeEach(function () {
      sinon.stub(installerController, 'getUserPref', function(){
        return {
          complete: function(func) {
            if (func) {
              func();
            }
          }
        };
      });
    });
    afterEach(function () {
      installerController.getUserPref.restore();
    });
    it ('Should change run poller state', function() {
      installerController.set('currentPrefObject', {name: 'n1'});
      installerController.dataLoading().then(function(data){
        expect(data).to.be.eql({
          "name": "n1"
        });
      });
    });
  });

  describe('#getStack', function() {
    var res;
    beforeEach(function () {
      sinon.stub(App.ajax, 'send', function(data) {
        res = data;
      });
    });
    afterEach(function () {
      App.ajax.send.restore();
    });
    it ('Should return send value', function() {
      var callback = {
        'callback': true
      };
      installerController.getStack(callback);
      res = JSON.parse(JSON.stringify(res));
      expect(res).to.be.eql({
        "name": "router.login.clusters",
        "sender": {
          "isPollerRunning": true,
          "currentPrefObject": null
        },
        "callback": {
          "callback": true
        }
      });
    });
  });

  describe('#clusterDisplayName', function() {
    it ('Should return cluster display name', function() {
      installerController.set('clusterName', '');
      expect(installerController.get('clusterDisplayName')).to.equal('mycluster');
    });
  });

  describe('#isClusterDataLoaded', function() {
    beforeEach(function () {
      sinon.stub(App.router, 'get').returns('cl1');
    });
    afterEach(function () {
      App.router.get.restore();
    });
    it ('Should return true, when data loaded', function() {
      expect(installerController.get('isClusterDataLoaded')).to.be.equal('cl1');
    });
  });

});
