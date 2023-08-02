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
require('views/main/admin/highAvailability/journalNode/step3_view');

describe('App.ManageJournalNodeWizardStep3View', function () {
  var view = App.ManageJournalNodeWizardStep3View.create({
    controller: Em.Object.create({
      content: {},
      pullCheckPointsStatuses: Em.K
    })
  });

  var nameSpaceMock = Em.Object.create({
    masterComponentGroups: [{name: 'group1'}]
  });
  var severalNameSpacesMock = Em.Object.create({
    masterComponentGroups: [{name: 'group1'}, {name: 'group2'}]
  });

  describe("#didInsertElement()", function () {
    before(function () {
      sinon.spy(view.get('controller'), 'pullCheckPointsStatuses');
    });
    after(function () {
      view.get('controller').pullCheckPointsStatuses.restore();
    });
    it("call pullCheckPointsStatuses", function () {
      view.didInsertElement();
      expect(view.get('controller').pullCheckPointsStatuses.calledOnce).to.be.true;
    });
  });

  describe("#step3BodyText", function () {

    beforeEach(function () {
      this.stub = sinon.stub(App.HDFSService, 'find');
    });

    afterEach(function () {
      this.stub.restore();
    });

    it("formatted with dependent data (1 namespace)", function () {
      this.stub.returns(nameSpaceMock);
      view.set('controller.content.masterComponentHosts', [{
        component: 'NAMENODE',
        isInstalled: true,
        hostName: 'host1'
      }]);
      view.set('controller.content.activeNN', {
        host_name: 'Active NameNode'
      });
      view.set('controller.content.hdfsUser', 'user');
      view.propertyDidChange('step3BodyText');
      expect(view.get('step3BodyText')).to.equal(Em.I18n.t('admin.manageJournalNode.wizard.step3.body').format('Active NameNode',
          Em.I18n.t('admin.manageJournalNode.wizard.step3.body.singleNameSpace.safeModeText'),
          Em.I18n.t('admin.manageJournalNode.wizard.step3.body.singleNameSpace.safeModeCommand').format('user'),
          Em.I18n.t('admin.manageJournalNode.wizard.step3.body.singleNameSpace.checkPointText'),
          Em.I18n.t('admin.manageJournalNode.wizard.step3.body.singleNameSpace.checkPointCommand').format('user'),
          Em.I18n.t('admin.manageJournalNode.wizard.step3.body.singleNameSpace.proceed'),
          Em.I18n.t('admin.manageJournalNode.wizard.step3.body.singleNameSpace.recentCheckPoint')
      ));
    });

    it("formatted with dependent data (several namespaces)", function () {
      var nameSpaces = ['group1', 'group2'];
      this.stub.returns(severalNameSpacesMock);
      view.set('controller.content.masterComponentHosts', [{
        component: 'NAMENODE',
        isInstalled: true,
        hostName: 'host1'
      }]);
      view.set('controller.content.activeNN', {
        host_name: 'Active NameNode'
      });
      view.set('controller.content.hdfsUser', 'user');
      view.propertyDidChange('step3BodyText');
      expect(view.get('step3BodyText')).to.equal(Em.I18n.t('admin.manageJournalNode.wizard.step3.body').format('Active NameNode',
          Em.I18n.t('admin.manageJournalNode.wizard.step3.body.multipleNameSpaces.safeModeText'),
          nameSpaces.map(function(ns) {
            return Em.I18n.t('admin.manageJournalNode.wizard.step3.body.multipleNameSpaces.safeModeCommand')
                .format('user', ns);
          }).join('<br>'),
          Em.I18n.t('admin.manageJournalNode.wizard.step3.body.multipleNameSpaces.checkPointText'),
          nameSpaces.map(function(ns) {
            return Em.I18n.t('admin.manageJournalNode.wizard.step3.body.multipleNameSpaces.checkPointCommand')
                .format('user', ns);
          }).join('<br>'),
          Em.I18n.t('admin.manageJournalNode.wizard.step3.body.multipleNameSpaces.proceed'),
          Em.I18n.t('admin.manageJournalNode.wizard.step3.body.multipleNameSpaces.recentCheckPoint')
      ));
    });

  });

  describe("#nnCheckPointText", function() {

    beforeEach(function () {
      this.stub = sinon.stub(App.HDFSService, 'find');
    });

    afterEach(function () {
      this.stub.restore();
    });

    it("formatted with dependent data (HDFS name spaces not loaded)", function() {
      view.set('controller.isHDFSNameSpacesLoaded', false);
      view.propertyDidChange('nnCheckPointText');
      expect(view.get('nnCheckPointText')).to.equal('');
    });

    it("formatted with dependent data (1 HDFS name space and and nn checkpoint created)", function() {
      this.stub.returns(nameSpaceMock);
      view.set('controller.isNextEnabled', true)
      view.set('controller.isHDFSNameSpacesLoaded', true);
      view.propertyDidChange('nnCheckPointText');
      expect(view.get('nnCheckPointText')).to.equal(Em.I18n.t('admin.highAvailability.wizard.step4.ckCreated'));
    });

    it("formatted with dependent data (1 HDFS name space and and nn checkpoint not created)", function() {
      this.stub.returns(nameSpaceMock);
      view.set('controller.isNextEnabled', false)
      view.set('controller.isHDFSNameSpacesLoaded', true);
      view.propertyDidChange('nnCheckPointText');
      expect(view.get('nnCheckPointText')).to.equal(Em.I18n.t('admin.highAvailability.wizard.step4.ckNotCreated'));
    });

    it("formatted with dependent data (several HDFS name spaces and and nn checkpoint created)", function() {
      this.stub.returns(severalNameSpacesMock);
      view.set('controller.isNextEnabled', true)
      view.set('controller.isHDFSNameSpacesLoaded', true);
      view.propertyDidChange('nnCheckPointText');
      expect(view.get('nnCheckPointText')).to.equal(Em.I18n.t('admin.manageJournalNode.wizard.step3.checkPointsCreated'));
    });

    it("formatted with dependent data (several HDFS name spaces and and nn checkpoint not created)", function() {
      this.stub.returns(severalNameSpacesMock);
      view.set('controller.isNextEnabled', false)
      view.set('controller.isHDFSNameSpacesLoaded', true);
      view.propertyDidChange('nnCheckPointText');
      expect(view.get('nnCheckPointText')).to.equal(Em.I18n.t('admin.manageJournalNode.wizard.step3.checkPointsNotCreated'));
    });

  });

  describe("#errorText", function() {

    beforeEach(function () {
      this.stub = sinon.stub(App.HDFSService, 'find');
    });

    afterEach(function () {
      this.stub.restore();
    });

    it("formatted with dependent data (HDFS name spaces not loaded)", function() {
      view.set('controller.isHDFSNameSpacesLoaded', false);
      view.propertyDidChange('errorText');
      expect(view.get('errorText')).to.equal('');
    });

    it("formatted with dependent data (1 HDFS name space and nn started)", function() {
      this.stub.returns(nameSpaceMock);
      view.set('controller.isNameNodeStarted', true)
      view.set('controller.isHDFSNameSpacesLoaded', true);
      view.propertyDidChange('errorText');
      expect(view.get('errorText')).to.equal('');
    });

    it("formatted with dependent data (1 HDFS name space and nn not started)", function() {
      this.stub.returns(nameSpaceMock);
      view.set('controller.isNameNodeStarted', false)
      view.set('controller.isHDFSNameSpacesLoaded', true);
      view.propertyDidChange('errorText');
      expect(view.get('errorText')).to.equal(Em.I18n.t('admin.highAvailability.wizard.step4.error.nameNode'));
    });

    it("formatted with dependent data (several HDFS name spaces and active nn started)", function() {
      this.stub.returns(severalNameSpacesMock);
      view.set('controller.isActiveNameNodesStarted', true)
      view.set('controller.isHDFSNameSpacesLoaded', true);
      view.propertyDidChange('errorText');
      expect(view.get('errorText')).to.equal('');
    });

    it("formatted with dependent data (several HDFS name spaces and active nn not started)", function() {
      this.stub.returns(severalNameSpacesMock);
      view.set('controller.isActiveNameNodesStarted', false)
      view.set('controller.isHDFSNameSpacesLoaded', true);
      view.propertyDidChange('errorText');
      expect(view.get('errorText')).to.equal(Em.I18n.t('admin.manageJournalNode.wizard.step3.error.multipleNameSpaces.nameNodes'));
    });
    
  });

});
