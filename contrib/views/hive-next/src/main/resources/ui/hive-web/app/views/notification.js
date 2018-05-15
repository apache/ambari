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

export default Ember.View.extend({
  closeAfter         : 5000,
  isHovering         : false,
  templateName       : 'notification',
  removeNotification : 'removeNotification',

  setup: function () {
    this.set('typeClass', this.get('notification.type.typeClass'));
    this.set('typeIcon', this.get('notification.type.typeIcon'));
  }.on('init'),

  removeLater: function () {
    Ember.run.later(this, function () {
      if (this.get('isHovering')) {
        this.removeLater();
      } else if (this.element) {
        this.send('close');
      }
    }, this.get('closeAfter'));
  }.on('didInsertElement'),

  mouseEnter: function () { this.set('isHovering', true);  },
  mouseLeave: function () { this.set('isHovering', false); },

  actions: {
    close: function () {
      this.remove();
      this.get('parentView').send('removeNotification', this.get('notification'));
    }
  }
});
