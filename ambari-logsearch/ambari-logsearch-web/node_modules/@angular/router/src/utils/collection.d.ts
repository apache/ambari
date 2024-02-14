/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
import { NgModuleFactory } from '@angular/core';
import { Observable } from 'rxjs/Observable';
export declare function shallowEqualArrays(a: any[], b: any[]): boolean;
export declare function shallowEqual(a: {
    [x: string]: any;
}, b: {
    [x: string]: any;
}): boolean;
export declare function flatten<T>(arr: T[][]): T[];
export declare function last<T>(a: T[]): T | null;
export declare function and(bools: boolean[]): boolean;
export declare function forEach<K, V>(map: {
    [key: string]: V;
}, callback: (v: V, k: string) => void): void;
export declare function waitForMap<A, B>(obj: {
    [k: string]: A;
}, fn: (k: string, a: A) => Observable<B>): Observable<{
    [k: string]: B;
}>;
export declare function andObservables(observables: Observable<Observable<any>>): Observable<boolean>;
export declare function wrapIntoObservable<T>(value: T | NgModuleFactory<T> | Promise<T> | Observable<T>): Observable<T>;
