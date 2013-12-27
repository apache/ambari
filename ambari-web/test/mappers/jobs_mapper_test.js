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

require('views/main/apps/item/dag_view');
require('mappers/server_data_mapper');
require('mappers/jobs_mapper');

describe('App.jobTimeLineMapper', function () {

  var test_input = {
    "map": [
      {
        "x": 1369394950,
        "y": 0
      },
      {
        "x": 1369394951,
        "y": 1
      },
      {
        "x": 1369394952,
        "y": 1
      },
      {
        "x": 1369394953,
        "y": 0
      }
    ],
    "shuffle": [
      {
        "x": 1369394950,
        "y": 0
      },
      {
        "x": 1369394951,
        "y": 0
      },
      {
        "x": 1369394952,
        "y": 1
      },
      {
        "x": 1369394953,
        "y": 1
      }
    ],
    "reduce": [
      {
        "x": 1369394950,
        "y": 0
      },
      {
        "x": 1369394951,
        "y": 0
      },
      {
        "x": 1369394952,
        "y": 0
      },
      {
        "x": 1369394953,
        "y": 0
      }
    ]
  };

  describe('#coordinatesModify()', function () {
    it('map', function() {
      var new_map = App.jobTimeLineMapper.coordinatesModify(test_input.map);
      expect(new_map.length).to.equal(6);

      expect(new_map[1].y).to.equal(new_map[0].y);
      expect(new_map[2].x).to.equal(new_map[1].x);

      expect(new_map[4].y).to.equal(new_map[5].y);
      expect(new_map[3].x).to.equal(new_map[4].x);
    });
    it('shuffle', function() {
      var new_shuffle = App.jobTimeLineMapper.coordinatesModify(test_input.shuffle);
      expect(new_shuffle.length).to.equal(6);

      expect(new_shuffle[2].y).to.equal(new_shuffle[1].y);
      expect(new_shuffle[3].x).to.equal(new_shuffle[2].x);

      expect(new_shuffle[3].y).to.equal(new_shuffle[4].y);
      expect(new_shuffle[4].x).to.equal(new_shuffle[5].x);
    });
    it('reduce', function() {
      var new_reduce = App.jobTimeLineMapper.coordinatesModify(test_input.reduce);
      expect(new_reduce.length).to.equal(4);
    });
  });
});
