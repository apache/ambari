import { AngularCompilerOptions, ModuleMetadata } from '@angular/tsc-wrapped';
import * as ts from 'typescript';
import { CompilerHost, CompilerHostContext } from './compiler_host';
/**
 * This version of the AotCompilerHost expects that the program will be compiled
 * and executed with a "path mapped" directory structure, where generated files
 * are in a parallel tree with the sources, and imported using a `./` relative
 * import. This requires using TS `rootDirs` option and also teaching the module
 * loader what to do.
 */
export declare class PathMappedCompilerHost extends CompilerHost {
    constructor(program: ts.Program, options: AngularCompilerOptions, context: CompilerHostContext);
    getCanonicalFileName(fileName: string): string;
    moduleNameToFileName(m: string, containingFile: string): string | null;
    /**
     * We want a moduleId that will appear in import statements in the generated code.
     * These need to be in a form that system.js can load, so absolute file paths don't work.
     * Relativize the paths by checking candidate prefixes of the absolute path, to see if
     * they are resolvable by the moduleResolution strategy from the CompilerHost.
     */
    fileNameToModuleName(importedFile: string, containingFile: string): string;
    getMetadataFor(filePath: string): ModuleMetadata[];
}
