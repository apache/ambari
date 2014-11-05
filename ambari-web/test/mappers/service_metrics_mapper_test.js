/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */


var App = require('app');

describe('App.serviceMetricsMapper', function () {

  describe('#yarnMapper', function () {

    it('should set ACTIVE RM first in any cases (if RM HA enabled)', function() {
      var item = {
          components: [
            {
              ServiceComponentInfo: {
                component_name: 'RESOURCEMANAGER'
              },
              host_components: [
                {
                  HostRoles: {
                    ha_state: null,
                    host_name : 'h1'
                  }
                },
                {
                  HostRoles: {
                    ha_state: 'ACTIVE',
                    host_name : 'h2'
                  },
                  metrics: {
                    yarn: {
                      Queue: {
                        root: {
                          default: {}
                        }
                      }
                    }
                  }
                }
              ]
            }
          ]
        },
        result = App.serviceMetricsMapper.yarnMapper(item);
      expect(result.queue).to.equal("{\"root\":{\"default\":{}}}");
    });

  });

});