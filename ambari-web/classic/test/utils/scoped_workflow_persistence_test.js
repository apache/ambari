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
var persistence = require('utils/scoped_workflow_persistence');

function state(revision, workflow, values) {
  return {
    revision: revision,
    owner: workflow === 'IDLE' ? null : 'alice',
    workflow: workflow,
    phase: workflow === 'IDLE' ? 'IDLE' : 'SAVED',
    values: values || {}
  };
}

describe('classic scoped workflow persistence', function () {
  beforeEach(function () {
    persistence.clearForTests();
    App.db.cleanUp();
    App.db.setLoginName('alice');
    sinon.stub(App.router, 'get').withArgs('loginName').returns('alice');
  });

  afterEach(function () {
    App.router.get.restore();
    if (App.ajax.send.restore) {
      App.ajax.send.restore();
    }
    persistence.clearForTests();
    App.db.cleanUp();
    Object.keys(sessionStorage).filter(function (key) {
      return key.indexOf('ambari.workflow.') === 0;
    }).forEach(function (key) {
      sessionStorage.removeItem(key);
    });
  });

  it('refuses implicit recovery when more than one cluster is authorized', function () {
    sinon.stub(App.ajax, 'send').returns(resolved({
      items: [
        {Clusters: {cluster_id: 1, cluster_name: 'a'}},
        {Clusters: {cluster_id: 2, cluster_name: 'b'}}
      ]
    }));
    var error;

    persistence.loadCurrent().fail(function (value) {
      error = value;
    });

    expect(error.code).to.equal('CLASSIC_SCOPED_TARGET_REQUIRED');
    expect(App.ajax.send.calledOnce).to.be.true;
  });

  it('imports a sole-cluster legacy draft only with matching user and workflow ownership', function () {
    App.db.set('AddService', 'currentStep', 4);
    var updates = [];
    sinon.stub(App.ajax, 'send', function (options) {
      if (options.name === 'cluster.load_cluster_name') {
        return resolved({items: [{Clusters: {cluster_id: 7, cluster_name: 'c1'}}]});
      }
      if (options.name === 'persist.scoped.get') {
        return resolved(state(0, 'IDLE'));
      }
      if (options.name === 'persist.get') {
        return resolved({userName: 'alice', controllerName: 'addServiceController'});
      }
      if (options.name === 'persist.scoped.put') {
        updates.push(options.data.workflowState);
        return resolved(state(1, 'ADD_SERVICE', options.data.workflowState.values));
      }
      return rejected({status: 404});
    });
    var restored;

    persistence.loadCurrent().done(function (value) {
      restored = value;
    });

    expect(updates[0].expected_revision).to.equal(0);
    expect(updates[0].workflow).to.equal('ADD_SERVICE');
    expect(updates[0].values.CLUSTER_CURRENT_STATUS.clusterName).to.equal('c1');
    expect(updates[0].values.CLUSTER_CURRENT_STATUS.localdb.AddService.currentStep).to.equal(4);
    expect(restored.revision).to.equal(1);
  });

  it('pauses after a revision conflict and does not replay an old save after retry', function () {
    var puts = [];
    var currentState = state(2, 'ADD_SERVICE', {
      wizardData: {userName: 'alice', controllerName: 'addServiceController'}
    });
    sinon.stub(App.ajax, 'send', function (options) {
      if (options.name === 'cluster.load_cluster_name') {
        return resolved({items: [{Clusters: {cluster_id: 7, cluster_name: 'c1'}}]});
      }
      if (options.name === 'persist.scoped.get') {
        return resolved(currentState);
      }
      if (options.name === 'persist.scoped.put') {
        puts.push(options.data.workflowState);
        if (puts.length === 1) {
          return rejected({responseJSON: {code: 'WORKFLOW_VERSION_CONFLICT'}});
        }
        currentState = state(3, 'ADD_SERVICE', options.data.workflowState.values);
        return resolved(currentState);
      }
      return rejected({status: 404});
    });
    persistence.loadCurrent();
    persistence.setUser('addServiceController');
    persistence.setUser('addServiceController');
    expect(puts.length).to.equal(1);

    persistence.session.retryLoad();
    persistence.setUser('addServiceController');

    expect(puts.length).to.equal(2);
    expect(puts[1].expected_revision).to.equal(2);
  });

  it('ignores an in-flight save after retry reloads the authoritative revision', function () {
    var staleSave = $.Deferred();
    var puts = [];
    var currentState = state(2, 'ADD_SERVICE', {
      wizardData: {userName: 'alice', controllerName: 'addServiceController'}
    });
    sinon.stub(App.ajax, 'send', function (options) {
      if (options.name === 'cluster.load_cluster_name') {
        return resolved({items: [{Clusters: {cluster_id: 7, cluster_name: 'c1'}}]});
      }
      if (options.name === 'persist.scoped.get') {
        return resolved(currentState);
      }
      if (options.name === 'persist.scoped.put') {
        puts.push(options.data.workflowState);
        if (puts.length === 1) {
          return staleSave.promise();
        }
        currentState = state(3, 'ADD_SERVICE', options.data.workflowState.values);
        return resolved(currentState);
      }
      return rejected({status: 404});
    });
    persistence.loadCurrent();
    persistence.session.save('addServiceController', 'OLD_STEP', {oldSnapshot: true});

    persistence.session.retryLoad();
    staleSave.resolve(state(3, 'ADD_SERVICE', {oldSnapshot: true}));
    persistence.setUser('addServiceController');

    expect(puts.length).to.equal(2);
    expect(puts[1].expected_revision).to.equal(2);
    expect(puts[1].values.oldSnapshot).to.be.undefined;
  });

  it('settles every queued save after the first save fails', function () {
    var firstSave = $.Deferred();
    var puts = 0;
    sinon.stub(App.ajax, 'send', function (options) {
      if (options.name === 'cluster.load_cluster_name') {
        return resolved({items: [{Clusters: {cluster_id: 7, cluster_name: 'c1'}}]});
      }
      if (options.name === 'persist.scoped.get') {
        return resolved(state(2, 'ADD_SERVICE'));
      }
      if (options.name === 'persist.scoped.put') {
        puts++;
        return firstSave.promise();
      }
      return rejected({status: 404});
    });
    persistence.loadCurrent('addServiceController');
    var first = persistence.session.save('addServiceController', 'ONE', {step: 1});
    var second = persistence.session.save('addServiceController', 'TWO', {step: 2});
    var third = persistence.session.save('addServiceController', 'THREE', {step: 3});

    firstSave.reject({responseJSON: {code: 'WORKFLOW_VERSION_CONFLICT'}});

    expect(first.state()).to.equal('rejected');
    expect(second.state()).to.equal('rejected');
    expect(third.state()).to.equal('rejected');
    expect(puts).to.equal(1);
  });

  it('invalidates delayed work when switching from an installer draft to an installed workflow', function () {
    var draftLoad = $.Deferred();
    var installedSession;
    sinon.stub(App.ajax, 'send', function (options) {
      if (options.name === 'persist.scoped.get') {
        return draftLoad.promise();
      }
      if (options.name === 'cluster.load_cluster_name') {
        return resolved({items: [{Clusters: {cluster_id: 7, cluster_name: 'c1'}}]});
      }
      return rejected({status: 404});
    });
    var staleLoad = persistence.loadCurrent('installerController');

    persistence.resolveSession('addServiceController').done(function (session) {
      installedSession = session;
    });
    draftLoad.resolve(state(0, 'IDLE'));

    expect(staleLoad.state()).to.equal('rejected');
    expect(installedSession.scope).to.eql({type: 'clusters', id: '7'});
    expect(installedSession.controllerName).to.equal('addServiceController');
  });

  it('does not restore a delayed prior-controller response into the active workflow', function () {
    var staleSave = $.Deferred();
    var putCount = 0;
    sinon.stub(App.ajax, 'send', function (options) {
      if (options.name === 'cluster.load_cluster_name') {
        return resolved({items: [{Clusters: {cluster_id: 7, cluster_name: 'c1'}}]});
      }
      if (options.name === 'persist.scoped.get') {
        return resolved(state(2, 'ADD_SERVICE'));
      }
      if (options.name === 'persist.scoped.put') {
        putCount++;
        return staleSave.promise();
      }
      return rejected({status: 404});
    });
    persistence.loadCurrent('addServiceController');
    var stale = persistence.session.save('addServiceController', 'OLD', {});
    persistence.resolveSession('reassignMasterController');
    App.db.set('ReassignMaster', 'currentStep', 2);

    staleSave.resolve(state(3, 'ADD_SERVICE', {
      CLUSTER_CURRENT_STATUS: {localdb: {ReassignMaster: {currentStep: 9}}}
    }));

    expect(stale.state()).to.equal('rejected');
    expect(putCount).to.equal(1);
    expect(App.db.get('ReassignMaster', 'currentStep')).to.equal(2);
  });

  it('keeps a live installer SSH key after a redacted checkpoint acknowledgement', function () {
    var revision = 0;
    sinon.stub(App.ajax, 'send', function (options) {
      if (options.name === 'persist.scoped.get') {
        return resolved(state(revision, revision ? 'CLUSTER_CREATE' : 'IDLE'));
      }
      if (options.name === 'persist.scoped.put') {
        revision++;
        return resolved(state(revision, options.data.workflowState.workflow, options.data.workflowState.values));
      }
      return rejected({status: 404});
    });
    persistence.loadCurrent('installerController');
    App.db.set('Installer', 'installOptions', {sshKey: 'private material'});

    persistence.saveStatus({
      clusterName: 'c1',
      clusterState: 'CLUSTER_DEPLOY_PREP_2',
      wizardControllerName: 'installerController'
    });

    expect(App.db.get('Installer', 'installOptions').sshKey).to.equal('private material');
    expect(JSON.stringify(persistence.session.values)).to.not.contain('private material');
  });

  it('keeps Widget drafts in the verified browser scope without a server workflow', function () {
    sinon.stub(App.ajax, 'send', function (options) {
      if (options.name === 'cluster.load_cluster_name') {
        return resolved({items: [{Clusters: {cluster_id: 7, cluster_name: 'c1'}}]});
      }
      return rejected({status: 500});
    });

    persistence.setUser('widgetWizardController');
    App.db.set('WidgetWizard', 'widgetName', 'Capacity');
    persistence.saveStatus({
      clusterName: 'c1',
      clusterState: 'WIDGET_DEPLOY',
      wizardControllerName: 'widgetWizardController'
    });

    expect(persistence.session.scope).to.eql({type: 'clusters', id: '7'});
    expect(App.db.getWorkflowStorageScope()).to.equal(persistence.session.browserScope);
    expect(App.db.get('WidgetWizard', 'widgetName')).to.equal('Capacity');
    expect(App.ajax.send.calledWithMatch({name: 'persist.scoped.put'})).to.be.false;
  });

  it('releases the exact loaded workflow revision without clearing another scope', function () {
    var released;
    sinon.stub(App.ajax, 'send', function (options) {
      if (options.name === 'cluster.load_cluster_name') {
        return resolved({items: [{Clusters: {cluster_id: 7, cluster_name: 'c1'}}]});
      }
      if (options.name === 'persist.scoped.get') {
        return resolved(state(4, 'ADD_SERVICE', {
          wizardData: {userName: 'alice', controllerName: 'addServiceController'}
        }));
      }
      if (options.name === 'persist.scoped.put') {
        released = options.data;
        return resolved(state(5, 'IDLE'));
      }
      return rejected({status: 404});
    });

    persistence.loadCurrent();
    persistence.release();

    expect(released.scopeType).to.equal('clusters');
    expect(released.scopeId).to.equal('7');
    expect(released.workflowState).to.eql({
      expected_revision: 4,
      workflow: 'IDLE',
      phase: 'IDLE',
      values: {}
    });
  });

  it('routes recovered credentials to their input step and blocks deployment until re-entry', function () {
    var puts = [];
    sinon.stub(App.ajax, 'send', function (options) {
      if (options.name === 'cluster.load_cluster_name') {
        return resolved({items: [{Clusters: {cluster_id: 7, cluster_name: 'c1'}}]});
      }
      if (options.name === 'persist.scoped.get') {
        return resolved(state(3, 'ENABLING_KERBEROS', {
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
              }
            }
          },
          wizardData: {userName: 'alice', controllerName: 'kerberosWizardController'}
        }));
      }
      if (options.name === 'persist.scoped.put') {
        puts.push(options.data.workflowState);
        return resolved(state(4, options.data.workflowState.workflow, options.data.workflowState.values));
      }
      return rejected({status: 404});
    });
    var blocked;

    persistence.loadCurrent('kerberosWizardController');
    persistence.saveStatus({
      clusterName: 'c1',
      clusterState: 'ADD_SECURITY_STEP_3',
      wizardControllerName: 'kerberosWizardController'
    }).fail(function (error) {
      blocked = error;
    });

    expect(App.db.get('KerberosWizard', 'currentStep')).to.equal(2);
    expect(persistence.requiresReentry()).to.be.true;
    expect(blocked.code).to.equal('WORKFLOW_REENTRY_REQUIRED');
    expect(puts).to.be.empty;

    App.db.set('KerberosWizard', 'serviceConfigProperties', [{
      name: 'admin_password',
      value: 'new-password',
      requires_reentry: true
    }]);
    persistence.saveStatus({
      clusterName: 'c1',
      clusterState: 'ADD_SECURITY_STEP_3',
      wizardControllerName: 'kerberosWizardController'
    });

    expect(puts).to.have.length(1);
    expect(JSON.stringify(puts[0].values)).to.not.contain('new-password');
  });
});

function resolved(value) {
  return $.Deferred().resolve(value).promise();
}

function rejected(value) {
  return $.Deferred().reject(value).promise();
}
