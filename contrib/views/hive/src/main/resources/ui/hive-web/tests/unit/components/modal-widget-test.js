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

import Ember from 'ember';
import constants from 'hive/utils/constants';
import { moduleForComponent, test } from 'ember-qunit';

moduleForComponent('modal-widget', 'ModalWidgetComponent', {
  needs: ['helper:tb-helper']
});

test('It send ok action on keyPress enter', function(assert) {
  assert.expect(1);

  Ember.run.debounce = function(target, func) {
    func.call(target);
  };

  var component = this.subject({
    ok: 'ok',
    targetObject: {
      ok: function() {
        assert.ok(1, 'OK action sent');
      }
    }
  });

  var $component = this.$();

  component.keyPress({ which: 13 });
  Ember.$('.modal-backdrop').remove(); // remove overlay
});

test('It send close action on keyPress escape', function(assert) {
  assert.expect(1);

  Ember.run.debounce = function(target, func) {
    func.call(target);
  };

  var component = this.subject({
    close: 'close',
    targetObject: {
      close: function() {
        assert.ok(1, 'Close action sent');
      }
    }
  });

  var $component = this.$();

  component.keyPress({ which: 27 });
  Ember.$('.modal-backdrop').remove(); // remove overlay
});
