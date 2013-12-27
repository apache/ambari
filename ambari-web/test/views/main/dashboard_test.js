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
require('messages');
var filters = require('views/common/filter_view');
require('views/main/dashboard');

describe('App.MainDashboardView', function() {

  var mainDashboardView = App.MainDashboardView.create();

  describe('#setInitPrefObject', function() {
    var hdfs_widgets_count = 7;
    var mapreduce_widgets_count = 7;
    var hbase_widgets_count = 4;
    var yarn_widgets_count = 4;
    var total_widgets_count = 27;
    var tests = [
      {
        models: {
          hdfs_model: null,
          mapreduce_model: null,
          hbase_model: null,
          yarn_model: null
        },
        e: {
          visibleL: total_widgets_count - hdfs_widgets_count - mapreduce_widgets_count - hbase_widgets_count - yarn_widgets_count - 1,
          hiddenL: 0
        },
        m: 'All models are null'
      },
      {
        models: {
          hdfs_model: {},
          mapreduce_model: null,
          hbase_model: null,
          yarn_model: null
        },
        e: {
          visibleL: total_widgets_count  - mapreduce_widgets_count - hbase_widgets_count - yarn_widgets_count - 1,
          hiddenL: 0
        },
        m: 'mapreduce_model, hbase_model, yarn_model are null'
      },
      {
        models: {
          hdfs_model: {},
          mapreduce_model: {},
          hbase_model: null,
          yarn_model: null
        },
        e: {
          visibleL: total_widgets_count - hbase_widgets_count - yarn_widgets_count - 1,
          hiddenL: 0
        },
        m: 'hbase_model and yarn_model are null'
      },
      {
        models: {
          hdfs_model: {},
          mapreduce_model: {},
          hbase_model: {},
          yarn_model: null
        },
        e: {
          visibleL: total_widgets_count - yarn_widgets_count - 1,
          hiddenL: 1
        },
        m: 'yarn_model is null'
      },
      {
        models: {
          hdfs_model: {},
          mapreduce_model: {},
          hbase_model: {},
          yarn_model: {}
        },
        e: {
          visibleL: total_widgets_count - 1,
          hiddenL: 1
        },
        m: 'All models are not null'
      }
    ];
    tests.forEach(function(test) {
      it(test.m, function() {
        mainDashboardView.set('hdfs_model', test.models.hdfs_model);
        mainDashboardView.set('mapreduce_model', test.models.mapreduce_model);
        mainDashboardView.set('hbase_model', test.models.hbase_model);
        mainDashboardView.set('yarn_model', test.models.yarn_model);
        mainDashboardView.setInitPrefObject();
        expect(mainDashboardView.get('initPrefObject.visible.length')).to.equal(test.e.visibleL);
        expect(mainDashboardView.get('initPrefObject.hidden.length')).to.equal(test.e.hiddenL);
      });
    });
  });

  describe('#persistKey', function() {
    App.router.set('loginName', 'tdk');
    it('Check it', function() {
      expect(mainDashboardView.get('persistKey')).to.equal('user-pref-tdk-dashboard');
    });
  });

});
