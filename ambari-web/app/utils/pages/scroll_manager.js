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

var App = require('app');

function calculatePosition(movedTop, defaultTop, scrollTop, defaultLeft, scrollLeft) {
  var css = {
    top: 'auto',
    left: 'auto'
  };
  if (scrollTop >= defaultTop) {
    css.top = movedTop;
  } else if (scrollTop > 0) {
    css.top = (defaultTop - scrollTop) + 'px';
  }
  //if (css.top == '0px') debugger;
  if (scrollLeft > 0) {
    css.left = defaultLeft;
  }
  return css;
}

/**
 *
 * @type {Em.Object}
 */
App.ScrollManager = Em.Object.create({

  /**
   @typedef FixedElement
   @type {object}
   @property {string} id - identifier for element (not css-id). used to determine if element already exists in the list !IMPORTANT - don't push same element two times!
   @property {string} updatedElementSelector - css-selector for element that should be replaced
   @property {string} elementForLeftOffsetSelector - css-selector for element which is used to determine left-offset for <code>updatedElementSelector</code>-element
   @property {number} defaultTop - value for top-offset when user scrolls not under the <code>updatedElementSelector</code>-element
   @property {number} movedTop - value for top-offset when user scrolls  under the <code>updatedElementSelector</code>-element
   */


  /**
   * List of elements that should be placed on the top of the page
   * @type {FixedElement[]}
   */
  elements: [],

  /**
   * Recalculate position for each elements of the <code>elements</code>
   * Should be called from some view
   *
   * @method updatePositionForElements
   */
  updatePositionForElements: function () {
    var self = this;
    //reset defaultTop value in closure
    $(window).unbind('scroll');

    $(window).on('scroll', function () {
      self.get('elements').forEach(function (element) {
        var defaultTop, defaultLeft;
        var infoBar = $(Em.get(element, 'updatedElementSelector'));
        var versionSlider = $(Em.get(element, 'elementForLeftOffsetSelector'));
        var scrollTop = $(window).scrollTop();
        var scrollLeft = $(window).scrollLeft();
        if (!infoBar.length) {
          return;
        }
        defaultTop = (infoBar.get(0).getBoundingClientRect() && infoBar.get(0).getBoundingClientRect().top) || Em.get(element, 'defaultTop');
        // keep the version info bar always aligned to version slider
        defaultLeft = (versionSlider.get(0).getBoundingClientRect() && versionSlider.get(0).getBoundingClientRect().left);
        infoBar.css(calculatePosition(Em.get(element, 'movedTop'), defaultTop, scrollTop, defaultLeft, scrollLeft));
      });
    });

  }

});
