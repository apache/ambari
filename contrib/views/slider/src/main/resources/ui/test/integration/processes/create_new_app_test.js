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

var appTypes = {
  items: [
    {
      "id": "HBASE",
      "instance_name": "SLIDER_1",
      "typeComponents": [
        {
          "id": "HBASE_MASTER",
          "name": "HBASE_MASTER",
          "displayName": "HBASE_MASTER",
          "instanceCount": 1,
          "maxInstanceCount": 2,
          "yarnMemory": 1024,
          "yarnCpuCores": 1
        },
        {
          "id": "HBASE_REGIONSERVER",
          "name": "HBASE_REGIONSERVER",
          "category": "SLAVE",
          "displayName": "HBASE_REGIONSERVER",
          "priority": 2,
          "instanceCount": 2,
          "maxInstanceCount": 0,
          "yarnMemory": 1024,
          "yarnCpuCores": 1
        }
      ],
      "typeDescription": "Apache HBase is the Hadoop database, a distributed, scalable, big data store.\n        Requirements:\n        1. Ensure parent dir for path (hbase-site/hbase.rootdir) is accessible to the App owner.\n        2. Ensure ZK root (hbase-site/zookeeper.znode.parent) is unique for the App instance.",
      "typeName": "HBASE",
      "typePackageFileName": "hbase_v096.zip",
      "typeVersion": "0.96.0.2.1.1",
      "version": "1.0.0",
      "view_name": "SLIDER",
      "typeConfigs": {
        "agent.conf": "/slider/agent/conf/agent.ini",
        "application.def": "/slider/hbase_v096.zip",
        "config_types": "core-site,hdfs-site,hbase-site",
        "java_home": "/usr/jdk64/jdk1.7.0_45",
        "package_list": "files/hbase-0.96.1-hadoop2-bin.tar.gz",
        "site.core-site.fs.defaultFS": "${NN_URI}",
        "site.global.app_install_dir": "${AGENT_WORK_ROOT}/app/install",
        "site.global.ganglia_server_host": "${NN_HOST}",
        "site.global.ganglia_server_id": "Application1",
        "site.global.ganglia_server_port": "8667",
        "site.global.hbase_master_heapsize": "1024m",
        "site.global.hbase_regionserver_heapsize": "1024m",
        "site.global.security_enabled": "false",
        "site.global.user_group": "hadoop",
        "site.hbase-site.hbase.client.keyvalue.maxsize": "10485760",
        "site.hbase-site.hbase.client.scanner.caching": "100",
        "site.hbase-site.zookeeper.znode.parent": "/hbase-unsecure",
        "site.hdfs-site.dfs.namenode.https-address": "${NN_HOST}:50470"
      }
    }
  ]
};

var selectors = {
    buttonNext: 'button.next-btn',
    step2: {
      content: '#step2 table tbody'
    }
  },
  newApp = {
    name: 'new_app',
    type: 'HBASE',
    components: {
      HBASE_MASTER: 4,
      HBASE_REGIONSERVER: 5
    },
    categoriesCount: 6,
    newConfig: {
      name: 'new_property',
      value: 'new_value'
    }
  };

QUnit.module('integration/processes - Create New App', {

  setup: function () {
    Em.run(App, App.advanceReadiness);
    Em.run(function () {
      App.set('viewEnabled', true); // Important!
      App.ApplicationTypeMapper.parse(appTypes);
    });
  },

  teardown: function () {
    App.reset();
  }

});

test('basic (no errors - just valid data)', function () {

  /* STEP 1 */
  visit('/createAppWizard/step1');
  equal(find(selectors.buttonNext).attr('disabled'), 'disabled', '"Next"-button should be disabled at the beginning of Step 1');
  equal(find('select.type-select option').length, 1, '1 App Type loaded - 1 App Type in select');
  fillIn('#app-name-input', newApp.name);
  andThen(function () {
    equal(find(selectors.buttonNext).attr('disabled'), null, '"Next"-button should be enabled after user input a valid name');
  });
  click(selectors.buttonNext);

  andThen(function () {
    /* STEP 2 */
    equal(currentURL(), '/createAppWizard/step2', 'User comes to Step 2');

    equal(find(selectors.buttonNext).attr('disabled'), null, '"Next"-button should be enabled at the beginning of Step 2');
    equal(find(selectors.step2.content + ' tr').length, 2, 'Selected App Type has 2 components');
    equal(find(selectors.step2.content + ' tr:eq(0) input').val(), '1', 'Component count for 1st component is valid by default');
    equal(find(selectors.step2.content + ' tr:eq(1) input').val(), '2', 'Component count for 2nd component is valid by default');

    fillIn(selectors.step2.content + ' tr:eq(0) input:eq(0)', newApp.components.HBASE_MASTER);
    fillIn(selectors.step2.content + ' tr:eq(1) input:eq(0)', newApp.components.HBASE_REGIONSERVER);

    equal(find(selectors.step2.content + ' tr:eq(0) input:eq(4)').attr('disabled'), 'disabled', 'YARN label input-field should be disabled by default');
    find(selectors.step2.content + ' tr:eq(0) input:eq(3)').click();
    equal(find(selectors.step2.content + ' tr:eq(0) input:eq(4)').attr('disabled'), null, 'YARN label input-field should be enabled after checkbox checked');

    click(selectors.buttonNext);
    andThen(function () {
      /* STEP 3 */
      equal(currentURL(), '/createAppWizard/step3', 'User comes to Step 3');
      equal(find(selectors.buttonNext).attr('disabled'), null, '"Next"-button should be enabled at the beginning of Step 3');
      equal(find('.panel').length, newApp.categoriesCount, 'Config categories count');
    });
    // Add Custom Property
    click('button.btn-link');
    fillIn('.modal-dialog input:eq(0)', newApp.newConfig.name);
    fillIn('.modal-dialog input:eq(1)', newApp.newConfig.value);
    click('.modal-dialog .btn-success');

    click(selectors.buttonNext);

    andThen(function () {
      /* STEP 4 */
      equal(currentURL(), '/createAppWizard/step4', 'User comes to Step 4');
      equal(find(selectors.buttonNext).attr('disabled'), null, '"Next"-button should be enabled at the beginning of Step 4');

      ok(find('#step4').text().indexOf('App Name: ' + newApp.name) > -1, 'App Name exists');
      ok(find('#step4').text().indexOf('App Type: ' + newApp.type) > -1, 'App Type exists');
      ok(find('#step4').text().indexOf('HBASE_MASTER: ' + newApp.components.HBASE_MASTER) > -1, 'HBASE_MASTER count exists');
      ok(find('#step4').text().indexOf('HBASE_REGIONSERVER: ' + newApp.components.HBASE_REGIONSERVER) > -1, 'HBASE_REGIONSERVER count exists');
      ok(find('pre').text().indexOf('"' + newApp.newConfig.name + '":"' + newApp.newConfig.value + '"') > -1, 'Custom property exists');

      click('button.btn-success');
      andThen(function () {
        /* FINISHED */
        equal(currentURL(), '/', 'After click "Finish" user is sent to index page');
      });
    });
  });

});