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

import {test} from 'qunit';
import moduleForAcceptance from 'hawq-view/tests/helpers/module-for-acceptance';
import {getMockPayload} from 'hawq-view/tests/helpers/test-helper';
import Utils from  'hawq-view/utils/utils';

moduleForAcceptance('Acceptance | application');

test('visiting /', function (assert) {
  visit('/');

  andThen(function () {
    assert.equal(currentURL(), '/');
    assert.equal(find('img#hawq-logo').length, 1);

    // Test Row Data
    var data = getMockPayload().items;

    for (var i = 0, ii = data.length; i < ii; i++) {
      let rowName = `query-table-row${i}`;
      assert.equal(this.$(`tr#${rowName}`).length, 1);

      let attr = data[i].attributes;
      assert.equal(this.$(`td#${rowName}-pid`).text(), attr['procpid']);
      assert.equal(this.$(`td#${rowName}-databasename`).text(), attr['datname']);
      assert.equal(this.$(`td#${rowName}-duration`).text(), Utils.formatDuration(attr['query_duration']));

      let statusDOM = this.$(`td#${rowName}-status`);
      assert.equal(statusDOM.text(), Utils.generateStatusString(attr['waiting'], attr['waiting_resource']));

      let mockStatusClass = attr['waiting_resource'] ? '' : (attr['waiting'] ? 'orange' : 'green');
      assert.ok(statusDOM.attr('class').match(mockStatusClass));
      assert.equal(this.$(`td#${rowName}-username`).text(), attr['usename']);
      assert.equal(this.$(`td#${rowName}-clientaddress`).text(), Utils.computeClientAddress(attr['client_addr'], attr['client_port']));
    }
  });
});