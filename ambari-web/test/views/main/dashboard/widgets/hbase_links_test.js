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

require('views/main/dashboard/widgets/hbase_links');

describe('App.HBaseLinksView', function () {

  var view;

  beforeEach(function () {
    view = App.HBaseLinksView.create({
      model: Em.Object.create()
    });
  });

  describe('#port()', function () {

    beforeEach(function () {
      this.stackServiceStub = sinon.stub(App.StackService, 'find');
    });

    afterEach(function () {
      this.stackServiceStub.restore();
    });

    it('should return port 16010', function () {
      this.stackServiceStub.returns(Em.Object.create({
        serviceName: 'HBASE',
        serviceVersion: '1.0',
        compareCurrentVersion: App.StackService.proto().compareCurrentVersion
      }));
      expect(view.get('port')).to.equal('60010');
    });

    it('should return port 16010', function () {
      this.stackServiceStub.returns(Em.Object.create({
        serviceName: 'HBASE',
        serviceVersion: '1.2',
        compareCurrentVersion: App.StackService.proto().compareCurrentVersion
      }));
      expect(view.get('port')).to.equal('16010');
    });
  });

  describe('#hbaseMasterWebUrl()', function () {
    beforeEach(function () {
      this.stackServiceStub = sinon.stub(App.StackService, 'find');
    });

    afterEach(function () {
      this.stackServiceStub.restore();
    });

    it('should return empty string', function () {
      expect(view.get('hbaseMasterWebUrl')).to.equal('');
    });

    it('should return proper url', function () {
      this.stackServiceStub.returns(Em.Object.create({
        serviceName: 'HBASE',
        serviceVersion: '1.2',
        compareCurrentVersion: App.StackService.proto().compareCurrentVersion
      }));
      view.set('model.hostComponents', Em.A([
        {
          isMaster: true,
          haStatus: 'true',
          host: {
            publicHostName: 'c1'
          }
        }
      ]));
      expect(view.get('hbaseMasterWebUrl')).to.equal('http://c1:16010');
    });
  });

  describe('#calcWebUrl()', function () {

    it('should return ""', function () {
      expect(view.calcWebUrl()).to.equal('');
    });
  });

});
