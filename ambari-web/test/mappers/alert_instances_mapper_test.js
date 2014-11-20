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

require('mappers/alert_instances_mapper');
var testHelpers = require('test/helpers');

describe('App.alertInstanceMapper', function () {

  var alertInstances = [
      {id: 1},
      {id: 2},
      {id: 3},
      {id: 4}
    ],
    json = {
      "items" : [
        {
          "Alert" : {
            "component_name" : "AMBARI_AGENT",
            "host_name" : "c6401.ambari.apache.org",
            "id" : 2,
            "instance" : null,
            "label" : "Ambari Agent Disk Usage",
            "latest_timestamp" : 1415224354954,
            "maintenance_state" : "OFF",
            "name" : "ambari_agent_disk_usage",
            "original_timestamp" : 1414695835400,
            "scope" : "HOST",
            "service_name" : "AMBARI",
            "state" : "OK",
            "text" : "Capacity Used: [1.26%, 6.6 GB], Capacity Total: [525.3 GB]"
          }
        },
        {
          "Alert" : {
            "component_name" : null,
            "host_name" : null,
            "id" : 3,
            "instance" : null,
            "label" : "Percent DataNodes Available",
            "latest_timestamp" : 1415224362617,
            "maintenance_state" : "OFF",
            "name" : "datanode_process_percent",
            "original_timestamp" : 1414695787466,
            "scope" : "SERVICE",
            "service_name" : "HDFS",
            "state" : "CRITICAL",
            "text" : "affected: [1], total: [1]"
          }
        }
      ]
    };

  beforeEach(function () {

    sinon.stub(App.alertInstanceMapper, 'deleteRecord', Em.K);

    sinon.stub(App.store, 'loadMany', function (type, content) {
      type.content = content;
    });

    App.alertInstanceMapper.model = {
      find: function () {
        if (arguments.length) {
          return alertInstances.findProperty('id', arguments[0]);
        }
        return alertInstances;
      }
    };

  });

  afterEach(function () {

    App.alertInstanceMapper.deleteRecord.restore();
    App.alertInstanceMapper.model = App.AlertInstance;
    App.store.loadMany.restore();

  });

  it('should delete not existing models', function () {

    App.alertInstanceMapper.map(json);

    expect(App.alertInstanceMapper.deleteRecord.calledTwice).to.be.true;
    expect(App.alertInstanceMapper.deleteRecord.args[0][0].id).to.equal(1);
    expect(App.alertInstanceMapper.deleteRecord.args[1][0].id).to.equal(4);

  });

  it('should map alert instances', function () {

    var expected = [
      {
        "id": 2,
        "label": "Ambari Agent Disk Usage",
        "service_id": "AMBARI",
        "component_name": "AMBARI_AGENT",
        "host_id": "c6401.ambari.apache.org",
        "scope": "HOST",
        "original_timestamp": 1414695835400,
        "latest_timestamp": 1415224354954,
        "maintenance_state": "OFF",
        "instance": null,
        "state": "OK",
        "text": "Capacity Used: [1.26%, 6.6 GB], Capacity Total: [525.3 GB]"
      },
      {
        "id": 3,
        "label": "Percent DataNodes Available",
        "service_id": "HDFS",
        "component_name": null,
        "host_id": null,
        "scope": "SERVICE",
        "original_timestamp": 1414695787466,
        "latest_timestamp": 1415224362617,
        "maintenance_state": "OFF",
        "instance": null,
        "state": "CRITICAL",
        "text": "affected: [1], total: [1]"
      }
    ];

    App.alertInstanceMapper.map(json);
    testHelpers.nestedExpect(expected, App.alertInstanceMapper.model.content);

  });

});