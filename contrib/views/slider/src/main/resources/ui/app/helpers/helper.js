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


String.prototype.format = function () {
  var args = arguments;
  return this.replace(/{(\d+)}/g, function (match, number) {
    return typeof args[number] != 'undefined' ? args[number] : match;
  });
};

/**
 * Return formatted string with inserted spaces before upper case and replaced '_' to spaces
 * Also capitalize first letter
 *
 * Examples:
 * 'apple' => 'Apple'
 * 'apple_banana' => 'Apple banana'
 * 'apple_bananaUranium' => 'Apple banana Uranium'
 */
String.prototype.humanize = function () {
  var content = this;
  return content && (content[0].toUpperCase() + content.slice(1)).replace(/([A-Z])/g, ' $1').replace(/_/g, ' ');
}

/**
 * Helper function for bound property helper registration
 * @memberof App
 * @method registerBoundHelper
 * @param name {String} name of helper
 * @param view {Em.View} view
 */
App.registerBoundHelper = function(name, view) {
  Ember.Handlebars.registerHelper(name, function(property, options) {
    options.hash.contentBinding = property;
    return Ember.Handlebars.helpers.view.call(this, view, options);
  });
};

/**
 * Return formatted string with inserted <code>wbr</code>-tag after each dot
 *
 * @param {String} content
 *
 * Examples:
 *
 * returns 'apple'
 * {{formatWordBreak 'apple'}}
 *
 * returns 'apple.<wbr />banana'
 * {{formatWordBreak 'apple.banana'}}
 *
 * returns 'apple.<wbr />banana.<wbr />uranium'
 * {{formatWordBreak 'apple.banana.uranium'}}
 */
App.registerBoundHelper('formatWordBreak', Em.View.extend({
  tagName: 'span',
  template: Ember.Handlebars.compile('{{{view.result}}}'),
  devider:'/',

  /**
   * @type {string}
   */
  result: function() {
    var d = this.get('devider');
    var r = new RegExp('\\'+d,"g");
    return this.get('content') && this.get('content').toString().replace(r, d+'<wbr />');
  }.property('content')
}));

/**
 * Return formatted string with inserted spaces before upper case and replaced '_' to spaces
 * Also capitalize first letter
 *
 * @param {String} content
 *
 * Examples:
 *
 * returns 'apple'
 * {{humanize 'Apple'}}
 *
 * returns 'apple_banana'
 * {{humanize 'Apple banana'}}
 *
 * returns 'apple_bananaUranium'
 * {{humanize 'Apple banana Uranium'}}
 */
App.registerBoundHelper('humanize', Em.View.extend({

  tagName: 'span',

  template: Ember.Handlebars.compile('{{{view.result}}}'),

  /**
   * @type {string}
   */
  result: function() {
    var content = this.get('content');
    return content && content.humanize();
  }.property('content')
}));

/**
 * Ambari overrides the default date transformer.
 * This is done because of the non-standard data
 * sent. For example Nagios sends date as "12345678".
 * The problem is that it is a String and is represented
 * only in seconds whereas Javascript's Date needs
 * milliseconds representation.
 */
DS.attr.transforms = {
  date: {
    from: function (serialized) {
      var type = typeof serialized;
      if (type === 'string') {
        serialized = parseInt(serialized);
        type = typeof serialized;
      }
      if (type === 'number') {
        if (!serialized) {  //serialized timestamp = 0;
          return 0;
        }
        // The number could be seconds or milliseconds.
        // If seconds, then the length is 10
        // If milliseconds, the length is 13
        if (serialized.toString().length < 13) {
          serialized = serialized * 1000;
        }
        return new Date(serialized);
      } else if (serialized === null || serialized === undefined) {
        // if the value is not present in the data,
        // return undefined, not null.
        return serialized;
      } else {
        return null;
      }
    },
    to: function (deserialized) {
      if (deserialized instanceof Date) {
        return deserialized.getTime();
      } else {
        return null;
      }
    }
  }
};
/**
 * Allow get translation value used in I18n for attributes that ends with Translation.
 * For example:
 * <code>
 *  {{input name="new" placeholderTranslation="any"}}
 * </code>
 **/
Em.TextField.reopen(Em.I18n.TranslateableAttributes);