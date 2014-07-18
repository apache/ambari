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

/**
 * Allow to run object method periodically and stop it
 * Example:
 *  <code>
 *    var obj = Ember.Object.createWithMixins(App.RunPeriodically, {
 *      method: Ember.K
 *    });
 *    obj.set('interval', 10000); // override default value
 *    obj.loop('method'); // run periodically
 *    obj.stop(); // stop running
 *  </code>
 * @type {Ember.Mixin}
 */
App.RunPeriodically = Ember.Mixin.create({

  /**
   * Interval for loop
   * @type {number}
   */
  interval: 5000,

  /**
   * setTimeout's return value
   * @type {number}
   */
  timer: null,

  /**
   * Run <code>methodName</code> periodically with <code>interval</code>
   * @param {string} methodName method name to run periodically
   * @param {bool} initRun should methodName be run before setInterval call (default - true)
   * @method run
   */
  loop: function(methodName, initRun) {
    initRun = Em.isNone(initRun) ? true : initRun;
    var self = this,
      interval = this.get('interval');
    Ember.assert('Interval should be numeric and greated than 0', $.isNumeric(interval) && interval > 0);
    if (initRun) {
      this[methodName]();
    }
    this.set('timer',
      setInterval(function () {
        self[methodName]();
      }, interval)
    );
  },

  /**
   * Stop running <code>timer</code>
   * @method stop
   */
  stop: function() {
    var timer = this.get('timer');
    if (!Em.isNone(timer)) {
      clearTimeout(timer);
    }
  }

});
