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

import {Injectable} from '@angular/core';
import * as moment from 'moment-timezone';
import {ListItem} from '@app/classes/list-item';
import {HomogeneousObject, LogField} from '@app/classes/object';
import {NodeItem} from '@app/classes/models/node-item';

@Injectable()
export class UtilsService {

  /**
   * Comparison of two instances of any data type be value instead of reference
   * @param valueA
   * @param valueB
   * @returns {boolean}
   */
  isEqual = (valueA: any, valueB: any): boolean => {
    if (valueA === valueB) {
      return true;
    }
    if (valueA instanceof Date && valueB instanceof Date) {
      return valueA.valueOf() === valueB.valueOf();
    }
    if ((typeof valueA === 'function' && typeof valueB === 'function') ||
      (valueA instanceof RegExp && valueB instanceof RegExp) ||
      (valueA instanceof String && valueB instanceof String) ||
      (valueA instanceof Number && valueB instanceof Number) ||
      (valueA instanceof Boolean && valueB instanceof Boolean)) {
      return valueA.toString() === valueB.toString();
    }
    if (!(valueA instanceof Object) || !(valueB instanceof Object)) {
      return false;
    }
    if (valueA.constructor !== valueB.constructor) {
      return false;
    }
    if (valueA.isPrototypeOf(valueB) || valueB.isPrototypeOf(valueA)) {
      return false;
    }
    for (const key in valueA) {
      if (!valueA.hasOwnProperty(key)) {
        continue;
      }
      if (!valueB.hasOwnProperty(key)) {
        return false;
      }
      if (valueA[key] === valueB[key]) {
        continue;
      }
      if (typeof valueA[key] !== 'object' || !this.isEqual(valueA[key], valueB[key])) {
        return false;
      }
    }
    for (const key in valueB) {
      if (valueB.hasOwnProperty(key) && !valueA.hasOwnProperty(key)) {
        return false;
      }
    }
    return true;
  }

  isEnterPressed(event: KeyboardEvent): boolean {
    return event.keyCode === 13;
  }

  isBackSpacePressed(event: KeyboardEvent): boolean {
    return event.keyCode === 8;
  }

  isDifferentDates(dateA, dateB, timeZone): boolean {
    const momentA = moment(dateA).tz(timeZone),
      momentB = moment(dateB).tz(timeZone);
    return !momentA.isSame(momentB, 'day');
  }

  fitIntegerDigitsCount(numberToFormat: number, minLength: number = 2): string {
    return numberToFormat.toLocaleString(undefined, {
      minimumIntegerDigits: minLength
    });
  }

  isEmptyObject(obj: any): boolean {
    return this.isEqual(obj, {});
  }

  getMaxNumberInObject(obj: HomogeneousObject<number>): number {
    const keys = Object.keys(obj);
    return keys.reduce((currentMax: number, currentKey: string): number => {
      return isNaN(obj[currentKey]) ? currentMax : Math.max(currentMax, obj[currentKey]);
    }, 0);
  }

  /**
   * Get instance for dropdown list from string
   * @param name {string}
   * @returns {ListItem}
   */
  getListItemFromString(name: string): ListItem {
    return {
      label: name,
      value: name
    };
  }

  /**
   * Get instance for dropdown list from NodeItem object
   * @param node {NodeItem}
   * @param addGroup {boolean}
   * @returns {ListItem}
   */
  getListItemFromNode(node: NodeItem, addGroup: boolean = false): ListItem {
    const group: string = addGroup && node.group ? `${node.group.label || node.group.name}: ` : '';
    return {
      label: `${group}${node.label || node.name} (${node.value})`,
      value: node.name
    };
  }

  logFieldToListItemMapper<FieldT extends LogField>(fields: FieldT[]): ListItem[] {
    return fields ? fields.map((field: FieldT): ListItem => {
      return {
        value: field.name,
        label: field.label || field.name,
        isChecked: field.visible
      };
    }) : [];
  }

  /**
   * Method that updates source array with only the values which aren't already present there
   * @param {Array} sourceArray
   * @param {Array} itemsToPush
   * @param {Function} [compareFunction=this.isEqual] - custom comparison function;
   * item is skipped if it returns true, and pushed - if false
   * @returns {Array}
   */
  pushUniqueValues = (
    sourceArray: any[], itemsToPush: any[], compareFunction: (x: any, y: any) => boolean = this.isEqual
  ): any[] => {
    itemsToPush.forEach((item: any) => {
      const itemExists = sourceArray.some((sourceItem: any): boolean => compareFunction(item, sourceItem));
      if (!itemExists) {
        sourceArray.push(item);
      }
    });
    return sourceArray;
  }

}
