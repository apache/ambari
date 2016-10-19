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
 * This file contains patched methods for twitter bootstrap.js
 */

/**
 * Fixes error when <code>destroy</code> method called more than one time.
 * For more info check https://github.com/twbs/bootstrap/issues/20511
 */
$.fn.tooltip.Constructor.prototype.destroy = function() {
  var that = this
  clearTimeout(this.timeout)
  this.hide(function () {
    if (that.$element !== null) {
      that.$element.off('.' + that.type).removeData('bs.' + that.type)
    }
    if (that.$tip) {
      that.$tip.detach()
    }
    that.$tip = null
    that.$arrow = null
    that.$viewport = null
    that.$element = null
  })
}
