/**
 * @license
 * Copyright 2016 Palantir Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
 * Enforces the invariant that the input is an array.
 */
export declare function arrayify<T>(arg?: T | T[]): T[];
/**
 * Enforces the invariant that the input is an object.
 */
export declare function objectify(arg: any): any;
/**
 * Replace hyphens in a rule name by upper-casing the letter after them.
 * E.g. "foo-bar" -> "fooBar"
 */
export declare function camelize(stringWithHyphens: string): string;
/**
 * Removes leading indents from a template string without removing all leading whitespace
 */
export declare function dedent(strings: TemplateStringsArray, ...values: string[]): string;
/**
 * Strip comments from file content.
 */
export declare function stripComments(content: string): string;
/**
 * Escapes all special characters in RegExp pattern to avoid broken regular expressions and ensure proper matches
 */
export declare function escapeRegExp(re: string): string;
/** Return true if both parameters are equal. */
export declare type Equal<T> = (a: T, b: T) => boolean;
export declare function arraysAreEqual<T>(a: T[] | undefined, b: T[] | undefined, eq: Equal<T>): boolean;
