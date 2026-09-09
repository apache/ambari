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
var scopedWorkflowPersistence = require('utils/scoped_workflow_persistence');
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

  App.TestAliases.testAsComputedNotExistsIn(status, 'isInstalled', 'clusterState', notInstalledStates);

  describe('#value', function () {
    it('should be set from properties', function () {
      Em.keys(values).forEach(function (key) {
        status.set(key, values[key]);
      });
      expect(status.get('value')).to.eql(values);
    });
  });

  describe('#getUserPrefSuccessCallback', function () {
    describe('response', function () {
      beforeEach(function () {
        status.getUserPrefSuccessCallback(response);
      });
      Em.keys(response).forEach(function (key) {
        it(key, function () {
          expect(status.get(key)).to.equal(response[key]);
        });
      });
    });
    describe('compressedResponse', function () {
      beforeEach(function () {
        status.getUserPrefSuccessCallback(compressedResponse);
      });
      Em.keys(response2).forEach(function (key) {
        it(key, function () {
          expect(status.get(key)).to.equal(response2[key]);
        });
      });
    });
  });

  describe('#updateFromServer scoped recovery', function () {
    beforeEach(function () {
      scopedWorkflowPersistence.clearForTests();
      App.db.cleanUp();
      App.db.setLoginName('alice');
      sinon.stub(App.router, 'get').withArgs('loginName').returns('alice');
    });

    afterEach(function () {
      App.router.get.restore();
      if (App.ajax.send.restore) {
        App.ajax.send.restore();
      }
      scopedWorkflowPersistence.clearForTests();
      App.db.cleanUp();
    });

    it('keeps the adjusted re-entry step and ignores inactive namespaces', function () {
      sinon.stub(App.ajax, 'send', function (options) {
        if (options.name === 'cluster.load_cluster_name') {
          return $.Deferred().resolve({
            items: [{Clusters: {cluster_id: 7, cluster_name: 'c1'}}]
          }).promise();
        }
        if (options.name === 'persist.scoped.get') {
          return $.Deferred().resolve({
            revision: 3,
            owner: 'alice',
            workflow: 'ENABLING_KERBEROS',
            phase: 'ADD_SECURITY_STEP_3',
            values: {
              wizardData: {
                userName: 'alice',
                controllerName: 'kerberosWizardController'
              },
              CLUSTER_CURRENT_STATUS: {
                clusterName: 'c1',
                clusterState: 'ADD_SECURITY_STEP_3',
                wizardControllerName: 'kerberosWizardController',
                localdb: {
                  KerberosWizard: {
                    currentStep: 7,
                    serviceConfigProperties: [{
                      name: 'admin_password',
                      requires_reentry: true
                    }]
                  },
                  AddService: {currentStep: 9}
                }
              }
            }
          }).promise();
        }
        return $.Deferred().reject({status: 404}).promise();
      });
      status.set('wizardControllerName', 'kerberosWizardController');

      status.updateFromServer(false);

      expect(App.db.get('KerberosWizard', 'currentStep')).to.equal(2);
      expect(App.db.get('AddService', 'currentStep')).to.be.undefined;
      expect(status.get('localdb').KerberosWizard.currentStep).to.equal(2);
      expect(status.get('localdb').AddService).to.be.undefined;
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
    });

    it('should set cluster status in non-test mode', function () {
      var clusterStatus = status.setClusterStatus(newValue);
      expect(clusterStatus).to.eql(newValue);
    });

  });

});
