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
require('controllers/wizard');

var c;

describe('App.WizardController', function () {

  var wizardController = App.WizardController.create({});

  var totalSteps = 11;
  var ruller = [];
  for(var i = 0; i < totalSteps; i++) {
    ruller.push(i);
  }

  beforeEach(function () {
    c = App.WizardController.create({});
  });

  describe('#setLowerStepsDisable', function() {
    for(var i = 1; i < totalSteps; i++) {
      var indx = i;
      var steps = [];
      for(var j = 1; j <= indx; j++) {
        steps.push(Em.Object.create({step:j,value:false}));
      }
      wizardController.set('isStepDisabled', steps);
      for(j = 1; j <= indx; j++) {
        it('Steps: ' + i + ' | Disabled: ' + (j-1), function() {
          wizardController.setLowerStepsDisable(j);
          expect(wizardController.get('isStepDisabled').filterProperty('value', true).length).to.equal(j-1);
        });
      }
    }
  });

  // isStep0 ... isStep10 tests
  App.WizardController1 = App.WizardController.extend({currentStep:''});
  var tests = [];
  for(var i = 0; i < totalSteps; i++) {
    var n = ruller.slice(0);
    n.splice(i,1);
    tests.push({i:i,n:n});
  }
  tests.forEach(function(test) {
    describe('isStep'+test.i, function() {
      var w = App.WizardController1.create();
      w.set('currentStep', test.i);
      it('Current Step is ' + test.i + ', so isStep' + test.i + ' is TRUE', function() {
        expect(w.get('isStep'+ test.i)).to.equal(true);
      });
      test.n.forEach(function(indx) {
        it('Current Step is ' + test.i + ', so isStep' + indx + ' is FALSE', function() {
          expect(w.get('isStep'+ indx)).to.equal(false);
        });
      });
    });
  });
  // isStep0 ... isStep10 tests end

  describe('#gotoStep', function() {
    var w = App.WizardController1.create();
    var steps = [];
    for(var j = 0; j < totalSteps; j++) {
      steps.push(Em.Object.create({step:j,value:false}));
    }
    steps.forEach(function(step, index) {
      step.set('value', true);
      w.set('isStepDisabled', steps);
      it('step ' + index + ' is disabled, so gotoStep('+index+') is not possible', function() {
        expect(w.gotoStep(index)).to.equal(false);
      });
    });
  });

  describe('#launchBootstrapSuccessCallback', function() {
    it('Save bootstrapRequestId', function() {
      var data = {requestId: 123};
      var params = {popup: {finishLoading: function(){}}};
      sinon.spy(params.popup, "finishLoading");
      wizardController.launchBootstrapSuccessCallback(data, {}, params);
      expect(params.popup.finishLoading.calledWith(123)).to.be.true;
      params.popup.finishLoading.restore();
    });
  });

  describe('#getInstallOptions', function () {

    var cases = [
        {
          isHadoopWindowsStack: true,
          expected: {
            useSsh: false
          }
        },
        {
          isHadoopWindowsStack: false,
          expected: {
            useSsh: true
          }
        }
      ],
      title = 'should return {0}';

    beforeEach(function () {
      sinon.stub(wizardController, 'get')
        .withArgs('installOptionsTemplate').returns({useSsh: true})
        .withArgs('installWindowsOptionsTemplate').returns({useSsh: false});
    });

    afterEach(function () {
      App.get.restore();
      wizardController.get.restore();
    });

    cases.forEach(function (item) {
      it(title.format(item.expected), function () {
        sinon.stub(App, 'get').withArgs('isHadoopWindowsStack').returns(item.isHadoopWindowsStack);
        expect(wizardController.getInstallOptions()).to.eql(item.expected);
      });
    });

  });

  describe('#clearInstallOptions', function () {

    wizardController.setProperties({
      content: {},
      name: 'wizard'
    });

    beforeEach(function () {
      sinon.stub(App, 'get').withArgs('isHadoopWindowsStack').returns(false);
    });

    afterEach(function () {
      App.get.restore();
    });

    it('should clear install options', function () {
      wizardController.clearInstallOptions();
      expect(wizardController.get('content.installOptions')).to.eql(wizardController.get('installOptionsTemplate'));
      expect(wizardController.get('content.hosts')).to.eql({});
      expect(wizardController.getDBProperty('installOptions')).to.eql(wizardController.get('installOptionsTemplate'));
      expect(wizardController.getDBProperty('hosts')).to.eql({});
    });
  });

  describe('#loadServiceConfigGroups', function () {
     beforeEach(function () {
      sinon.stub(wizardController, 'getDBProperties', function() {
        return {
          serviceConfigGroups: [
            {
              hosts: ['h1']
            }
          ],
          hosts: Em.Object.create({
            h1: Em.Object.create({
              id: 'h1'
            })
          })
        };
      });
    });
    afterEach(function () {
      wizardController.getDBProperties.restore();
    });
    it('should load service confgig group', function () {
      wizardController.loadServiceConfigGroups();
      expect(wizardController.get('content.configGroups')).to.eql([
        {
          "hosts": [
            "h1"
          ]
        }
      ]);
    });
  });

  describe('#saveTasksStatuses', function () {
    it('should set status', function () {
      wizardController.saveTasksStatuses('st');
      expect(wizardController.get('content.tasksStatuses')).to.equal('st');
    });
  });

  describe('#saveSlaveComponentHosts', function () {
    beforeEach(function(){
      sinon.stub(wizardController,'getDBProperty').returns(Em.A({
        'h1': {
          id: 1
        }
      }));
    });
    afterEach(function(){
      wizardController.getDBProperty.restore();
    });
    it('should save slave components', function () {
      var stepController = Em.Object.create({
        hosts: Em.A([
          Em.Object.create({
            hostName: 'h1',
            checkboxes: Em.A([
              Em.Object.create({title: 'hl1', checked: true})
            ])
          })
        ]),
        headers: Em.A([
          Em.Object.create({name: 'header1', label: 'hl1'})
        ])
      });
      wizardController.saveSlaveComponentHosts(stepController);
      var res = JSON.parse(JSON.stringify(wizardController.get('content.slaveComponentHosts')));
      expect(res).to.eql([
        {
          "componentName": "header1",
          "displayName": "hl1",
          "hosts": [
            {
              "group": "Default",
              "host_id": 1
            }
          ]
        }
      ]);
    });
  });

  describe('#showLaunchBootstrapPopup', function () {
    beforeEach(function(){
      sinon.stub(App.ModalPopup,'show', function(data){
        data.finishLoading.call(wizardController);
      });
    });
    afterEach(function(){
      App.ModalPopup.show.restore();
    });
    it('should set error', function () {
      wizardController.showLaunchBootstrapPopup(Em.K);
      expect(wizardController.get('isError')).to.be.true;
    });
  });

  describe('#gotoStep0', function () {
    var res;
    beforeEach(function(){
      sinon.stub(wizardController,'gotoStep', function(step){
        res = step;
      });
    });
    afterEach(function(){
      wizardController.gotoStep.restore();
    });
    it('should go to 0 step', function () {
      wizardController.gotoStep0(Em.K);
      expect(res).to.be.equal(0);
    });
  });

  describe('#gotoStep1', function () {
    var res;
    beforeEach(function(){
      sinon.stub(wizardController,'gotoStep', function(step){
        res = step;
      });
    });
    afterEach(function(){
      wizardController.gotoStep.restore();
    });
    it('should go to 1 step', function () {
      wizardController.gotoStep1(Em.K);
      expect(res).to.be.equal(1);
    });
  });

  describe('#gotoStep2', function () {
    var res;
    beforeEach(function(){
      sinon.stub(wizardController,'gotoStep', function(step){
        res = step;
      });
    });
    afterEach(function(){
      wizardController.gotoStep.restore();
    });
    it('should go to 2 step', function () {
      wizardController.gotoStep2(Em.K);
      expect(res).to.be.equal(2);
    });
  });

  describe('#gotoSte3', function () {
    var res;
    beforeEach(function(){
      sinon.stub(wizardController,'gotoStep', function(step){
        res = step;
      });
    });
    afterEach(function(){
      wizardController.gotoStep.restore();
    });
    it('should go to 3 step', function () {
      wizardController.gotoStep3(Em.K);
      expect(res).to.be.equal(3);
    });
  });

  describe('#gotoStep4', function () {
    var res;
    beforeEach(function(){
      sinon.stub(wizardController,'gotoStep', function(step){
        res = step;
      });
    });
    afterEach(function(){
      wizardController.gotoStep.restore();
    });
    it('should go to 4 step', function () {
      wizardController.gotoStep4(Em.K);
      expect(res).to.be.equal(4);
    });
  });

  describe('#gotoStep5', function () {
    var res;
    beforeEach(function(){
      sinon.stub(wizardController,'gotoStep', function(step){
        res = step;
      });
    });
    afterEach(function(){
      wizardController.gotoStep.restore();
    });
    it('should go to 5 step', function () {
      wizardController.gotoStep5(Em.K);
      expect(res).to.be.equal(5);
    });
  });

  describe('#gotoStep6', function () {
    var res;
    beforeEach(function(){
      sinon.stub(wizardController,'gotoStep', function(step){
        res = step;
      });
    });
    afterEach(function(){
      wizardController.gotoStep.restore();
    });
    it('should go to 6 step', function () {
      wizardController.gotoStep6(Em.K);
      expect(res).to.be.equal(6);
    });
  });

  describe('#gotoStep7', function () {
    var res;
    beforeEach(function(){
      sinon.stub(wizardController,'gotoStep', function(step){
        res = step;
      });
    });
    afterEach(function(){
      wizardController.gotoStep.restore();
    });
    it('should go to 7 step', function () {
      wizardController.gotoStep7(Em.K);
      expect(res).to.be.equal(7);
    });
  });

  describe('#gotoStep8', function () {
    var res;
    beforeEach(function(){
      sinon.stub(wizardController,'gotoStep', function(step){
        res = step;
      });
    });
    afterEach(function(){
      wizardController.gotoStep.restore();
    });
    it('should go to 8 step', function () {
      wizardController.gotoStep8(Em.K);
      expect(res).to.be.equal(8);
    });
  });

  describe('#gotoStep9', function () {
    var res;
    beforeEach(function(){
      sinon.stub(wizardController,'gotoStep', function(step){
        res = step;
      });
    });
    afterEach(function(){
      wizardController.gotoStep.restore();
    });
    it('should go to 9 step', function () {
      wizardController.gotoStep9(Em.K);
      expect(res).to.be.equal(9);
    });
  });

  describe('#gotoStep10', function () {
    var res;
    beforeEach(function(){
      sinon.stub(wizardController,'gotoStep', function(step){
        res = step;
      });
    });
    afterEach(function(){
      wizardController.gotoStep.restore();
    });
    it('should go to 10 step', function () {
      wizardController.gotoStep10(Em.K);
      expect(res).to.be.equal(10);
    });
  });

  describe('#gotoStep', function () {
    beforeEach(function(){
      sinon.stub(App.ModalPopup,'show', Em.K);
      sinon.stub(App.clusterStatus,'setClusterStatus', Em.K);  
      sinon.stub(App.router,'send', Em.K);  
    });
    afterEach(function(){
      App.ModalPopup.show.restore();
      App.clusterStatus.setClusterStatus.restore();
      App.router.send.restore();
    });
    it('should go to step', function () {
      wizardController.set('isStepDisabled', Em.A([
        Em.Object.create({
          step: '8',
          value: false
        })
      ]));
      wizardController.hide = Em.K;
      wizardController.set('content.controllerName','installerController');
      wizardController.set('currentStep','9');

      expect(wizardController.gotoStep('8')).to.be.true;
    });
  });

  describe('#launchBootstrap', function () {
    beforeEach(function(){
      sinon.stub(wizardController,'showLaunchBootstrapPopup').returns({
        name: 'popup'
      });
      sinon.stub(App.ajax,'send', Em.K);
    });
    afterEach(function(){
      wizardController.showLaunchBootstrapPopup.restore();
      App.ajax.send.restore();
    });
    it('should return popup', function () {
      expect(wizardController.launchBootstrap()).to.be.eql({
        name: 'popup'
      });
    });
  });

  describe('#save', function () {
    it('should save data', function () {
      var res;
      sinon.stub(wizardController,'setDBProperty', function(data){
        res = data;
      });
      sinon.stub(wizardController,'toJSInstance').returns('val');
      wizardController.save('name');
      wizardController.setDBProperty.restore();
      wizardController.toJSInstance.restore();
      expect(res).to.be.equal('name');
    });
  });

  describe('#installServicesSuccessCallback', function () {
    var res;
    beforeEach(function(){
      sinon.stub(wizardController,'saveClusterStatus', function(data){
        res = JSON.parse(JSON.stringify(data));
      });
      sinon.stub(App,'dateTime').returns('22');
    });
    afterEach(function(){
      wizardController.saveClusterStatus.restore();
      App.dateTime.restore();
    });
    it('should call callbeck with data', function () {
      var jsonData = {
        Requests: {
          id: 1
        }
      };
      wizardController.installServicesSuccessCallback(jsonData);
      expect(res).to.be.eql({
        "status": "PENDING",
        "requestId": 1,
        "isInstallError": false,
        "isCompleted": false,
        "installStartTime": "22"
      });
    });
  });

  describe('#installServices', function () {
    var res;
    beforeEach(function(){
      sinon.stub(wizardController,'saveClusterStatus', function(data){
        res = JSON.parse(JSON.stringify(data));
      });
      sinon.stub(App.ajax,'send').returns({
        then: function() {}
      });
    });
    afterEach(function(){
      wizardController.saveClusterStatus.restore();
      App.ajax.send.restore();
    });
    it('should call callbeck with data', function () {
      var jsonData = {
        Requests: {
          id: 1
        }
      };
      wizardController.set('content', Em.Object.create({
        cluster: {
          oldRequestsId: '1'
        }
      }));
      wizardController.installServices(true);
      expect(res).to.be.eql({
        "status": "PENDING"
      });
    });
  });

  describe('#saveInstalledHosts', function () {
    beforeEach(function(){
      sinon.stub(wizardController,'getDBProperty').returns({
        'h1': {
          id: 1,
          status: '',
          name: 'h1'
        }
      });
    });
    afterEach(function(){
      wizardController.getDBProperty.restore();
    });
    it('should save installed hosts', function () {
      var stepController = Em.Object.create({
        hosts: Em.A([
          Em.Object.create({
            hostName: 'h1',
            name: 'h1',
            status: 'st',
            message: 'ms',
            checkboxes: Em.A([
              Em.Object.create({title: 'hl1', checked: true})
            ])
          })
        ])
      });
      wizardController.saveInstalledHosts(stepController);
      var res = JSON.parse(JSON.stringify(wizardController.get('content.hosts')));
      expect(res).to.eql({
        "h1": {
          "id": 1,
          "status": "st",
          "name": "h1",
          "message": "ms"
        }
      });
    });
  });

  describe('#saveConfirmedHosts', function () {
    beforeEach(function(){
      sinon.stub(wizardController,'getDBProperty').returns({
        'h1': {
          id: 1,
          status: '',
          name: 'h1'
        }
      });
    });
    afterEach(function(){
      wizardController.getDBProperty.restore();
    });
    it('should save confirmed hosts', function () {
      var stepController = Em.Object.create({
        confirmedHosts: Em.A([
          {
            name: 'h2',
            cpu: '1',
            isInstalled: true
          }
        ])
      });
      wizardController.set('content.hosts', {
        'h1': {
          isInstalled: false,
          bootStatus: 'REGISTERED'
        },
        'h2': {
          isInstalled: true,
          bootStatus: 'REGISTERED'
        }
      });
      wizardController.saveConfirmedHosts(stepController);
      var res = JSON.parse(JSON.stringify(wizardController.get('content.hosts')));
      expect(res).to.eql({
        "h2": {
          "isInstalled": true,
          "bootStatus": "REGISTERED"
        }
      });
    });
  });

  describe('#loadTasksStatuses', function () {
    beforeEach(function () {
      sinon.stub(wizardController, 'getDBProperty').returns('st');
    });
    afterEach(function () {
      wizardController.getDBProperty.restore();
    });
    it('should load status', function () {
      wizardController.loadTasksStatuses();
      expect(wizardController.get('content.tasksStatuses')).to.equal('st');
    });
  });

  describe('#saveTasksRequestIds', function () {
    it('should save id', function () {
      wizardController.saveTasksRequestIds('st');
      expect(wizardController.get('content.tasksRequestIds')).to.equal('st');
    });
  });

  describe('#loadTasksRequestIds', function () {
    beforeEach(function () {
      sinon.stub(wizardController, 'getDBProperty').returns('st');
    });
    afterEach(function () {
      wizardController.getDBProperty.restore();
    });
    it('should load status', function () {
      wizardController.loadTasksRequestIds();
      expect(wizardController.get('content.tasksRequestIds')).to.equal('st');
    });
  });

  describe('#saveRequestIds', function () {
    it('should save id', function () {
      wizardController.saveRequestIds('st');
      expect(wizardController.get('content.requestIds')).to.equal('st');
    });
  });

  describe('#load', function () {
    it('should clear install options', function () {
      var name = 'Name';
      wizardController.set('get'+name.capitalize(), function() {return 'res';});
      wizardController.load(name, true);
      expect(wizardController.get('content.' + name)).to.equal('res');
    });
  });

  describe('#usersLoading', function () {
    beforeEach(function(){
      sinon.stub(App.MainAdminServiceAccountsController,'create').returns({
        loadUsers: function() {},
        get: function(type) {
          if (type == 'dataIsLoaded') {
            return true;
          }
          return Em.Object.create({
            hdfsUser: {
              name: 'user'
            }
          });
        }
      });
    });
    afterEach(function(){
      App.MainAdminServiceAccountsController.create.restore();
    });
    it('should load users', function () {
      wizardController.set('content.hdfsUser', true);
      wizardController.usersLoading().then(function(data){
        expect(data).to.be.undefined;
      });
    });
  });

  describe('#loadConfirmedHosts', function () {
    beforeEach(function(){
      sinon.stub(App.db, 'getHosts').returns(Em.A([
        Em.Object.create({
          name: 'h1'
        })
      ]));
    });
    afterEach(function(){
      App.db.getHosts.restore();
    });
    it('should load hosts from db', function () {
      wizardController.loadConfirmedHosts();
      var res = JSON.parse(JSON.stringify(wizardController.get('content.hosts')));
      expect(res).to.eql([
        {
          "name": "h1"
        }
      ]);
    });
  });

  describe('#loadServicesFromServer', function () {//TODO
    var res;
    beforeEach(function(){
      sinon.stub(App.StackService, 'find').returns(Em.A([
        Em.Object.create({
          isSelected: false,
          isInstalled: false,
          serviceName: 's1'
        })
      ]));
      sinon.stub(App.Service, 'find').returns(Em.A([
        Em.Object.create({
          isSelected: false,
          isInstalled: false,
          serviceName: 's1'
        })
      ]));
      sinon.stub(wizardController, 'setDBProperty', function(data) {
        res = data;
      });
    });
    
    afterEach(function () {
      App.StackService.find.restore();
      App.Service.find.restore();
      wizardController.setDBProperty.restore();
    });
    it('should load services from server', function () {
      wizardController.loadServicesFromServer();
      expect(res).to.eql('services');
    });
  });

  describe('#loadRequestIds', function () {
    beforeEach(function () {
      sinon.stub(wizardController, 'getDBProperty').returns('st');
    });
    afterEach(function () {
      wizardController.getDBProperty.restore();
    });
    it('should load status', function () {
      wizardController.loadRequestIds();
      expect(wizardController.get('content.requestIds')).to.equal('st');
    });
  });

  describe('#loadServiceComponentsSuccessCallback', function () {
    beforeEach(function () {
      sinon.stub(wizardController, 'getDBProperties', function() {
        return {
          selectedServiceNames: ['a','b'],
          installedServiceNames: ['c','d']
        };
      });
      sinon.stub(App.stackServiceMapper, 'mapStackServices', Em.K); 
    });
    afterEach(function () {
      wizardController.getDBProperties.restore();
      App.stackServiceMapper.mapStackServices.restore();
    });
    it('should load json data', function () {
      var jsonData = {
        items: [
          {
            StackServices: {
              isSelected: false,
              service_name: 'a'
            }
          },
          {
            StackServices: {
              isSelected: false,
              service_name: 'none'
            }
          }
        ]
      };
      wizardController.loadServiceComponentsSuccessCallback(jsonData);
      var exp = {
        "items": [
          {
            "StackServices": {
              "isSelected": false,
              "service_name": "a",
              "is_selected": true,
              "is_installed": false
            }
          },
          {
            "StackServices": {
              "isSelected": false,
              "service_name": "none",
              "is_selected": false,
              "is_installed": false
            }
          }
        ]
      };

      expect(jsonData).to.eql(exp);
    });
  });

  describe('#setInfoForStep9', function () {

    var res;

    beforeEach(function () {
      sinon.stub(wizardController, 'getDBProperty').returns(Em.Object.create({
        status: {},
        message: {},
        logTasks: {},
        tasks: {},
        progress: {}
      }));
      sinon.stub(wizardController, 'setDBProperty', function(title,data) {
        res = data;
      });
    });

    afterEach(function () {
      wizardController.getDBProperty.restore();
      wizardController.setDBProperty.restore();
    });

    it('should return info for step 9', function () {
      wizardController.setInfoForStep9();
      var exp = {
        "status": {
          "status": "pending",
          "message": "Waiting",
          "logTasks": [],
          "tasks": [],
          "progress": "0"
        },
        "message": {
          "status": "pending",
          "message": "Waiting",
          "logTasks": [],
          "tasks": [],
          "progress": "0"
        },
        "logTasks": {
          "status": "pending",
          "message": "Waiting",
          "logTasks": [],
          "tasks": [],
          "progress": "0"
        },
        "tasks": {
          "status": "pending",
          "message": "Waiting",
          "logTasks": [],
          "tasks": [],
          "progress": "0"
        },
        "progress": {
          "status": "pending",
          "message": "Waiting",
          "logTasks": [],
          "tasks": [],
          "progress": "0"
        }
      };

      res = JSON.parse(JSON.stringify(res));

      expect(res).to.eql(exp);
    });
  });

  describe('#saveServiceConfigProperties', function () {

    beforeEach(function () {
      c.set('content', {});
      sinon.stub(c, 'setDBProperties', Em.K);
      sinon.stub(c, 'getDBProperty').withArgs('fileNamesToUpdate').returns([]);
      sinon.stub(App.config, 'shouldSupportFinal').returns(true);
    });

    afterEach(function () {
      c.setDBProperties.restore();
      c.getDBProperty.restore();
      App.config.shouldSupportFinal.restore();
    });

    var kerberosStepController = Em.Object.create({
      installedServiceNames: ['KERBEROS'],
      stepConfigs: [
        Em.Object.create({
          serviceName: 'KERBEROS',
          configs: [
            Em.Object.create({
              id: 'id',
              name: 'admin_password',
              value: 'value',
              defaultValue: 'defaultValue',
              description: 'description',
              serviceName: 'serviceName',
              domain: 'domain',
              isVisible: true,
              isNotDefaultValue: true,
              isFinal: true,
              defaultIsFinal: true,
              supportsFinal: true,
              filename: 'krb5-conf.xml',
              displayType: 'string',
              isRequiredByAgent: true,
              hasInitialValue: true,
              isRequired: true,
              group: {name: 'group'},
              showLabel: true,
              category: 'some_category'
            }),

            Em.Object.create({
              id: 'id',
              name: 'admin_principal',
              value: 'value',
              defaultValue: 'defaultValue',
              description: 'description',
              serviceName: 'serviceName',
              domain: 'domain',
              isVisible: true,
              isNotDefaultValue: true,
              isFinal: true,
              defaultIsFinal: true,
              supportsFinal: true,
              filename: 'krb5-conf.xml',
              displayType: 'string',
              isRequiredByAgent: true,
              hasInitialValue: true,
              isRequired: true,
              group: {name: 'group'},
              showLabel: true,
              category: 'some_category'
            })
          ]
        })
      ]
    });

    var stepController = Em.Object.create({
      installedServiceNames: ['HDFS'],
      stepConfigs: [
      Em.Object.create({
        serviceName: 'HDFS',
        configs: [
          Em.Object.create({
            id: 'id',
            name: 'name',
            value: 'value',
            defaultValue: 'defaultValue',
            description: 'description',
            serviceName: 'serviceName',
            domain: 'domain',
            isVisible: true,
            isNotDefaultValue: true,
            isFinal: true,
            defaultIsFinal: true,
            supportsFinal: true,
            filename: 'filename',
            displayType: 'string',
            isRequiredByAgent: true,
            hasInitialValue: true,
            isRequired: true,
            showLabel: true,
            category: 'some_category'
          })
        ]
      }),
      Em.Object.create({
        serviceName: 'YARN',
        configs: [
          Em.Object.create({
            id: 'id',
            name: 'name',
            value: 'value',
            defaultValue: 'defaultValue',
            description: 'description',
            serviceName: 'serviceName',
            domain: 'domain',
            isVisible: true,
            isFinal: true,
            defaultIsFinal: true,
            supportsFinal: true,
            filename: 'filename',
            displayType: 'string',
            isRequiredByAgent: true,
            hasInitialValue: true,
            isRequired: true,
            group: {name: 'group'},
            showLabel: true,
            category: 'some_category'
          })
        ]
      })
    ]});

    it('should save configs from default config group to content.serviceConfigProperties', function () {
      c.saveServiceConfigProperties(stepController);
      var saved = c.get('content.serviceConfigProperties');
      expect(saved.length).to.equal(1);
      expect(saved[0].category).to.equal('some_category');
    });

    it('should not save admin_principal or admin_password to the localStorage', function () {
      c.saveServiceConfigProperties(kerberosStepController);
      var saved = c.get('content.serviceConfigProperties');
      saved.forEach(function(config) {
        expect(config.value).to.equal('');
      });
    });
  });

  describe('#enableStep', function () {

    it('should update appropriate value in isStepDisabled', function () {

      c.set('isStepDisabled', [
        Em.Object.create({step: 1, value: true}),
        Em.Object.create({step: 2, value: true}),
        Em.Object.create({step: 3, value: true}),
        Em.Object.create({step: 4, value: true}),
        Em.Object.create({step: 5, value: true}),
        Em.Object.create({step: 6, value: true}),
        Em.Object.create({step: 7, value: true})
      ]);

      c.enableStep(1);
      expect(c.get('isStepDisabled')[0].get('value')).to.be.false;

      c.enableStep(7);
      expect(c.get('isStepDisabled')[6].get('value')).to.be.false;
    });

  });

  describe('#allHosts', function () {

    it('should return all hosts', function () {
      var hosts = {
        'h1': {hostComponents: ['c1', 'c2'], disk_info: [{size: 2, available: 1}]},
        'h2': {hostComponents: ['c3', 'c4'], disk_info: [{size: 2, available: 1}]}
      };

      var content = Em.Object.create({
        hosts: hosts
      });

      c.set('content', content);

      var exp = [
        {
          "id": "h1",
          "hostName": "h1",
          "publicHostName": "h1",
          "diskInfo": [
            {
              "size": 2,
              "available": 1
            }
          ],
          "diskTotal": 0.0000019073486328125,
          "diskFree": 9.5367431640625e-7,
          "disksMounted": 1,
          "osType": 0,
          "osArch": 0,
          "ip": 0,
          "hostComponents": [
            {
              "componentName": "c1",
              "displayName": "C1"
            },
            {
              "componentName": "c2",
              "displayName": "C2"
            }
          ]
        },
        {
          "id": "h2",
          "hostName": "h2",
          "publicHostName": "h2",
          "diskInfo": [
            {
              "size": 2,
              "available": 1
            }
          ],
          "diskTotal": 0.0000019073486328125,
          "diskFree": 9.5367431640625e-7,
          "disksMounted": 1,
          "osType": 0,
          "osArch": 0,
          "ip": 0,
          "hostComponents": [
            {
              "componentName": "c3",
              "displayName": "C3"
            },
            {
              "componentName": "c4",
              "displayName": "C4"
            }
          ]
        }
      ];

      var res = JSON.parse(JSON.stringify(c.get('allHosts')));

      expect(res).to.be.eql(exp);
    });
  });

  describe('#getSlaveComponentHosts', function () {
    beforeEach(function () {
      sinon.stub(App.Service, 'find').returns(Em.A([
        Em.Object.create({
          serviceName: 's1'
        })
      ]));
      sinon.stub(App.StackService, 'find').returns(Em.A([
        Em.Object.create({
          serviceName: 's2',
          isSelected: true
        })
      ]));
      sinon.stub(App.StackServiceComponent, 'find').returns(Em.A([
        Em.Object.create({componentName: 'DATANODE', serviceName: 's1', isSlave: true}),
        Em.Object.create({componentName: 'c2', serviceName: 's2', isSlave: true})
      ]));
      sinon.stub(App.HostComponent, 'find').returns(Em.A([
        Em.Object.create({
          componentName: 'DATANODE',
          hostName: 'h1'
        })
      ]));
    });

    afterEach(function () {
      App.Service.find.restore();
      App.HostComponent.find.restore();
      App.StackService.find.restore();
      App.StackServiceComponent.find.restore();
    });

    it('should return slave components', function () {
      var res = JSON.parse(JSON.stringify(c.getSlaveComponentHosts()));
      var exp = [
        {
          "componentName": "DATANODE",
          "displayName": "DataNode",
          "hosts": [
            {
              "group": "Default",
              "hostName": "h1",
              "isInstalled": true
            }
          ],
          "isInstalled": true
        },
        {
          "componentName": "CLIENT",
          "displayName": "Client",
          "hosts": [],
          "isInstalled": true
        },
        {
          "componentName": "c2",
          "displayName": "C2",
          "hosts": [
            {
              "group": "Default",
              "hostName": "h1",
              "isInstalled": false
            }
          ],
          "isInstalled": false
        }
      ];

      expect(res).to.be.eql(exp);
    });

  });

  describe('#setSkipSlavesStep', function () {

    var step = 6,
      cases = [
        {
          services: [
            {
              hasSlave: true,
              hasNonMastersWithCustomAssignment: true
            }
          ],
          skipSlavesStep: false,
          title: 'service with customizable slave selected'
        },
        {
          services: [
            {
              hasClient: true,
              hasNonMastersWithCustomAssignment: true
            }
          ],
          skipSlavesStep: false,
          title: 'service with customizable client selected'
        },
        {
          services: [
            {
              hasSlave: true,
              hasNonMastersWithCustomAssignment: false
            },
            {
              hasClient: true,
              hasNonMastersWithCustomAssignment: false
            }
          ],
          skipSlavesStep: true,
          title: 'no service with customizable slaves or clients selected'
        },
        {
          services: [
            {
              hasSlave: false,
              hasClient: false
            }
          ],
          skipSlavesStep: true,
          title: 'no service with slaves or clients selected'
        }
      ];

    beforeEach(function () {
      c.reopen({
        isStepDisabled: [
          Em.Object.create({
            step: 6
          })
        ],
        content: {}
      });
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        c.setSkipSlavesStep(item.services, step);
        expect(Boolean(c.get('isStepDisabled').findProperty('step', step).get('value'))).to.equal(item.skipSlavesStep);
      });
    });

  });

  describe('#toJSInstance', function () {

    var testCases = [
      {
        o: {'test': 'test'},
        e: {'test': 'test'}
      },
      {
        o: {'test': Em.Object.create()},
        e: {'test': {}}
      },
      {
        o: {'test': Em.Object.create({'test': {}})},
        e: {'test': {'test': {}}}
      },
      {
        o: [],
        e: []
      },
      {
        o: Em.A([[]]),
        e: [[]]
      },
      {
        o: 11,
        e: 11
      },
      {
        o: '11',
        e: '11'
      },
      {
        o: null,
        e: null
      }
    ];

    it('should convert objects and arrays to pure JS objects and arrays', function () {
      testCases.forEach(function (testCase) {
        expect(c.toJSInstance(testCase.o)).to.eql(testCase.e);
      });
    });
  });

  describe('#loadConfigThemes', function() {
    beforeEach(function () {
      sinon.stub(wizardController, 'loadConfigThemeForServices').returns({
        always: function(callback) {callback();}
      });
      sinon.stub(App.themesMapper, 'generateAdvancedTabs').returns(true);
      sinon.stub(App.config, 'loadConfigsFromStack').returns({
        done: function(callback) {
          callback();
        }
      });
      sinon.stub(App.StackService, 'find').returns(Em.A([
        Em.Object.create({
          isSelected: true,
          serviceName: 's1'
        })
      ]));
    });
    afterEach(function () {
      App.get.restore();
      App.StackService.find.restore();
      App.config.loadConfigsFromStack.restore();
      App.themesMapper.generateAdvancedTabs.restore();
      wizardController.loadConfigThemeForServices.restore();
    });
    it('Should load config themes', function() { 
      sinon.stub(App, 'get').returns(true);
      wizardController.loadConfigThemes().then(function(data) {
        expect().to.be.undefined;
      });
    });
    it('Should load config themes', function() {
      sinon.stub(App, 'get').returns(false); 
      wizardController.loadConfigThemes().then(function(data) {
        expect().to.be.undefined;
      });
    });
  });

  describe('#dataLoading', function () {
    var clusterController = Em.Object.create({
      isLoaded: false
    });
    beforeEach(function(){
      sinon.stub(App.router,'get').returns(clusterController);
      sinon.stub(wizardController, 'connectOutlet', Em.K);
      clusterController.set('isLoaded', false);
    });
    afterEach(function(){
      App.router.get.restore();
      wizardController.connectOutlet.restore();
    });
    it('should load data', function () {
      clusterController.set('isLoaded', true);
      wizardController.dataLoading().then(function(data){
        expect(data).to.be.undefined;
      });
    });
    it('should load data after 25ms', function () {
      clusterController.set('isLoaded', false);
      setTimeout(function(){
        clusterController.set('isLoaded', true);
      },25);
      wizardController.dataLoading().then(function(data){
        expect(data).to.be.undefined;
      });
    });
  });

  describe('#loadRecommendations', function () {

    beforeEach(function () {
      sinon.stub(c, 'getDBProperty').returns({});
    });

    afterEach(function () {
      c.getDBProperty.restore();
    });

    it('should set recommendations', function () {
      c.set('content', {});
      c.loadRecommendations();
      expect(c.get('content.recommendations')).to.eql({});
    });

  });

});
