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

const types = {
  get: (prop: any): string => Object.prototype.toString.call(prop),
  object: "[object Object]",
  array: "[object Array]",
  string: "[object String]",
  boolean: "[object Boolean]",
  number: "[object Number]",
};

type HandlerFunction = (target: any, source: any, ...args: any[]) => any;

export default {
  isChild(obj: any): boolean {
    for (const k in obj) {
      if (obj.hasOwnProperty(k)) {
        if (obj[k] instanceof Object) {
          return false;
        }
      }
    }
    return true;
  },

  /**
   * Recursively parse an object to remove any nested objects.
   * @param {object} obj - The object to parse.
   * @returns {object} - The parsed object.
   */
  parseObject(obj: Record<string, any>): Record<string, any> {
    const res: Record<string, any> = {};
    for (const p in obj) {
      if (Object.prototype.hasOwnProperty.call(obj, p)) {
        if (obj[p] instanceof Object) {
          res[p] = this.parseObject(obj[p]);
        }
      }
    }
    return res;
  },
  /**
   * Recursive function to count the number of keys in an object.
   * @param {object} obj - The object to count keys in.
   * @returns {number|null} - The number of keys or null if the input is not an object.
   */
  recursiveKeysCount(obj: any): number | null {
    if (!(obj instanceof Object)) {
      return null;
    }
    const self = this;
    const r = (obj: any): number => {
      let count = 0;
      for (const k in obj) {
        if (self.isChild(obj[k])) {
          count++;
        } else {
          count += r(obj[k]);
        }
      }
      return count;
    };
    return r(obj);
  },

  deepEqual(...values: any[]): boolean {
    let leftChain: any[];
    let rightChain: any[];

    const compare2Objects = (x: any, y: any): boolean => {
      let p: string;

      if (
        isNaN(x) &&
        isNaN(y) &&
        typeof x === "number" &&
        typeof y === "number"
      ) {
        return true;
      }

      if (x === y) {
        return true;
      }

      if (
        (typeof x === "function" && typeof y === "function") ||
        (x instanceof Date && y instanceof Date) ||
        (x instanceof RegExp && y instanceof RegExp) ||
        (x instanceof String && y instanceof String) ||
        (x instanceof Number && y instanceof Number)
      ) {
        return x.toString() === y.toString();
      }

      if (!(x instanceof Object && y instanceof Object)) {
        return false;
      }

      if (x.isPrototypeOf(y) || y.isPrototypeOf(x)) {
        return false;
      }

      if (x.constructor !== y.constructor) {
        return false;
      }

      if (x.prototype !== y.prototype) {
        return false;
      }

      if (leftChain.includes(x) || rightChain.includes(y)) {
        return false;
      }

      for (p in y) {
        if (y.hasOwnProperty(p) !== x.hasOwnProperty(p)) {
          return false;
        } else if (typeof y[p] !== typeof x[p]) {
          return false;
        }
      }

      for (p in x) {
        if (y.hasOwnProperty(p) !== x.hasOwnProperty(p)) {
          return false;
        } else if (typeof y[p] !== typeof x[p]) {
          return false;
        }
        switch (typeof x[p]) {
          case "object":
          case "function":
            leftChain.push(x);
            rightChain.push(y);
            if (!compare2Objects(x[p], y[p])) {
              return false;
            }
            leftChain.pop();
            rightChain.pop();
            break;
          default:
            if (x[p] !== y[p]) {
              return false;
            }
            break;
        }
      }

      return true;
    };

    if (values.length < 1) {
      return true;
    }

    for (let i = 1; i < values.length; i++) {
      leftChain = [];
      rightChain = [];
      if (!compare2Objects(values[0], values[i])) {
        return false;
      }
    }

    return true;
  },

  recursiveTree(obj: any): string | null {
    if (!(obj instanceof Object)) {
      return null;
    }
    const self = this;
    const r = (obj: any, parent: string): string => {
      let leaf = "";
      for (const k in obj) {
        if (self.isChild(obj[k])) {
          leaf += `${k} (${parent})<br/>`;
        } else {
          leaf += r(obj[k], `${parent}/${k}`);
        }
      }
      return leaf;
    };
    return r(obj, "");
  },

  /**
   *
   * @param {object|array|object[]} target
   * @param {object|array|object[]} source
   * @param {function} handler
   * @returns {object|array|object[]}
   */
  deepMerge(
    target: any,
    source: any,
    handler?: HandlerFunction,
    ...handlerOpts: any[]
  ): any {
    if (typeof target !== "object" || typeof source !== "object") return target;
    const isArray = Array.isArray(source);
    let ret: any =
      handler &&
          //@ts-ignore
      typeof handler.apply(this, [target, source].concat(handlerOpts)) !==
        "undefined"
        ? handler(target, source)
        : isArray
          ? []
          : {};
    const self = this;

    // handle array
    if (isArray) {
      target = target || [];
      ret = ret.concat(target);

      if (types.object === types.get(target[0])) {
        ret = self.smartArrayObjectMerge(target, source);
      } else {
        for (let i = 0; i < source.length; i++) {
          if (typeof ret[i] === "undefined") {
            ret[i] = source[i];
          } else if (typeof source[i] === "object") {
            ret[i] = this.deepMerge(
              target[i],
              source[i],
              handler,
              ...handlerOpts
            );
          } else {
            if (target.indexOf(source[i]) === -1) {
              ret.push(source[i]);
            }
          }
        }
      }
    } else {
      if (target && typeof target === "object") {
        Object.keys(target).forEach((key) => {
          ret[key] = target[key];
        });
      }
      Object.keys(source).forEach((key) => {
        // handle value which is not Array or Object
        if (typeof source[key] !== "object" || !source[key]) {
          ret[key] = source[key];
        } else {
          if (!target[key]) {
            ret[key] = source[key];
          } else {
            ret[key] = self.deepMerge(
              target[key],
              source[key],
              handler,
              ...handlerOpts
            );
          }
        }
      });
    }

    return ret;
  },

  /**
   * Find objects by index key (@see detectIndexedKey) and merge them.
   *
   * @param {object[]} target
   * @param {object[]} source
   * @returns {object[]}
   */
  smartArrayObjectMerge(target: any[], source: any[]): any[] {
    // keep the first object and take all keys that contains primitive value
    const id = this.detectIndexedKey(target);
    const self = this;
    // when uniq key not found let's merge items by the key itself
    if (!id) {
      source.forEach((obj) => {
        Object.keys(obj).forEach((objKey) => {
          const ret = self.objectByRoot(objKey, target);
          if (ret !== undefined) {
            if ([types.object, types.array].includes(types.get(ret))) {
              target[objKey as any] = self.deepMerge(obj[objKey], ret);
            } else {
              target[objKey as any] = ret;
            }
          } else {
            const _obj: any = {};
            _obj[objKey] = obj[objKey];
            target.push(_obj);
          }
        });
      });
      return target;
    }

    return target
      .map((item) => item[id])
      .concat(source.map((item) => item[id]))
      .filter((value, index, self) => self.indexOf(value) === index)
      .map((value) => {
        if (!target.some((item) => item[id] === value)) {
          return source.find((item) => item[id] === value);
        } else if (!source.some((item) => item[id] === value)) {
          return target.find((item) => item[id] === value);
        }
        return self.deepMerge(
          target.find((item) => item[id] === value),
          source.find((item) => item[id] === value)
        );
      });
  },

  /**
   * Determines key with unique value. This key will be used to find correct objects in target and source to merge.
   *
   * @param {object[]} target
   * @returns {string|undefined}
   */
  detectIndexedKey(target: any[]): string | undefined {
    const keys = Object.keys(target[0])
      .map((key) => {
        if ([types.object, types.array].includes(types.get(target[0][key]))) {
          return null;
        }
        return key;
      })
      .filter((key) => key !== null) as string[];

    return keys.filter((key) => {
      const values = target.map((item) => item[key]);
      return values.length === new Set(values).size;
    })[0];
  },

  /**
   *
   * @param {string} rootKey
   * @param {object[]} target
   */
  objectByRoot(rootKey: string, target: any[]): any {
    return target
      .map((item) => item[rootKey] || null)
      .filter((item) => item !== null)[0];
  },
};
