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

require('mappers/socket/host_component_status_mapper');

describe('App.hostComponentStatusMapper', function () {

  describe('#map', function() {
    var hc = Em.Object.create({
      workStatus: 'INSTALLED',
      isLoaded: true
    });
    beforeEach(function() {
      sinon.stub(App.HostComponent, 'find').returns(hc);
    });
    afterEach(function() {
      App.HostComponent.find.restore();
    });

    it('host-component should have STARTED status', function() {
      const event = {
        componentName: 'C1',
        hostName: 'host1',
        currentState: 'STARTED'
      };
      App.hostComponentStatusMapper.map(event);
      expect(hc.get('workStatus')).to.be.equal('STARTED');
    });
  });
});
