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

import { module } from 'qunit';
import startApp from 'hawq-view/tests/helpers/start-app';
import destroyApp from 'hawq-view/tests/helpers/destroy-app';
import Pretender from 'pretender';
import { getMockPayload } from 'hawq-view/tests/helpers/test-helper';
import Utils from 'hawq-view/utils/utils';

let server;

export default function(name, options = {}) {
  module(name, {
    beforeEach() {
      this.application = startApp();
      server = new Pretender(function() {
        this.get(Utils.getNamespace() + '/queries', function () {
          return [200, {'Content-Type': 'application/json'}, JSON.stringify(getMockPayload())];
        });
      });

      if (options.beforeEach) {
        options.beforeEach.apply(this, arguments);
      }
    },

    afterEach() {
      if (options.afterEach) {
        options.afterEach.apply(this, arguments);
      }

      server.shutdown();
      destroyApp(this.application);
    }
  });
}
