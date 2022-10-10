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
require('views/main/admin/highAvailability/journalNode/step5_view');

describe('App.ManageJournalNodeWizardStep5View', function () {
  var view = App.ManageJournalNodeWizardStep5View.create({
    controller: Em.Object.create({
      content: {
        serviceConfigProperties: {
          items: [{
            type: 'hdfs-site',
            properties: {
              'dfs.journalnode.edits.dir': 'dir',
              'dfs.journalnode.edits.dir.group1': 'dir2',
              'dfs.journalnode.edits.dir.group2': 'dir3'
            }
          }]
        }
      }
    })
  });

  var nameSpaceMock = Em.Object.create({
    masterComponentGroups: [{name: 'group1'}]
  });
  var severalNameSpacesMock = Em.Object.create({
    masterComponentGroups: [{name: 'group1'}, {name: 'group2'}]
  });

  describe("#step5BodyText", function () {

    beforeEach(function () {
      this.stub = sinon.stub(App.HDFSService, 'find');
    });

    afterEach(function () {
      this.stub.restore();
    });

    it("formatted with dependent data (1 namespace)", function () {
      this.stub.returns(nameSpaceMock);
      view.set('controller.content.masterComponentHosts', [{
        component: 'JOURNALNODE',
        isInstalled: true,
        hostName: 'host1'
      }]);
      view.set('controller.content.hdfsUser', 'user');
      view.propertyDidChange('step5BodyText');
      expect(view.get('step5BodyText')).to.equal(Em.I18n.t('admin.manageJournalNode.wizard.step5.body').format('host1', '<b>dir</b>'));
    });

    it("formatted with dependent data (several namespaces)", function () {
      this.stub.returns(severalNameSpacesMock);
      view.set('controller.content.masterComponentHosts', [{
        component: 'JOURNALNODE',
        isInstalled: true,
        hostName: 'host1'
      }]);
      view.set('controller.content.hdfsUser', 'user');
      view.propertyDidChange('step5BodyText');
      expect(view.get('step5BodyText')).to.equal(Em.I18n.t('admin.manageJournalNode.wizard.step5.body').format('host1', '<b>dir2</b>, <b>dir3</b>'));
    });

  });

});
