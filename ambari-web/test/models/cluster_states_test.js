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
var LZString = require('utils/lz-string');
require('models/cluster_states');

var status = App.clusterStatus,
  notInstalledStates = ['CLUSTER_NOT_CREATED_1', 'CLUSTER_DEPLOY_PREP_2', 'CLUSTER_INSTALLING_3', 'SERVICE_STARTING_3'],
  values = {
    clusterName: 'name',
    clusterState: 'STACK_UPGRADING',
    wizardControllerName: 'wizardStep0Controller',
    localdb: {}
  },
  response = {
    clusterState: 'DEFAULT',
    clusterName: 'cluster'
  },
  response2 = {
    clusterState: 'DEFAULT2',
    clusterName: 'cluster2'
  },
  newValue = {
    clusterName: 'name',
    clusterState: 'STACK_UPGRADING',
    wizardControllerName: 'wizardStep0Controller'
  };
var compressedResponse = LZString.compressToBase64(JSON.stringify(response2));

describe('App.clusterStatus', function () {

  describe('#isInstalled', function () {
    notInstalledStates.forEach(function (item) {
      it('should be false', function () {
        status.set('clusterState', item);
        expect(status.get('isInstalled')).to.be.false;
      });
    });
    it('should be true', function () {
      status.set('clusterState', 'DEFAULT');
      expect(status.get('isInstalled')).to.be.true;
    });
  });

  describe('#value', function () {
    it('should be set from properties', function () {
      Em.keys(values).forEach(function (key) {
        status.set(key, values[key]);
      });
      expect(status.get('value')).to.eql(values);
    });
  });

  describe('#getUserPrefSuccessCallback', function () {
    it('should set the cluster parameters', function () {
      status.getUserPrefSuccessCallback(response);
      Em.keys(response).forEach(function (key) {
        expect(status.get(key)).to.equal(response[key]);
      });
      status.getUserPrefSuccessCallback(compressedResponse);
      Em.keys(response2).forEach(function (key) {
        expect(status.get(key)).to.equal(response2[key]);
      });
    });
  });

  describe('#setClusterStatus', function () {

    beforeEach(function() {
      sinon.stub(status, 'postUserPref', function() {
        return $.ajax();
      });
    });

    afterEach(function () {
      status.postUserPref.restore();
      App.get.restore();
    });

    it('should return false in test mode', function () {
      sinon.stub(App, 'get', function(k) {
        if (k === 'testMode') return true;
        return Em.get(App, k);
      });
      expect(status.setClusterStatus()).to.be.false;
    });

    it('should set cluster status in non-test mode', function () {
      sinon.stub(App, 'get', function(k) {
        if (k === 'testMode') return false;
        return Em.get(App, k);
      });
      var clusterStatus = status.setClusterStatus(newValue);
      expect(clusterStatus).to.eql(newValue);
    });

  });

});
