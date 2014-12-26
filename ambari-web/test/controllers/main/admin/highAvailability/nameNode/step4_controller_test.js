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

describe('App.HighAvailabilityWizardStep4Controller', function() {
  
  describe('#checkNnCheckPointStatus', function() {
    beforeEach(function() {
      this.controller = App.HighAvailabilityWizardStep4Controller.create();
      this.clock = sinon.useFakeTimers();
      sinon.stub(this.controller, 'pullCheckPointStatus');
    });

    afterEach(function() {
      this.clock.restore();
      this.controller.pullCheckPointStatus.restore();
    });

    var tests = [
      {
        responseData: {
          HostRoles: { desired_state: 'STARTED' }
        },
        m: 'NameNode started, Safemode off, no journal node transaction. Polling should be performed and isNameNodeStarted should be true',
        e: {
          isPollingCalled: true,
          isNameNodeStarted: true,
          isNextEnabled: false
        }
      },
      {
        responseData: {
          HostRoles: { desired_state: 'STARTED' },
          metrics: { dfs: { namenode: {
            Safemode: 'ON',
            JournalTransactionInfo: "{\"LastAppliedOrWrittenTxId\":\"4\",\"MostRecentCheckpointTxId\":\"2\"}"
          }}}
        },
        m: 'NameNode started, Safemode on, journal node transaction invalid. Polling should be performed and isNameNodeStarted should be true',
        e: {
          isPollingCalled: true,
          isNameNodeStarted: true,
          isNextEnabled: false
        }
      },
      {
        responseData: {
          HostRoles: { desired_state: 'INSTALLED' },
          metrics: { dfs: { namenode: {
            Safemode: 'ON',
            JournalTransactionInfo: "{\"LastAppliedOrWrittenTxId\":\"15\",\"MostRecentCheckpointTxId\":\"14\"}"
          }}}
        },
        m: 'NameNode not started, Safemode on, journal node transaction present. Polling should not be performed and isNameNodeStarted should be false',
        e: {
          isPollingCalled: false,
          isNameNodeStarted: false,
          isNextEnabled: true
        }
      },
      {
        responseData: {
          HostRoles: { desired_state: 'STARTED' },
          metrics: { dfs: { namenode: {
            Safemode: "",
            JournalTransactionInfo: "{\"LastAppliedOrWrittenTxId\":\"15\",\"MostRecentCheckpointTxId\":\"14\"}"
          }}}
        },
        m: 'NameNode started, Safemode off, journal node transaction present. Polling should not be performed and isNameNodeStarted should be true',
        e: {
          isPollingCalled: true,
          isNameNodeStarted: true,
          isNextEnabled: false
        }
      }
    ];

    tests.forEach(function(test) {
      it(test.m, function() {
        this.controller.set('isNameNodeStarted', !test.e.isNameNodeStarted);
        this.controller.checkNnCheckPointStatus(test.responseData);
        this.clock.tick(this.controller.get('POLL_INTERVAL'));
        expect(this.controller.get('isNameNodeStarted')).to.be.eql(test.e.isNameNodeStarted);
        expect(this.controller.get('isNextEnabled')).to.be.eql(test.e.isNextEnabled);
        expect(this.controller.pullCheckPointStatus.called).to.be.eql(test.e.isPollingCalled);
      });
    });
  });
});

