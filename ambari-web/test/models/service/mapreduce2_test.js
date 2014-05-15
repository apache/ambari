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
require('models/service/mapreduce2');

var mapReduce2Service,
  mapReduce2ServiceData = {
    id: 'mr2'
  },
  hostComponentsData = [
    {
      id: 'mr2client',
      componentName: 'MAPREDUCE2_CLIENT',
      host: {
        id: 'host'
      }
    }
  ];

describe('App.MapReduce2Service', function () {

  beforeEach(function () {
    mapReduce2Service = App.MapReduce2Service.createRecord(mapReduce2ServiceData);
  });

  afterEach(function () {
    modelSetup.deleteRecord(mapReduce2Service);
  });

  describe('#mapReduce2Clients', function () {
    it('should take one component from hostComponents', function () {
      mapReduce2Service.reopen({
        hostComponents: hostComponentsData
      });
      expect(mapReduce2Service.get('mapReduce2Clients')).to.have.length(1);
      expect(mapReduce2Service.get('mapReduce2Clients')[0].id).to.equal('host');
    });
  });

});
