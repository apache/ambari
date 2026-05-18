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

interface StringUtils {
  pad(str: string, len?: number, pad?: string, dir?: number): string;
  underScoreToCamelCase(name: string): string;
  getCamelCase(name: string | null): string | null;
  compareVersions(first: string, second: string): number;
  isSingleLine(string: string): boolean;
  arrayToCSV(array: Array<Record<string, any>>): string;
  getFileFromPath(path: string): string;
  getPath(path: string): string;
  getFormattedStringFromArray(array: string[], endSeparator?: string): string;
  pluralize(count: number, singular: string, plural?: string): string;
  htmlEntities(string: string): string;
  escapeRegExp(str: string): string;
  getRandomString(len: number, allowed?: string): string;
  upperUnderscoreToText(string: string): string;
  unicodeEscape(string: string, regexp?: RegExp): string;
}
 
const STR_PAD_LEFT = 1;
const STR_PAD_RIGHT = 2;
const STR_PAD_BOTH = 3;
 
const stringUtilsObj: StringUtils = {
  pad(str: string, len: number = 0, pad: string = ' ', dir: number = STR_PAD_RIGHT): string {
    if (len + 1 >= str.length) {
      switch (dir) {
        case STR_PAD_LEFT:
          str = Array(len + 1 - str.length).join(pad) + str;
          break;
        case STR_PAD_BOTH:
          const padlen = len - str.length;
          const right = Math.ceil(padlen / 2);
          const left = padlen - right;
          str = Array(left + 1).join(pad) + str + Array(right + 1).join(pad);
          break;
        default:
          str = str + Array(len + 1 - str.length).join(pad);
          break;
      }
    }
    return str;
  },
 
  underScoreToCamelCase(name: string): string {
    return name.replace(/_\w/g, (str) => str[1].toUpperCase());
  },
 
  getCamelCase(name: string | null): string | null {
    if (name != null) {
      return name.toLowerCase().replace(/(\b\w)/g, (f) => f.toUpperCase());
    }
    return name;
  },
 
  compareVersions(first: string, second: string): number {
    if (!(typeof first === 'string' && typeof second === 'string')) {
      return -1;
    }
    if (first === '' || second === '') {
      return -1;
    }
    const firstNumbers = first.split(/[\.-]/);
    const secondNumbers = second.split(/[\.-]/);
    const length = Math.max(firstNumbers.length, secondNumbers.length);
    
    for (let i = 0; i < length; i++) {
      const firstNum = parseInt(firstNumbers[i] || '0');
      const secondNum = parseInt(secondNumbers[i] || '0');
      
      if (firstNum > secondNum) return 1;
      if (firstNum < secondNum) return -1;
    }
    return 0;
  },
 
  isSingleLine(string: string): boolean {
    return String(string).trim().indexOf("\n") === -1;
  },
 
  arrayToCSV(array: Array<Record<string, any>>): string {
    return array.map(item =>
      Object.values(item).join(',')
    ).join('\n');
  },
 
  getFileFromPath(path: string): string {
    if (!path || typeof path !== 'string') {
      return '';
    }
    return path.replace(/^.*[\/]/, '');
  },
 
  getPath(path: string): string {
    if (!path || typeof path !== 'string' || path[0] !== '/') {
      return '';
    }
    const lastSlash = path.lastIndexOf('/');
    return lastSlash !== 0 ? path.substr(0, lastSlash) : '/';
  },
 
  getFormattedStringFromArray(array: string[], endSeparator: string = 'and'): string {
    if (array.length === 0) return '';
    if (array.length === 1) return array[0];
    
    const lastElement = array[array.length - 1];
    const otherElements = array.slice(0, -1);
    
    return `${otherElements.join(', ')} ${endSeparator} ${lastElement}`;
  },
 
  pluralize(count: number, singular: string, plural?: string): string {
    const _plural = plural || `${singular}s`; // Simple pluralization if not provided
    return count > 1 ? _plural : singular;
  },
 
  htmlEntities(string: string): string {
    if (typeof string !== 'string') return "";
    const div = document.createElement('div');
    div.textContent = string;
    return div.innerHTML;
  },
 
  escapeRegExp(str: string): string {
    return str.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  },
 
  getRandomString(len: number, allowed?: string): string {
    if (len <= 0) {
      throw new Error('len should be defined and more than 0');
    }
    
    const chars = allowed || "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    return Array.from({ length: len }, () =>
      chars.charAt(Math.floor(Math.random() * chars.length))
    ).join('');
  },
 
  upperUnderscoreToText(string: string): string {
    if (typeof string !== 'string') {
      return '';
    }
    return string
      .split('_')
      .map(word => word.toLowerCase().replace(/^\w/, c => c.toUpperCase()))
      .join(' ');
  },
 
  unicodeEscape(string: string, regexp: RegExp = /[\s\S]/g): string {
    return string.replace(regexp, (escape) =>
      '\\u' + ('0000' + escape.charCodeAt(0).toString(16)).slice(-4)
    );
  }
};
 
export default stringUtilsObj;