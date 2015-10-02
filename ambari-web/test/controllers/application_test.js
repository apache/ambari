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

  var applicationController = App.ApplicationController.create();

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
      applicationController.showAboutPopup();
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
      expect(applicationController.get('clusterName')).to.equal('cl1');
    });
  });



  describe('#startKeepAlivePoller', function() {
    it ('Should change run poller state', function() {
      applicationController.set('isPollerRunning', false);
      applicationController.startKeepAlivePoller();
      expect(applicationController.get('isPollerRunning')).to.be.true;
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
      applicationController.goToAdminView();
      expect(result).to.be.equal('adminView');
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
      applicationController.getStack(callback);
      res = JSON.parse(JSON.stringify(res));
      expect(res).to.be.eql({
        "name": "router.login.clusters",
        "sender": {
          "isPollerRunning": true
        },
        "callback": {
          "callback": true
        }
      });
    });
  });

  describe('#clusterDisplayName', function() {
    it ('Should return cluster display name', function() {
      applicationController.set('clusterName', '');
      expect(applicationController.get('clusterDisplayName')).to.equal('mycluster');
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
      expect(applicationController.get('isClusterDataLoaded')).to.be.equal('cl1');
    });
  });

});
