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
 Merge the contents of two objects together into the first object.

 ```javascript
 Ember.merge({first: 'Tom'}, {last: 'Dale'}); // {first: 'Tom', last: 'Dale'}
 var a = {first: 'Yehuda'}, b = {last: 'Katz'};
 Ember.merge(a, b); // a == {first: 'Yehuda', last: 'Katz'}, b == {last: 'Katz'}
 ```

 @method merge
 @for Ember
 @param {Object} original The object to merge into
 @param {Object} updates The object to copy properties from
 @return {Object}
 */
Ember.merge = function(original, updates) {
  for (var prop in updates) {
    if (!updates.hasOwnProperty(prop)) { continue; }
    original[prop] = updates[prop];
  }
  return original;
};

/**
 Returns true if the passed value is null or undefined. This avoids errors
 from JSLint complaining about use of ==, which can be technically
 confusing.

 ```javascript
 Ember.isNone();              // true
 Ember.isNone(null);          // true
 Ember.isNone(undefined);     // true
 Ember.isNone('');            // false
 Ember.isNone([]);            // false
 Ember.isNone(function() {});  // false
 ```

 @method isNone
 @for Ember
 @param {Object} obj Value to test
 @return {Boolean}
 */
Ember.isNone = function(obj) {
  return obj === null || obj === undefined;
};

/**
 Verifies that a value is `null` or an empty string, empty array,
 or empty function.

 Constrains the rules on `Ember.isNone` by returning false for empty
 string and empty arrays.

 ```javascript
 Ember.isEmpty();                // true
 Ember.isEmpty(null);            // true
 Ember.isEmpty(undefined);       // true
 Ember.isEmpty('');              // true
 Ember.isEmpty([]);              // true
 Ember.isEmpty('Adam Hawkins');  // false
 Ember.isEmpty([0,1,2]);         // false
 ```

 @method isEmpty
 @for Ember
 @param {Object} obj Value to test
 @return {Boolean}
 */
Ember.isEmpty = function(obj) {
  return Ember.isNone(obj) || (obj.length === 0 && typeof obj !== 'function') || (typeof obj === 'object' && Ember.get(obj, 'length') === 0);
};

/**
 A value is blank if it is empty or a whitespace string.

 ```javascript
 Ember.isBlank();                // true
 Ember.isBlank(null);            // true
 Ember.isBlank(undefined);       // true
 Ember.isBlank('');              // true
 Ember.isBlank([]);              // true
 Ember.isBlank('\n\t');          // true
 Ember.isBlank('  ');            // true
 Ember.isBlank({});              // false
 Ember.isBlank('\n\t Hello');    // false
 Ember.isBlank('Hello world');   // false
 Ember.isBlank([1,2,3]);         // false
 ```

 @method isBlank
 @for Ember
 @param {Object} obj Value to test
 @return {Boolean}
 */
Ember.isBlank = function(obj) {
  return Ember.isEmpty(obj) || (typeof obj === 'string' && obj.match(/\S/) === null);
};

/**
 * Calculates sum of two numbers
 * Use this function as a callback on <code>invoke</code> etc
 *
 * @method sum
 * @param {Number} a
 * @param {Number} b
 * @returns {Number}
 */
Ember.sum = function (a, b) {
  return a + b;
};

/**
 * Execute passed callback
 *
 * @param {Function} callback
 * @returns {*}
 */
Ember.clb = function (callback) {
  return callback();
};

/**
 *
 */
Ember.RadioButton = Ember.Checkbox.extend({
  tagName: "input",
  type: "radio",
  attributeBindings: [ "type", "name", "value", "checked", "style", "disabled" ],
  style: "vertical-align: middle; margin: 0px;",
  click: function () {
    this.set("selection", this.$().val())
  },
  checked: function () {
    return this.get("value") == this.get("selection");
  }.property('value', 'selection')
});

/**
 * Set value to obj by path
 * Create nested objects if needed
 * Example:
 * <code>
 *   var a = {b: {}};
 *   var path = 'b.c.d';
 *   var value = 1;
 *   Em.setFullPath(a, path, value); // a = {b: {c: {d: 1}}}
 * </code>
 *
 * @param {object} obj
 * @param {string} path
 * @param {*} value
 */
Ember.setFullPath = function (obj, path, value) {
  var parts = path.split('.'),
    sub_path = '';
  parts.forEach(function(_path, _index) {
    Em.assert('path parts can\'t be empty', _path.length);
    sub_path += '.' + _path;
    if (_index === parts.length - 1) {
      Em.set(obj, sub_path, value);
      return;
    }
    if (Em.isNone(Em.get(obj, sub_path))) {
      Em.set(obj, sub_path, {});
    }
  });
};

/**
 *
 * @param {object} target
 * @param {string[]} propertyNames
 * @returns {{}}
 */
Ember.getProperties = function (target, propertyNames) {
  var ret = {};
  for(var i = 0; i < propertyNames.length; i++) {
    ret[propertyNames[i]] = Em.get(target, propertyNames[i]);
  }
  return ret;
};

Em.View.reopen({
  /**
   * overwritten set method of Ember.View to avoid uncaught errors
   * when trying to set property of destroyed view
   */
  set: function(attr, value){
    if(!this.get('isDestroyed') && !this.get('isDestroying')){
      this._super(attr, value);
    } else {
      console.debug('Calling set on destroyed view');
    }
  },

  /**
   * overwritten setProperties method of Ember.View to avoid uncaught errors
   * when trying to set multiple properties of destroyed view
   */
  setProperties: function(hash){
    if(!this.get('isDestroyed') && !this.get('isDestroying')){
      this._super(hash);
    } else {
      console.debug('Calling setProperties on destroyed view');
    }
  }
});

Ember._HandlebarsBoundView.reopen({
  /**
   * overwritten set method of Ember._HandlebarsBoundView to avoid uncaught errors
   * when trying to set property of destroyed view
   */
  render: function(buffer){
    if(!this.get('isDestroyed') && !this.get('isDestroying')){
      this._super(buffer);
    } else {
      console.debug('Calling set on destroyed view');
    }
  }
});

Ember.TextArea.reopen({
  attributeBindings: ['readonly']
});

/**
 * Simply converts query string to object.
 *
 * @param  {string} queryString query string e.g. '?param1=value1&param2=value2'
 * @return {object} converted object
 */
function parseQueryParams(queryString) {
  if (!queryString) {
    return {};
  }
  return queryString.replace(/^\?/, '').split('&').map(decodeURIComponent)
    .reduce(function(p, c) {
      var keyVal = c.split('=');
      p[keyVal[0]] = keyVal[1];
      return p;
    }, {});
};

Ember.Route.reopen({
  /**
   *  When you move to a new route by pressing the back or forward button, change url manually, click on link with url defined in href,
   *  call Router.transitionTo or Router.route this method is called.
   *  This method unites unroutePath, navigateAway and exit events to handle Route leaving in one place.
   *  Also unlike the exit event it is possible to stop transition inside this handler.
   *  To proceed transition just call callback..
   *
   * @param {Router}  router
   * @param {Object|String} context context from transition or path from route
   * @param {callback} callback should be called to proceed transition
   */
  exitRoute: function (router, context, callback) {
    callback();
  },

  /**
   * Query Params serializer. This method should be used inside <code>serialize</code> method.
   * You need to specify `:query` dynamic sygment in your route's <code>route</code> attribute
   * e.g. Em.Route.extend({ route: '/login:query'}) and return result of this method.
   * This method will set <code>serializedQuery</code> property to specified controller by name.
   * For concrete example see `app/routes/main.js`.
   *
   * @example
   *  queryParams: Em.Route.extend({
   *   route: '/queryDemo:query',
   *   serialize: function(route, params) {
   *     return this.serializeQueryParams(route, params, 'controllerNameToSetQueryObject');
   *   }
   *  });
   *  // now when navigated to http://example.com/#/queryDemo?param1=value1&param2=value2
   *  // App.router.get('controllerNameToSetQueryObject').get('serializedQuery')
   *  // will return { param1: 'value1', param2: 'value2' }
   *
   * @param  {Em.Router} router router instance passed to <code>serialize</code> method
   * @param  {object} params dynamic segment passed to <code>seriazlie</code>
   * @param  {string} controllerName name of the controller to set `serializedQuery` as result
   * @return {object}
   */
  serializeQueryParams: function(router, params, controllerName) {
    var controller = router.get(controllerName);
    controller.set('serializedQuery', parseQueryParams(params ? params.query : ''));
    return params || { query: ''};
  }
});

Ember.Router.reopen({

  // reopen original transitionTo and route methods to add calling of exitRoute

  transitionTo: function (router, context) {
    var self = this;
    var args = arguments;
    var transitionTo = self._super;
    var callback = function () {
      transitionTo.apply(self, args);
    };
    if (!this.get('currentState.exitRoute')) {
      callback();
    } else {
      this.get('currentState').exitRoute(this, context, callback);
    }
  },

  route: function (path) {
    var self = this;
    var args = arguments;
    var transitionTo = self._super;
    var callback = function () {
      self.get('location').setURL(path);
      transitionTo.apply(self, args);
    };
    var realPath;
    if (!this.get('currentState.exitRoute')) {
      callback();
    } else {
      realPath = this.get('currentState').absoluteRoute(this);
      this.get('location').setURL(realPath);
      this.get('currentState').exitRoute(this, path, callback);
    }
  }
});
