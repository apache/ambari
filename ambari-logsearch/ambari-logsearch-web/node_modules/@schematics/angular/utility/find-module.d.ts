/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
import { SchematicPath, Tree } from '@angular-devkit/schematics';
export interface ModuleOptions {
    module?: string;
    name: string;
    flat?: boolean;
    sourceDir?: string;
    path?: string;
    skipImport?: boolean;
}
/**
 * Find the module refered by a set of options passed to the schematics.
 */
export declare function findModuleFromOptions(host: Tree, options: ModuleOptions): SchematicPath | undefined;
/**
 * Function to find the "closest" module to a generated file's path.
 */
export declare function findModule(host: Tree, generateDir: string): SchematicPath;
/**
 * Build a relative path from one file path to another file path.
 */
export declare function buildRelativePath(from: string, to: string): string;
