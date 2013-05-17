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

module.exports = {
  /**
   *
   * @param node - DOM element which blinking
   * @param delay - overall time of blinking
   * @param callback
   * @param interval - change frequence of blinking
   */
  pulsate: function (node, delay, callback, interval) {
    var self = this;
    /**
     * execute single blink
     * @param interval - time of single blink
     * @param callback
     * @param opacity
     * @param iteration - current iteration(default amount of iterations: 10)
     * @param isReverse - flag, that mean opacity increase or decrease
     */
    var blink = function (interval, callback, opacity, iteration, isReverse) {
      var iterations = 10;
      opacity = opacity || 1;
      iteration = (iteration !== undefined) ? iteration : 10;
      if (iteration > 0) {
        iteration--;
        setTimeout(function () {
          isReverse = isReverse || (opacity <= 1 / (iterations / 2));
          opacity = (isReverse) ? opacity + (1 / (iterations / 2)) : opacity - (1 / (iterations / 2));
          node.css('opacity', opacity);
          blink(interval, callback, opacity, iteration, isReverse);
        }, interval / iterations);
      } else {
        node.css('opacity', 1);
        callback();
      }
    };
    interval = interval || 200;
    if (delay > 0) {
      delay -= interval;
      setTimeout(function () {
        blink(interval, function () {
          self.pulsate(node, delay, callback, interval);
        });
      }, interval);
    } else {
      callback();
    }
  }
};