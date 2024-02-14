/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
import * as ts from 'typescript';
import { ModuleMetadata } from './schema';
/**
 * A set of collector options to use when collecting metadata.
 */
export declare class CollectorOptions {
    /**
     * Version of the metadata to collect.
     */
    version?: number;
    /**
     * Collect a hidden field "$quoted$" in objects literals that record when the key was quoted in
     * the source.
     */
    quotedNames?: boolean;
    /**
     * Do not simplify invalid expressions.
     */
    verboseInvalidExpression?: boolean;
}
/**
 * Collect decorator metadata from a TypeScript module.
 */
export declare class MetadataCollector {
    private options;
    constructor(options?: CollectorOptions);
    /**
     * Returns a JSON.stringify friendly form describing the decorators of the exported classes from
     * the source file that is expected to correspond to a module.
     */
    getMetadata(sourceFile: ts.SourceFile, strict?: boolean): ModuleMetadata;
}
