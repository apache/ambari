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

var Ember = require('ember');
var App = require('app');

require('mappers/server_data_mapper');
require('mappers/dataset_mapper');

describe('App.dataSetMapper', function () {

  describe('#getId', function() {
    var tests = [
      {i:'My Name',e:'My_Name'},
      {i:'MyName',e:'MyName'},
      {i:'My  Name',e:'My__Name'},
      {i:'My Big Name',e:'My_Big_Name'}
    ];
    it('Replace spaces with _', function() {
      tests.forEach(function(test) {
        expect(App.dataSetMapper.getId(test.i)).to.equal(test.e);
      });
    });
  });

  describe('#parseSchedule', function() {
    var tests = [
      {
        "Feeds":{
          "frequency":"minutes(1)",
          "timezone":"UTC",
          "clusters":{
            "cluster":[
              {
                "name":"drsource3",
                "type":"source",
                "validity":{
                  "start":"2010-01-01T00:00Z",
                  "end":"2015-01-01T02:00Z"
                }
              }
            ]
          }
        },
        "results": {
          start_time: '2:0:AM',
          end_time: '4:0:AM',
          frequency: 'minutes(1)',
          timezone: 'UTC',
          start_date: '1/5/2010',
          end_date: '1/4/2015'
        }
      },
      {
        "Feeds":{
          "frequency":"minutes(5)",
          "timezone":"UTC",
          "clusters":{
            "cluster":[
              {
                "name":"drsource3",
                "type":"source",
                "validity":{
                  "start":"2013-01-01T15:00Z",
                  "end":"2014-01-01T12:00Z"
                }
              }
            ]
          }
        },
        "results": {
          start_time: '5:0:PM',
          end_time: '2:0:PM',
          frequency: 'minutes(5)',
          timezone: 'UTC',
          start_date: '1/2/2013',
          end_date: '1/3/2014'
        }
      }
    ];
    tests.forEach(function(test) {
      it('parse valid data', function() {
        var schedule = App.dataSetMapper.parseSchedule(test);
        expect(schedule.start_time).to.equal(test.results.start_time);
        expect(schedule.end_time).to.equal(test.results.end_time);
        expect(schedule.frequency).to.equal(test.results.frequency);
        expect(schedule.timezone).to.equal(test.results.timezone);
        expect(schedule.start_date).to.equal(test.results.start_date);
        expect(schedule.end_date).to.equal(test.results.end_date);
      });
    });
  });

});
