/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
/**
 * This is a private API for the ngtools toolkit.
 *
 * This API should be stable for NG 2. It can be removed in NG 4..., but should be replaced by
 * something else.
 */
import { AotCompilerHost, StaticReflector } from '@angular/compiler';
export interface LazyRoute {
    routeDef: RouteDef;
    absoluteFilePath: string;
}
export declare type LazyRouteMap = {
    [route: string]: LazyRoute;
};
export declare class RouteDef {
    readonly path: string;
    readonly className: string | null;
    private constructor(path, className?);
    toString(): string;
    static fromString(entry: string): RouteDef;
}
/**
 *
 * @returns {LazyRouteMap}
 * @private
 */
export declare function listLazyRoutesOfModule(entryModule: string, host: AotCompilerHost, reflector: StaticReflector): LazyRouteMap;
export declare function flatten<T>(list: Array<T | T[]>): T[];
