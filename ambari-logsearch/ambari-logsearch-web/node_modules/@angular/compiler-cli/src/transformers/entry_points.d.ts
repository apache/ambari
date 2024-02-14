/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
import * as ts from 'typescript';
import { CompilerHost, CompilerOptions } from './api';
import { createModuleFilenameResolver } from './module_filename_resolver';
export { createProgram } from './program';
export { createModuleFilenameResolver };
export declare function createHost({tsHost, options}: {
    tsHost: ts.CompilerHost;
    options: CompilerOptions;
}): CompilerHost;
