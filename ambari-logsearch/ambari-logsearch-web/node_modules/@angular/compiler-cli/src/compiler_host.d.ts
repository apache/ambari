/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
import { AotCompilerHost } from '@angular/compiler';
import { AngularCompilerOptions, CollectorOptions, MetadataCollector, ModuleMetadata } from '@angular/tsc-wrapped';
import * as ts from 'typescript';
export interface CompilerHostContext extends ts.ModuleResolutionHost {
    readResource?(fileName: string): Promise<string> | string;
    assumeFileExists(fileName: string): void;
}
export declare class CompilerHost implements AotCompilerHost {
    protected program: ts.Program;
    protected options: AngularCompilerOptions;
    protected context: CompilerHostContext;
    protected metadataCollector: MetadataCollector;
    private isGenDirChildOfRootDir;
    protected basePath: string;
    private genDir;
    private resolverCache;
    private flatModuleIndexCache;
    private flatModuleIndexNames;
    private flatModuleIndexRedirectNames;
    private moduleFileNames;
    protected resolveModuleNameHost: CompilerHostContext;
    constructor(program: ts.Program, options: AngularCompilerOptions, context: CompilerHostContext, collectorOptions?: CollectorOptions);
    getCanonicalFileName(fileName: string): string;
    moduleNameToFileName(m: string, containingFile: string): string | null;
    /**
     * We want a moduleId that will appear in import statements in the generated code.
     * These need to be in a form that system.js can load, so absolute file paths don't work.
     *
     * The `containingFile` is always in the `genDir`, where as the `importedFile` can be in
     * `genDir`, `node_module` or `basePath`.  The `importedFile` is either a generated file or
     * existing file.
     *
     *               | genDir   | node_module |  rootDir
     * --------------+----------+-------------+----------
     * generated     | relative |   relative  |   n/a
     * existing file |   n/a    |   absolute  |  relative(*)
     *
     * NOTE: (*) the relative path is computed depending on `isGenDirChildOfRootDir`.
     */
    fileNameToModuleName(importedFile: string, containingFile: string): string;
    private dotRelative(from, to);
    /**
     * Moves the path into `genDir` folder while preserving the `node_modules` directory.
     */
    private rewriteGenDirPath(filepath);
    protected getSourceFile(filePath: string): ts.SourceFile;
    getMetadataFor(filePath: string): ModuleMetadata[] | undefined;
    readMetadata(filePath: string, dtsFilePath: string): ModuleMetadata[];
    private upgradeVersion1Metadata(v1Metadata, dtsFilePath);
    loadResource(filePath: string): Promise<string> | string;
    loadSummary(filePath: string): string | null;
    getOutputFileName(sourceFilePath: string): string;
    isSourceFile(filePath: string): boolean;
    calculateEmitPath(filePath: string): string;
    private hasBundleIndex(filePath);
}
export declare class CompilerHostContextAdapter {
    protected assumedExists: {
        [fileName: string]: boolean;
    };
    assumeFileExists(fileName: string): void;
}
export declare class ModuleResolutionHostAdapter extends CompilerHostContextAdapter implements CompilerHostContext {
    private host;
    directoryExists: ((directoryName: string) => boolean) | undefined;
    constructor(host: ts.ModuleResolutionHost);
    fileExists(fileName: string): boolean;
    readFile(fileName: string): string;
    readResource(s: string): Promise<string>;
}
export declare class NodeCompilerHostContext extends CompilerHostContextAdapter implements CompilerHostContext {
    fileExists(fileName: string): boolean;
    directoryExists(directoryName: string): boolean;
    readFile(fileName: string): string;
    readResource(s: string): Promise<string>;
}
