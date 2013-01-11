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

  pad: function(str, len, pad, dir) {

    var STR_PAD_LEFT = 1;
    var STR_PAD_RIGHT = 2;
    var STR_PAD_BOTH = 3;

    if (typeof(len) == "undefined") { var len = 0; }
    if (typeof(pad) == "undefined") { var pad = ' '; }
    if (typeof(dir) == "undefined") { var dir = STR_PAD_RIGHT; }

    if (len + 1 >= str.length) {

      switch (dir){

        case STR_PAD_LEFT:
          str = Array(len + 1 - str.length).join(pad) + str;
          break;

        case STR_PAD_BOTH:
          var right = Math.ceil((padlen = len - str.length) / 2);
          var left = padlen - right;
          str = Array(left+1).join(pad) + str + Array(right+1).join(pad);
          break;

        default:
          str = str + Array(len + 1 - str.length).join(pad);
          break;

      } // switch

    }
    return str;

  },
  underScoreToCamelCase: function(string){
    var result = string.split('');
    for(var i = 0; i < result.length; i++){
      if(result[i] === '_'){
        result[i] = result[i+1].toUpperCase();
        result.splice(i+1,1);
      }
    }
    return result.join('');
  }
}
