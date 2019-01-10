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
var stringUtils = require('utils/string_utils');

describe('App.ReassignMasterWizardStep5View', function () {
  var view;

  beforeEach(function () {
    view = App.ReassignMasterWizardStep5View.create({});
  });

  describe('#manualCommands', function () {
    it('should return empty string if component has not manual commands', function () {
      view.set('controller', Em.Object.create({
        content: {
          componentsWithManualCommands: [],
          reassign: {
            component_name: 'test'
          }
        }
      }));
      expect(view.get('manualCommands')).to.be.equal('');
    });

    it('should return correct message if component has manual commands without nn started hosts', function () {
      view.set('controller', Em.Object.create({
        content: {
          componentsWithManualCommands: ['namenode'],
          reassign: {
            component_name: 'namenode',
          },
          reassignHosts: {
            source: 'source_host',
            target: 'target_host'
          },
          componentDir: 'test_dir',
          hdfsUser: 'hdfs_user',
          group: 'group'
        }
      }));
      expect(view.get('manualCommands')).to.be.equal(
        Em.I18n.t('services.reassign.step5.body.namenode').format('test_dir', 'source_host', 'target_host', 'hdfs_user', undefined, 'group', 'test_dir', 'leveldb-timeline-store.ldb')
      );
    });

    it('should return correct message if component has manual commands with nn started hosts', function () {
      view.set('controller', Em.Object.create({
        content: {
          componentsWithManualCommands: ['NAMENODE'],
          reassign: {
            component_name: 'NAMENODE',
          },
          reassignHosts: {
            source: 'source_host',
            target: 'target_host'
          },
          componentDir: 'test_dir',
          hdfsUser: 'hdfs_user',
          group: 'group',
          masterComponentHosts: [
            Em.Object.create({component: 'NAMENODE', hostName: 'source_host'}),
            Em.Object.create({component: 'NAMENODE', hostName: 'test_host'}),
            Em.Object.create({component: 'NAMENODE', hostName: 'target_host'}),
          ]
        }
      }));
      sinon.stub(App, 'get').returns(true);
      expect(view.get('manualCommands')).to.be.equal(
        Em.I18n.t('services.reassign.step5.body.namenode_ha').format('test_dir', 'source_host', 'target_host', 'hdfs_user', ['test_host'], 'group', 'test_dir', 'leveldb-timeline-store.ldb')
      );
      App.get.restore();
    });
  });
});