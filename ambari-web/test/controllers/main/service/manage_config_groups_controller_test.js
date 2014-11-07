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
var c;
describe('App.ManageConfigGroupsController', function() {
  var controller = App.ManageConfigGroupsController.create({});

  beforeEach(function() {
    c = App.ManageConfigGroupsController.create({});
  });

  describe('Host Name converting', function() {
    describe('#convertHostNames', function() {
      var hosts = [
        Em.Object.create({
          hostName: 'internal-1.com',
          publicHostName: 'external-1.com'
        }),
        Em.Object.create({
          hostName: 'internal-2.com',
          publicHostName: 'external-2.com'
        }),
        Em.Object.create({
          hostName: 'internal-3.com',
          publicHostName: 'external-3.com'
        })
      ];

      describe('#hostsToPublic', function() {
        beforeEach(function() {
          controller = App.ManageConfigGroupsController.create({
            clusterHosts: Em.A(hosts)
          });
        });

        var tests = [
          {
            hostsList: ['internal-1.com', 'internal-2.com', 'internal-3.com'],
            e: ['external-1.com', 'external-2.com', 'external-3.com']
          },
          {
            hostsList: 'internal-2.com',
            e: 'external-2.com'
          }
        ];
        var message = 'should convert internal host names `{0}` to external host names `{1}`';
        tests.forEach(function(test) {
          it(message.format(test.hostsList, test.e), function() {
            expect(controller.hostsToPublic(test.hostsList)).to.eql(test.e);
          });
        });
      });

      describe('#publicToHostName', function() {
        beforeEach(function() {
          controller = App.ManageConfigGroupsController.create({
            clusterHosts: Em.A(hosts)
          });
        });

        var tests = [
          {
            hostsList: ['external-1.com', 'external-2.com', 'external-3.com'],
            e: ['internal-1.com', 'internal-2.com', 'internal-3.com']
          },
          {
            hostsList: 'external-2.com',
            e: 'internal-2.com'
          }
        ];
        var message = 'should convert internal host names `{0}` to external host names `{1}`';
        tests.forEach(function(test) {
          it(message.format(test.hostsList, test.e), function() {
            expect(controller.publicToHostName(test.hostsList)).to.eql(test.e);
          });
        });
      });
    });

  });

  describe('#deleteHosts', function() {
    var hosts = [
      Em.Object.create({
        hostName: 'internal-1.com',
        publicHostName: 'external-1.com'
      }),
      Em.Object.create({
        hostName: 'internal-2.com',
        publicHostName: 'external-2.com'
      }),
      Em.Object.create({
        hostName: 'internal-3.com',
        publicHostName: 'external-3.com'
      })
    ];

    beforeEach(function() {
      controller = App.ManageConfigGroupsController.create({
        clusterHosts: hosts
      });
    });

    var createConfigGroupWithParentMock = function(groupHosts, groupPublicHosts, allHosts) {
      var parentCGHosts = allHosts.filter(function(host) {
        return !groupHosts.contains(host.get('hostName'));
      });
      return Em.Object.create({
        parentConfigGroup: {
          hosts: parentCGHosts.mapProperty('hostName'),
          publicHosts: parentCGHosts.mapProperty('publicHostName')
        },
        hosts: groupHosts,
        publicHosts: groupPublicHosts
      });
    };

    var tests = [
      {
        selectedHosts: ['external-1.com', 'external-2.com'],
        selectedConfigGroup: createConfigGroupWithParentMock(
          ['internal-1.com', 'internal-2.com'],
          ['external-1.com', 'external-2.com'], hosts),
        e: []
      },
      {
        selectedHosts: ['external-1.com'],
        selectedConfigGroup: createConfigGroupWithParentMock(
          ['internal-1.com', 'internal-2.com'],
          ['external-1.com', 'external-2.com'], hosts),
        e: ['external-2.com']
      }
    ];

    tests.forEach(function(test) {
      it('should remove {0}'.format(test.selectedHosts.slice(0)), function() {
        controller.reopen({
          selectedHosts: test.selectedHosts,
          selectedConfigGroup: test.selectedConfigGroup
        });
        controller.deleteHosts();
        expect(controller.get('selectedConfigGroup.publicHosts').toArray()).to.eql(test.e);
      });
    });

  });

  describe('#addHostsCallback', function() {

    beforeEach(function() {

      c.reopen({
        selectedConfigGroup: Em.Object.create({
          hosts: ['h1'],
          publicHosts: ['p_h1'],
          parentConfigGroup: Em.Object.create({
            hosts: ['h2', 'h3'],
            publicHosts: ['p_h2', 'p_h3']
          })
        })
      });

      sinon.stub(c, 'hostsToPublic', function(s) {return 'p_' + s;});

    });

    afterEach(function() {
      c.hostsToPublic.restore();
    });

    it('should set hosts to selectedConfigGroup and remove them form default group', function () {

      c.addHostsCallback(['h2', 'h3']);

      expect(c.get('selectedConfigGroup.hosts')).to.include.members(['h1','h2','h3']);
      expect(c.get('selectedConfigGroup.publicHosts')).to.include.members(['p_h1','p_h2','p_h3']);
      expect(c.get('selectedConfigGroup.parentConfigGroup.hosts').toArray()).to.be.empty;
      expect(c.get('selectedConfigGroup.parentConfigGroup.publicHosts').toArray()).to.be.empty;
    });

  });

});
