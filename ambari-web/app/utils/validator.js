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

  isValidEmail: function(value) {
    var emailRegex = /^((([a-z]|\d|[!#\$%&'\*\+\-\/=\?\^_`{\|}~]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])+(\.([a-z]|\d|[!#\$%&'\*\+\-\/=\?\^_`{\|}~]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])+)*)|((\x22)((((\x20|\x09)*(\x0d\x0a))?(\x20|\x09)+)?(([\x01-\x08\x0b\x0c\x0e-\x1f\x7f]|\x21|[\x23-\x5b]|[\x5d-\x7e]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(\\([\x01-\x09\x0b\x0c\x0d-\x7f]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF]))))*(((\x20|\x09)*(\x0d\x0a))?(\x20|\x09)+)?(\x22)))@((([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.)+(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))$/i;
    return emailRegex.test(value);
  },

  isValidInt: function(value) {
    var intRegex = /^-?\d+$/;
    return intRegex.test(value);
  },

  isValidUNIXUser: function(value){
    var regex = /^[a-z_][a-z0-9_-]{0,31}$/;
    return regex.test(value);
  },

  isValidFloat: function(value) {
    if (typeof value === 'string' && value.trim() === '') {
      return false;
    }
    var floatRegex = /^-?(?:\d+|\d{1,3}(?:,\d{3})+)?(?:\.\d+)?$/;
    return floatRegex.test(value);
  },
  /**
   * validate directory with slash at the start
   * @param value
   * @return {Boolean}
   */
  isValidDir: function(value){
    var floatRegex = /^\/[0-9a-z]*/;
    var dirs = value.replace(/,/g,' ').trim().split(new RegExp("\\s+", "g"));
    for(var i = 0; i < dirs.length; i++){
      if(!floatRegex.test(dirs[i])){
        return false;
      }
    }
    return true;
  },

  /**
   * validate directory with slash at the start
   * @param value
   * @returns {boolean}
   */
  isValidDataNodeDir: function(value) {
    var dirRegex = /^(\[[0-9a-zA-Z]+\])?(\/[0-9a-z]*)/;
    var dirs = value.replace(/,/g,' ').trim().split(new RegExp("\\s+", "g"));
    for(var i = 0; i < dirs.length; i++){
      if(!dirRegex.test(dirs[i])){
        return false;
      }
    }
    return true;
  },

  /**
   * validate directory doesn't start "home" or "homes"
   * @param value
   * @returns {boolean}
   */
  isAllowedDir: function(value) {
    var dirs = value.replace(/,/g,' ').trim().split(new RegExp("\\s+", "g"));
    for(var i = 0; i < dirs.length; i++){
      if(dirs[i].startsWith('/home') || dirs[i].startsWith('/homes')) {
        return false;
      }
    }
    return true;
  },

  /**
   * validate ip address with port
   * @param value
   * @return {Boolean}
   */
  isIpAddress: function(value) {
    var ipRegex = /^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)($|\:[0-9]{1,5})$/;
    return ipRegex.test(value);
  },

  /**
   * validate hostname
   * @param value
   * @return {Boolean}
   */
  isHostname: function(value) {
    var regex = /(?=^.{3,254}$)(^([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])(\.([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\-]{0,61}[a-zA-Z0-9]))*(\.[a-zA-Z]{1,62})$)/;
    return regex.test(value);
  },

  hasSpaces: function(value) {
    var regex = /(\s+)/;
    return regex.test(value);
  },

  isNotTrimmed: function(value) {
    var regex = /(^\s+|\s+$)/;
    return regex.test(value);
  },
  /**
   * validate domain name with port
   * @param value
   * @return {Boolean}
   */
  isDomainName: function(value) {
    var domainRegex = /^([a-zA-Z0-9]([a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])?\.)+[a-zA-Z]{2,6}$/;
    return domainRegex.test(value);
  },

  /**
   * validate username
   * @param value
   * @return {Boolean}
   */
  isValidUserName: function(value) {
    var usernameRegex = /^[a-z]([-a-z0-9]{0,30})$/;
    return usernameRegex.test(value);
  },

  /**
   * validate key of configurations
   * @param value
   * @return {Boolean}
   */
  isValidConfigKey: function(value) {
    var configKeyRegex = /^[0-9a-z_\-\.\*]+$/i;
    return configKeyRegex.test(value);
  },

  empty:function (e) {
    switch (e) {
      case "":
      case 0:
      case "0":
      case null:
      case false:
      case undefined:
      case typeof this == "undefined":
        return true;
      default :
        return false;
    }
  },
  /**
   * Validate string that will pass as parameter to .matches() url param.
   * Try to prevent invalid regexp.
   * For example: /api/v1/clusters/c1/hosts?Hosts/host_name.matches(.*localhost.)
   *
   * @params {String} value - string to validate
   * @return {Boolean}
   * @method isValidMatchesRegexp
   */
  isValidMatchesRegexp: function(value) {
    var checkPair = function(chars) {
      chars = chars.map(function(char) { return '\\' + char; });
      var charsReg = new RegExp(chars.join('|'), 'g');
      if (charsReg.test(value)) {
        var pairContentReg = new RegExp(chars.join('.*'), 'g');
        if (!pairContentReg.test(value)) return false;
        var pairCounts = chars.map(function(char) { return value.match(new RegExp(char, 'g')).length; });
        if (pairCounts[0] != pairCounts[1] ) return false;
      }
      return true;
    }
    if (/^[\?\|\*\!,]/.test(value)) return false;
    return /^((\.\*?)?([\w\[\]\?\-_,\|\*\!\{\}]*)?)+(\.\*?)?$/g.test(value) && (checkPair(['[',']'])) && (checkPair(['{','}']));
  },

  /**
  * Remove validation messages for components which are already installed
  */
  filterNotInstalledComponents: function(validationData) {
    var hostComponents = App.HostComponent.find();
    return validationData.resources[0].items.filter(function(item) {
      // true is there is no host with this component
      return hostComponents.filterProperty("componentName", item["component-name"]).filterProperty("hostName", item.host).length === 0;
    });
  }
};
