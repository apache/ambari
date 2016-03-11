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
var stringUtils = require('utils/string_utils');
var timezoneUtils = require('utils/date/timezone');

/**
 * Remove spaces at beginning and ending of line.
 * @example
 *  var str = "  I'm a string  "
 *  str.trim() // return "I'm a string"
 * @method trim
 * @return {string}
 */
String.prototype.trim = function () {
  return this.replace(/^\s\s*/, '').replace(/\s\s*$/, '');
};
/**
 * Determines whether string end within another string.
 *
 * @method endsWith
 * @param suffix {string}  substring for search
 * @return {boolean}
 */
String.prototype.endsWith = function(suffix) {
  return this.indexOf(suffix, this.length - suffix.length) !== -1;
};
/**
 * Determines whether string start within another string.
 *
 * @method startsWith
 * @param prefix {string} substring for search
 * @return {boolean}
 */
String.prototype.startsWith = function (prefix){
  return this.indexOf(prefix) == 0;
};
/**
 * Determines whether string founded within another string.
 *
 * @method contains
 * @param substring {string} substring for search
 * @return {boolean}
 */
String.prototype.contains = function(substring) {
  return this.indexOf(substring) != -1;
};
/**
 * Capitalize the first letter of string.
 * @method capitalize
 * @return {string}
 */
String.prototype.capitalize = function () {
  return this.charAt(0).toUpperCase() + this.slice(1);
};

/**
 * Capitalize the first letter of string.
 * And set to lowercase other part of string
 * @method toCapital
 * @return {string}
 */
String.prototype.toCapital = function () {
  return this.charAt(0).toUpperCase() + this.slice(1).toLowerCase();
};

/**
 * Finds the value in an object where this string is a key.
 * Optionally, the index of the key can be provided where the
 * value of the nth key in the hierarchy is returned.
 *
 * Example:
 *  var tofind = 'smart';
 *  var person = {'name': 'Bob Bob', 'smart': 'no', 'age': '28', 'personality': {'smart': 'yes', 'funny': 'yes', 'emotion': 'happy'} };
 *  tofind.findIn(person); // 'no'
 *  tofind.findIn(person, 0); // 'no'
 *  tofind.findIn(person, 1); // 'yes'
 *  tofind.findIn(person, 2); // null
 *
 *  @method findIn
 *  @param multi {object}
 *  @param index {number} Occurrence count of this key
 *  @return {*} Value of key at given index
 */
String.prototype.findIn = function(multi, index, _foundValues) {
  if (!index) {
    index = 0;
  }
  if (!_foundValues) {
    _foundValues = [];
  }
  multi = multi || '';
  var value = null;
  var str = this.valueOf();
  if (typeof multi == 'object') {
    for ( var key in multi) {
      if (value != null) {
        break;
      }
      if (key == str) {
        _foundValues.push(multi[key]);
      }
      if (_foundValues.length - 1 == index) {
        // Found the value
        return _foundValues[index];
      }
      if (typeof multi[key] == 'object') {
        value = value || this.findIn(multi[key], index, _foundValues);
      }
    }
  }
  return value;
};
/**
 * Replace {i} with argument. where i is number of argument to replace with.
 * @example
 *  var str = "{0} world{1}";
 *  str.format("Hello", "!") // return "Hello world!"
 *
 * @method format
 * @return {string}
 */
String.prototype.format = function () {
  var args = arguments;
  return this.replace(/{(\d+)}/g, function (match, number) {
    return typeof args[number] != 'undefined' ? args[number] : match;
  });
};
/**
 * Wrap words in string within template.
 *
 * @method highlight
 * @param {string[]} words - words to wrap
 * @param {string} [highlightTemplate="<b>{0}</b>"] - template for wrapping
 * @return {string}
 */
String.prototype.highlight = function (words, highlightTemplate) {
  var self = this;
  highlightTemplate = highlightTemplate ? highlightTemplate : "<b>{0}</b>";

  words.forEach(function (word) {
    var searchRegExp = new RegExp("\\b" + word + "\\b", "gi");
    self = self.replace(searchRegExp, function (found) {
      return highlightTemplate.format(found);
    });
  });

  return self;
};
/**
 * Convert time in milliseconds to object contained days, hours and minutes.
 * @typedef ConvertedTime
 *  @type {Object}
 *  @property {number} d - days
 *  @property {number} h - hours
 *  @property {string} m - minutes
 * @example
 *  var time = 1000000000;
 *  time.toDaysHoursMinutes() // {d: 11, h: 13, m: "46.67"}
 *
 * @method toDaysHoursMinutes
 * @return {object}
 */
Number.prototype.toDaysHoursMinutes = function () {
  var formatted = {},
    dateDiff = this,
    secK = 1000, //ms
    minK = 60 * secK, // sec
    hourK = 60 * minK, // sec
    dayK = 24 * hourK;

  dateDiff = parseInt(dateDiff);
  formatted.d = Math.floor(dateDiff / dayK);
  dateDiff -= formatted.d * dayK;
  formatted.h = Math.floor(dateDiff / hourK);
  dateDiff -= formatted.h * hourK;
  formatted.m = (dateDiff / minK).toFixed(2);

  return formatted;
};


/**
 *
 * @param bound1 {Number}
 * @param bound2 {Number}
 * @return {boolean}
 */
Number.prototype.isInRange = function (bound1, bound2) {
  var upperBound, lowerBound;
  upperBound = bound1 > bound2 ? bound1: bound2;
  lowerBound = bound1 < bound2 ? bound1: bound2;
  return this > lowerBound && this < upperBound;
};

/**
 Sort an array by the key specified in the argument.
 Handle only native js objects as element of array, not the Ember's object.

 Can be used as alternative to sortProperty method of Ember library
 in order to speed up executing on large data volumes

 @method sortBy
 @param {String} path name(s) to sort on
 @return {Array} The sorted array.
 */
Array.prototype.sortPropertyLight = function (path) {
  var realPath = (typeof path === "string") ? path.split('.') : [];
  this.sort(function (a, b) {
    var aProperty = a;
    var bProperty = b;
    realPath.forEach(function (key) {
      aProperty = aProperty[key];
      bProperty = bProperty[key];
    });
    if (aProperty > bProperty) return 1;
    if (aProperty < bProperty) return -1;
    return 0;
  });
  return this;
};
/** @namespace Em **/
Em.CoreObject.reopen({
  t:function (key, attrs) {
    return Em.I18n.t(key, attrs)
  }
});

Em.TextArea.reopen(Em.I18n.TranslateableAttributes);

/** @namespace Em.Handlebars **/
Em.Handlebars.registerHelper('log', function (variable) {
  console.log(variable);
});

Em.Handlebars.registerHelper('warn', function (variable) {
  console.warn(variable);
});

Em.Handlebars.registerHelper('highlight', function (property, words, fn) {
  var context = (fn.contexts && fn.contexts[0]) || this;
  property = Em.Handlebars.getPath(context, property, fn);

  words = words.split(";");

//  if (highlightTemplate == undefined) {
  var highlightTemplate = "<b>{0}</b>";
//  }

  words.forEach(function (word) {
    var searchRegExp = new RegExp("\\b" + word + "\\b", "gi");
    property = property.replace(searchRegExp, function (found) {
      return highlightTemplate.format(found);
    });
  });

  return new Em.Handlebars.SafeString(property);
});

Em.Handlebars.registerHelper('isAccessible', function (context, options) {
  if (App.isAccessible(context)) {
    return options.fn(this);
  }
});

/**
 * @namespace App
 */
App = require('app');

/**
 * Certain variables can have JSON in string
 * format, or in JSON format itself.
 *
 * @memberof App
 * @function parseJson
 * @param {string|object}
 * @return {object}
 */
App.parseJSON = function (value) {
  if (typeof value == "string") {
    return jQuery.parseJSON(value);
  }
  return value;
};
/**
 * Check for empty <code>Object</code>, built in Em.isEmpty()
 * doesn't support <code>Object</code> type
 *
 * @memberof App
 * @method isEmptyObject
 * @param obj {Object}
 * @return {Boolean}
 */
App.isEmptyObject = function(obj) {
  var empty = true;
  for (var prop in obj) { if (obj.hasOwnProperty(prop)) {empty = false; break;} }
  return empty;
};

/**
 * Convert object under_score keys to camelCase
 *
 * @param {Object} object
 * @return {Object}
 **/
App.keysUnderscoreToCamelCase = function(object) {
  var tmp = {};
  for (var key in object) {
    tmp[stringUtils.underScoreToCamelCase(key)] = object[key];
  }
  return tmp;
};

/**
 * Convert dotted keys to camelcase
 *
 * @param {Object} object
 * @return {Object}
 **/
App.keysDottedToCamelCase = function(object) {
  var tmp = {};
  for (var key in object) {
    tmp[key.split('.').reduce(function(p, c) { return p + c.capitalize()})] = object[key];
  }
  return tmp;
};
/**
 * Returns object with defined keys only.
 *
 * @memberof App
 * @method permit
 * @param {Object} obj - input object
 * @param {String|Array} keys - allowed keys
 * @return {Object}
 */
App.permit = function(obj, keys) {
  var result = {};
  if (typeof obj !== 'object' || App.isEmptyObject(obj)) return result;
  if (typeof keys == 'string') keys = Array(keys);
  keys.forEach(function(key) {
    if (obj.hasOwnProperty(key))
      result[key] = obj[key];
  });
  return result;
};
/**
 *
 * @namespace App
 * @namespace App.format
 */
App.format = {
  /**
   * @memberof App.format
   * @type {object}
   * @property components
   */
  components: {
    'API': 'API',
    'DECOMMISSION_DATANODE': 'Update Exclude File',
    'DRPC': 'DRPC',
    'FLUME_HANDLER': 'Flume',
    'GLUSTERFS': 'GLUSTERFS',
    'HBASE': 'HBase',
    'HBASE_REGIONSERVER': 'RegionServer',
    'HCAT': 'HCat Client',
    'HDFS': 'HDFS',
    'HISTORYSERVER': 'History Server',
    'HIVE_SERVER': 'HiveServer2',
    'JCE': 'JCE',
    'MAPREDUCE2': 'MapReduce2',
    'MYSQL': 'MySQL',
    'REST': 'REST',
    'SECONDARY_NAMENODE': 'SNameNode',
    'STORM_REST_API': 'Storm REST API Server',
    'WEBHCAT': 'WebHCat',
    'YARN': 'YARN',
    'UI': 'UI',
    'ZKFC': 'ZKFailoverController',
    'ZOOKEEPER': 'ZooKeeper',
    'ZOOKEEPER_QUORUM_SERVICE_CHECK': 'ZK Quorum Service Check'
  },

  /**
   * @memberof App.format
   * @property command
   * @type {object}
   */
  command: {
    'INSTALL': 'Install',
    'UNINSTALL': 'Uninstall',
    'START': 'Start',
    'STOP': 'Stop',
    'EXECUTE': 'Execute',
    'ABORT': 'Abort',
    'UPGRADE': 'Upgrade',
    'RESTART': 'Restart',
    'SERVICE_CHECK': 'Check',
    'Excluded:': 'Decommission:',
    'Included:': 'Recommission:'
  },

  /**
   * cached map of service names
   * @type {object}
   */
  stackServiceRolesMap: {},

  /**
   * cached map of component names
   * @type {object}
   */
  stackComponentRolesMap: {},

  /**
   * convert role to readable string
   *
   * @memberof App.format
   * @method role
   * @param {string} role
   * @param {boolean} isServiceRole
   * return {string}
   */
  role: function (role, isServiceRole) {

    if (isServiceRole) {
      var model = App.StackService;
      var map = this.stackServiceRolesMap;
    } else {
      var model = App.StackServiceComponent;
      var map = this.stackComponentRolesMap;
    }

    this.initializeStackRolesMap(map, model);

    if (map[role]) {
      return map[role];
    }
    return this.normalizeName(role);
  },

  initializeStackRolesMap: function (map, model) {
    if (App.isEmptyObject(map)) {
      model.find().forEach(function (item) {
        map[item.get('id')] = item.get('displayName');
      });
    }
  },

  /**
   * Try to format non predefined names to readable format.
   *
   * @method normalizeName
   * @param name {String} - name to format
   * @return {String}
   */
  normalizeName: function(name) {
    if (!name || typeof name != 'string') return '';
    if (this.components[name]) return this.components[name];
    name = name.toLowerCase();
    var suffixNoSpaces = ['node','tracker','manager'];
    var suffixRegExp = new RegExp('(\\w+)(' + suffixNoSpaces.join('|') + ')', 'gi');
    if (/_/g.test(name)) {
      name = name.split('_').map(function(singleName) {
        return this.normalizeName(singleName.toUpperCase());
      }, this).join(' ');
    } else if(suffixRegExp.test(name)) {
      suffixRegExp.lastIndex = 0;
      var matches = suffixRegExp.exec(name);
      name = matches[1].capitalize() + matches[2].capitalize();
    }
    return name.capitalize();
  },

  /**
   * convert command_detail to readable string, show the string for all tasks name
   *
   * @memberof App.format
   * @method commandDetail
   * @param {string} command_detail
   * @param {string} request_inputs
   * @return {string}
   */
  commandDetail: function (command_detail, request_inputs) {
    var detailArr = command_detail.split(' ');
    var self = this;
    var result = '';
    detailArr.forEach( function(item) {
      // if the item has the pattern SERVICE/COMPONENT, drop the SERVICE part
      if (item.contains('/')) {
        item = item.split('/')[1];
      }
      if (item == 'DECOMMISSION,') {
        // ignore text 'DECOMMISSION,'( command came from 'excluded/included'), here get the component name from request_inputs
        item = (jQuery.parseJSON(request_inputs)) ? jQuery.parseJSON(request_inputs).slave_type : '';
      }
      if (self.components[item]) {
        result = result + ' ' + self.components[item];
      } else if (self.command[item]) {
        result = result + ' ' + self.command[item];
      } else {
        result = result + ' ' + self.role(item, false);
      }
    });

    if (result.indexOf('Decommission:') > -1 || result.indexOf('Recommission:') > -1) {
      // for Decommission command, make sure the hostname is in lower case
       result = result.split(':')[0] + ': ' + result.split(':')[1].toLowerCase();
    }
    //TODO check if UI use this
    if (result === ' Nagios Update Ignore Actionexecute') {
       result = Em.I18n.t('common.maintenance.task');
    }
    if (result.indexOf('Install Packages Actionexecute') != -1) {
      result = Em.I18n.t('common.installRepo.task');
    }
    if (result === ' Rebalancehdfs NameNode') {
       result = Em.I18n.t('services.service.actions.run.rebalanceHdfsNodes.title');
    }
    if (result === " Startdemoldap Knox Gateway") {
      result = Em.I18n.t('services.service.actions.run.startLdapKnox.title');
    }
    if (result === " Stopdemoldap Knox Gateway") {
      result = Em.I18n.t('services.service.actions.run.stopLdapKnox.title');
    }
    if (result === ' Refreshqueues ResourceManager') {
      result = Em.I18n.t('services.service.actions.run.yarnRefreshQueues.title');
    }
    // HAWQ custom commands on back Ops page.
    if (result === ' Resync Hawq Standby HAWQ Standby Master') {
      result = Em.I18n.t('services.service.actions.run.resyncHawqStandby.label');
    }
    if (result === ' Immediate Stop Hawq Service HAWQ Master') {
      result = Em.I18n.t('services.service.actions.run.immediateStopHawqService.label');
    }
    if (result === ' Immediate Stop Hawq Segment HAWQ Segment') {
      result = Em.I18n.t('services.service.actions.run.immediateStopHawqSegment.label');
    }
    if(result === ' Activate Hawq Standby HAWQ Standby Master') {
      result = Em.I18n.t('admin.activateHawqStandby.button.enable');
    }
    if(result === ' Hawq Clear Cache HAWQ Master') {
      result = Em.I18n.t('services.service.actions.run.clearHawqCache.label');
    }
    if(result === ' Run Hawq Check HAWQ Master') {
      result = Em.I18n.t('services.service.actions.run.runHawqCheck.label');
    }
    //<---End HAWQ custom commands--->
    
    return result;
  },

  /**
   * Convert uppercase status name to lowercase.
   * <br>
   * <br>PENDING - Not queued yet for a host
   * <br>QUEUED - Queued for a host
   * <br>IN_PROGRESS - Host reported it is working
   * <br>COMPLETED - Host reported success
   * <br>FAILED - Failed
   * <br>TIMEDOUT - Host did not respond in time
   * <br>ABORTED - Operation was abandoned
   *
   * @memberof App.format
   * @method taskStatus
   * @param {string} _taskStatus
   * @return {string}
   *
   */
  taskStatus:function (_taskStatus) {
    return _taskStatus.toLowerCase();
  },

  /**
   * simplify kdc error msg
   * @param {string} message
   * @param {boolean} strict if this flag is true ignore not defined msgs return null
   *  else return input msg as is;
   * @returns {*}
   */
  kdcErrorMsg: function(message, strict) {
    /**
     * Error messages for KDC administrator credentials error
     * is used for checking if error message is caused by bad KDC credentials
     * @type {{missingKDC: string, invalidKDC: string}}
     */
    var specialMsg = {
      "missingKDC": "Missing KDC administrator credentials.",
      "invalidKDC": "Invalid KDC administrator credentials.",
      "missingRDCForRealm": "Failed to find a KDC for the specified realm - kadmin"
    };

    for (var m in specialMsg) {
      if (specialMsg.hasOwnProperty(m) && message.contains(specialMsg[m]))
        return specialMsg[m];
    }
    return strict ? null : message;
  }
};

/**
 * wrapper to bootstrap popover
 * fix issue when popover stuck on view routing
 *
 * @memberof App
 * @method popover
 * @param {DOMElement} self
 * @param {object} options
 */
App.popover = function (self, options) {
  if (!self) return;
  self.popover(options);
  self.on("remove", function () {
    $(this).trigger('mouseleave').off().removeData('popover');
  });
};

/**
 * wrapper to bootstrap tooltip
 * fix issue when tooltip stuck on view routing
 * @memberof App
 * @method tooltip
 * @param {DOMElement} self
 * @param {object} options
 */
App.tooltip = function (self, options) {
  if (!self) return;
  self.tooltip(options);
  /* istanbul ignore next */
  self.on("remove", function () {
    $(this).trigger('mouseleave').off().removeData('tooltip');
  });
};

/**
 * wrapper to Date().getTime()
 * fix issue when client clock and server clock not sync
 *
 * @memberof App
 * @method dateTime
 * @return {Number} timeStamp of current server clock
 */
App.dateTime = function() {
  return new Date().getTime() + App.clockDistance;
};

/**
 *
 * @param {number} [x] timestamp
 * @returns {number}
 */
App.dateTimeWithTimeZone = function (x) {
  var timezone = App.router.get('userSettingsController.userSettings.timezone');
  if (timezone) {
    var tz = Em.getWithDefault(timezone, 'zones.0.value', '');
    return moment(moment.tz(x ? new Date(x) : new Date(), tz).toArray()).toDate().getTime();
  }
  return x || new Date().getTime();
};

App.formatDateTimeWithTimeZone = function (timeStamp, format) {
  var timezone = App.router.get('userSettingsController.userSettings.timezone'),
    time;
  if (timezone) {
    var tz = Em.getWithDefault(timezone, 'zones.0.value', '');
    time = moment.tz(timeStamp, tz);
  } else {
    time = moment(timeStamp);
  }
  return moment(time).format(format);
};

App.getTimeStampFromLocalTime = function (time) {
  var timezone = App.router.get('userSettingsController.userSettings.timezone'),
    offsetString = '',
    date = moment(time).format('YYYY-MM-DD HH:mm:ss');
  if (timezone) {
    var offset = timezone.utcOffset;
    offsetString = moment().utcOffset(offset).format('Z');
  }
  return moment(date + offsetString).toDate().getTime();
};

/**
 * Helper function for bound property helper registration
 * @memberof App
 * @method registerBoundHelper
 * @param name {String} name of helper
 * @param view {Em.View} view
 */
App.registerBoundHelper = function(name, view) {
  Em.Handlebars.registerHelper(name, function(property, options) {
    options.hash.contentBinding = property;
    return Em.Handlebars.helpers.view.call(this, view, options);
  });
};

/*
 * Return singular or plural word based on Em.I18n, view|controller context property key.
 *
 *  Example: {{pluralize hostsCount singular="t:host" plural="t:hosts"}}
 *           {{pluralize hostsCount singular="@view.hostName"}}
 */
App.registerBoundHelper('pluralize', Em.View.extend({
  tagName: 'span',
  template: Em.Handlebars.compile('{{view.wordOut}}'),

  wordOut: function() {
    var count, singular, plural;
    count = this.get('content');
    singular = this.get('singular');
    plural = this.get('plural');
    return this.getWord(count, singular, plural);
  }.property('content'),
  /**
   * Get computed word.
   *
   * @param {Number} count
   * @param {String} singular
   * @param {String} [plural]
   * @return {String}
   * @method getWord
   */
  getWord: function(count, singular, plural) {
    singular = this.parseValue(singular);
    // if plural not passed
    if (!plural) plural = singular + 's';
    else plural = this.parseValue(plural);
    if (singular && plural) {
      if (count == 1) {
        return singular;
      } else {
        return plural;
      }
    }
    return '';
  },
  /**
   * Detect and return value from its instance.
   *
   * @param {String} value
   * @return {*}
   * @method parseValue
   **/
  parseValue: function(value) {
    switch (value[0]) {
      case '@':
        value = this.getViewPropertyValue(value);
        break;
      case 't':
        value = this.tDetect(value);
        break;
      default:
        break;
    }
    return value;
  },
  /*
   * Detect for Em.I18n.t reference call
   * @params word {String}
   * return {String}
  */
  tDetect: function(word) {
    var splitted = word.split(':');
    if (splitted.length > 1 && splitted[0] == 't') {
      return Em.I18n.t(splitted[1]);
    } else {
      return splitted[0];
    }
  },
  /**
   * Get property value from view|controller by its key path.
   *
   * @param {String} value - key path
   * @return {*}
   * @method getViewPropertyValue
   **/
  getViewPropertyValue: function(value) {
    value = value.substr(1);
    var keyword = value.split('.')[0]; // return 'controller' or 'view'
    switch (keyword) {
      case 'controller':
        return Em.get(this, value);
      case 'view':
        return Em.get(this, value.replace(/^view/, 'parentView'));
      default:
        break;
    }
  }
  })
);
/**
 * Return defined string instead of empty if value is null/undefined
 * by default is `n/a`.
 *
 * @param empty {String} - value instead of empty string (not required)
 *  can be used with Em.I18n pass value started with't:'
 *
 * Examples:
 *
 * default value will be returned
 * {{formatNull service.someValue}}
 *
 * <code>empty<code> will be returned
 * {{formatNull service.someValue empty="I'm empty"}}
 *
 * Em.I18n translation will be returned
 * {{formatNull service.someValue empty="t:my.key.to.translate"
 */
App.registerBoundHelper('formatNull', Em.View.extend({
  tagName: 'span',
  template: Em.Handlebars.compile('{{view.result}}'),

  result: function() {
    var emptyValue = this.get('empty') ? this.get('empty') : Em.I18n.t('services.service.summary.notAvailable');
    emptyValue = emptyValue.startsWith('t:') ? Em.I18n.t(emptyValue.substr(2, emptyValue.length)) : emptyValue;
    return (this.get('content') || this.get('content') == 0) ? this.get('content') : emptyValue;
  }.property('content')
}));

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
  attributeBindings: ["data-original-title"],
  tagName: 'span',
  template: Em.Handlebars.compile('{{{view.result}}}'),

  /**
   * @type {string}
   */
  result: function() {
    return this.get('content') && this.get('content').replace(/\./g, '.<wbr />');
  }.property('content')
}));

/**
 * Return <i></i> with class that correspond to status
 *
 * @param {string} content - status
 *
 * Examples:
 *
 * {{statusIcon view.status}}
 * returns 'icon-cog'
 *
 */
App.registerBoundHelper('statusIcon', Em.View.extend({
  tagName: 'i',

  /**
   * relation map between status and icon class
   * @type {object}
   */
  statusIconMap: {
    'COMPLETED': 'icon-ok completed',
    'WARNING': 'icon-warning-sign',
    'FAILED': 'icon-exclamation-sign failed',
    'HOLDING_FAILED': 'icon-exclamation-sign failed',
    'SKIPPED_FAILED': 'icon-share-alt failed',
    'PENDING': 'icon-cog pending',
    'QUEUED': 'icon-cog queued',
    'IN_PROGRESS': 'icon-cogs in_progress',
    'HOLDING': 'icon-pause',
    'SUSPENDED': 'icon-pause',
    'ABORTED': 'icon-minus aborted',
    'TIMEDOUT': 'icon-time timedout',
    'HOLDING_TIMEDOUT': 'icon-time timedout',
    'SUBITEM_FAILED': 'icon-remove failed'
  },

  classNameBindings: ['iconClass'],
  attributeBindings: ['data-original-title'],

  didInsertElement: function () {
    App.tooltip($(this.get('element')));
  },

  'data-original-title': function() {
    return this.get('content').toCapital();
  }.property('content'),

  /**
   * @type {string}
   */
  iconClass: function () {
    return this.get('statusIconMap')[this.get('content')] || 'icon-question-sign';
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
DS.attr.transforms.date = {
  from: function (serialized) {
    var type = typeof serialized;
    if (type === Em.I18n.t('common.type.string')) {
      serialized = parseInt(serialized);
      type = typeof serialized;
    }
    if (type === Em.I18n.t('common.type.number')) {
      if (!serialized ){  //serialized timestamp = 0;
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
    } else if (deserialized === undefined) {
      return undefined;
    } else {
      return null;
    }
  }
};

DS.attr.transforms.object = {
  from: function(serialized) {
    return Ember.none(serialized) ? null : Object(serialized);
  },

  to: function(deserialized) {
    return Ember.none(deserialized) ? null : Object(deserialized);
  }
};

/**
 * Allows EmberData models to have array properties.
 *
 * Declare the property as <code>
 *  operations: DS.attr('array'),
 * </code> and
 * during load provide a JSON array for value.
 *
 * This transform simply assigns the same array in both directions.
 */
DS.attr.transforms.array = {
  from : function(serialized) {
    return serialized;
  },
  to : function(deserialized) {
    return deserialized;
  }
};

/**
 *  Utility method to delete all existing records of a DS.Model type from the model's associated map and
 *  store's persistence layer (recordCache)
 * @param type DS.Model Class
 */
App.resetDsStoreTypeMap = function(type) {
  var allRecords = App.get('store.recordCache');  //This fetches all records in the ember-data persistence layer
  var typeMaps = App.get('store.typeMaps');
  var guidForType = Em.guidFor(type);
  var typeMap = typeMaps[guidForType];
  if (typeMap) {
    var idToClientIdMap = typeMap.idToCid;
    for (var id in idToClientIdMap) {
      if (idToClientIdMap.hasOwnProperty(id) && idToClientIdMap[id]) {
        delete allRecords[idToClientIdMap[id]];  // deletes the cached copy of the record from the store
      }
    }
    typeMaps[guidForType] = {
      idToCid: {},
      clientIds: [],
      cidToHash: {},
      recordArrays: []
    };
  }
};
