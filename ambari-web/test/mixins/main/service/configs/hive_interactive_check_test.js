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

require('mixins/main/service/configs/hive_interactive_check');

var mixin;

describe('App.HiveInteractiveCheck', function () {

  beforeEach(function() {
    mixin = Em.Object.create(App.HiveInteractiveCheck, {});
  });

  describe("#loadHiveConfigs()", function () {

    it("should execute ajax send function", function() {
      mixin.loadHiveConfigs();
      expect(App.ajax.send.calledOnce).to.be.true;
    });
  });

  describe("#onLoadHiveConfigs()", function () {

    it("should set enableHiveInteractive to true", function() {
      var data = {
        items: [
          {
            configurations: [
              {
                type: 'hive-interactive-env',
                properties: {
                  enable_hive_interactive: 'true'
                }
              }
            ]
          },
          {
            configurations: [{type: 'hive-env'}]
          }
        ]
      };
      mixin.onLoadHiveConfigs(data);
      expect(mixin.get('enableHiveInteractive')).to.be.true;
    });

    it("should set enableHiveInteractive to false{1}", function() {
      var data = {
        items: [
          {
            configurations: [
              {
                type: 'hive-interactive-env',
                properties: {
                  enable_hive_interactive: 'false'
                }
              }
            ]
          },
          {
            configurations: [{type: 'hive-env'}]
          }
        ]
      };
      mixin.onLoadHiveConfigs(data);
      expect(mixin.get('enableHiveInteractive')).to.be.false;
    });

    it("should set enableHiveInteractive to false{2}", function() {
      var data = {
        items: [
          {
            configurations: [{type: 'hive'}]
          },
          {
            configurations: [{type: 'hive-env'}]
          }
        ]
      };
      mixin.onLoadHiveConfigs(data);
      expect(mixin.get('enableHiveInteractive')).to.be.false;
    });
  });

});