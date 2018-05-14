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

export default Ember.TextField.extend(Ember.I18n.TranslateableProperties, {
  didInsertElement: function () {
    var dynamicValue = this.get('dynamicValue');
    var dynamicContext = this.get('dynamicContext');

    if (dynamicValue && dynamicContext) {
      this.set('value', dynamicContext.get(dynamicValue));
    }
  },

  sendValueChanged: function () {
    var dynamicValue = this.get('dynamicValue');
    var dynamicContext = this.get('dynamicContext');

    if (dynamicValue && dynamicContext) {
      dynamicContext.set(dynamicValue, this.get('value'));
    }

    this.sendAction('valueChanged', this.get('value'));
  },

  keyUp: function (e) {
    //if user has pressed enter
    if (e.keyCode === 13) {
      this.sendAction('valueSearched', this.get('value'));
    } else {
      Ember.run.debounce(this, this.get('sendValueChanged'), 300);
    }
  }
});
