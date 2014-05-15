/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * License); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an AS IS BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var App = require('app');

var modelSetup = require('test/init_model_test');
require('models/service/hbase');

var hBaseService,
  hBaseServiceData = {
    id: 'hbase'
  },
  hostComponentsData = [
    {
      id: 'regionserver',
      componentName: 'HBASE_REGIONSERVER'
    }
  ];

describe('App.HBaseService', function () {

  beforeEach(function () {
    hBaseService = App.HBaseService.createRecord(hBaseServiceData);
  });

  afterEach(function () {
    modelSetup.deleteRecord(hBaseService);
  });

  describe('#regionServers', function () {
    it('should take one component from hostComponents', function () {
      hBaseService.reopen({
        hostComponents: hostComponentsData
      });
      expect(hBaseService.get('regionServers')).to.have.length(1);
      expect(hBaseService.get('regionServers')[0].id).to.equal('regionserver');
    });
  });

});
