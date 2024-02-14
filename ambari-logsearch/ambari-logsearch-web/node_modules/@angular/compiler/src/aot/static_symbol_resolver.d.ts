/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
import { SummaryResolver } from '../summary_resolver';
import { StaticSymbol, StaticSymbolCache } from './static_symbol';
export declare class ResolvedStaticSymbol {
    symbol: StaticSymbol;
    metadata: any;
    constructor(symbol: StaticSymbol, metadata: any);
}
/**
 * The host of the SymbolResolverHost disconnects the implementation from TypeScript / other
 * language
 * services and from underlying file systems.
 */
export interface StaticSymbolResolverHost {
    /**
     * Return a ModuleMetadata for the given module.
     * Angular CLI will produce this metadata for a module whenever a .d.ts files is
     * produced and the module has exported variables or classes with decorators. Module metadata can
     * also be produced directly from TypeScript sources by using MetadataCollector in tools/metadata.
     *
     * @param modulePath is a string identifier for a module as an absolute path.
     * @returns the metadata for the given module.
     */
    getMetadataFor(modulePath: string): {
        [key: string]: any;
    }[] | undefined;
    /**
     * Converts a module name that is used in an `import` to a file path.
     * I.e.
     * `path/to/containingFile.ts` containing `import {...} from 'module-name'`.
     */
    moduleNameToFileName(moduleName: string, containingFile?: string): string | null;
    /**
     * Converts a file path to a module name that can be used as an `import.
     * I.e. `path/to/importedFile.ts` should be imported by `path/to/containingFile.ts`.
     *
     * See ImportResolver.
     */
    fileNameToModuleName(importedFilePath: string, containingFilePath: string): string | null;
}
/**
 * This class is responsible for loading metadata per symbol,
 * and normalizing references between symbols.
 *
 * Internally, it only uses symbols without members,
 * and deduces the values for symbols with members based
 * on these symbols.
 */
export declare class StaticSymbolResolver {
    private host;
    private staticSymbolCache;
    private summaryResolver;
    private errorRecorder;
    private metadataCache;
    private resolvedSymbols;
    private resolvedFilePaths;
    private importAs;
    private symbolResourcePaths;
    private symbolFromFile;
    private knownFileNameToModuleNames;
    constructor(host: StaticSymbolResolverHost, staticSymbolCache: StaticSymbolCache, summaryResolver: SummaryResolver<StaticSymbol>, errorRecorder?: (error: any, fileName?: string) => void);
    resolveSymbol(staticSymbol: StaticSymbol): ResolvedStaticSymbol;
    /**
     * getImportAs produces a symbol that can be used to import the given symbol.
     * The import might be different than the symbol if the symbol is exported from
     * a library with a summary; in which case we want to import the symbol from the
     * ngfactory re-export instead of directly to avoid introducing a direct dependency
     * on an otherwise indirect dependency.
     *
     * @param staticSymbol the symbol for which to generate a import symbol
     */
    getImportAs(staticSymbol: StaticSymbol): StaticSymbol | null;
    /**
     * getResourcePath produces the path to the original location of the symbol and should
     * be used to determine the relative location of resource references recorded in
     * symbol metadata.
     */
    getResourcePath(staticSymbol: StaticSymbol): string;
    /**
     * getTypeArity returns the number of generic type parameters the given symbol
     * has. If the symbol is not a type the result is null.
     */
    getTypeArity(staticSymbol: StaticSymbol): number | null;
    /**
     * Converts a file path to a module name that can be used as an `import`.
     */
    fileNameToModuleName(importedFilePath: string, containingFilePath: string): string | null;
    recordImportAs(sourceSymbol: StaticSymbol, targetSymbol: StaticSymbol): void;
    /**
     * Invalidate all information derived from the given file.
     *
     * @param fileName the file to invalidate
     */
    invalidateFile(fileName: string): void;
    private _resolveSymbolMembers(staticSymbol);
    private _resolveSymbolFromSummary(staticSymbol);
    /**
     * getStaticSymbol produces a Type whose metadata is known but whose implementation is not loaded.
     * All types passed to the StaticResolver should be pseudo-types returned by this method.
     *
     * @param declarationFile the absolute path of the file where the symbol is declared
     * @param name the name of the type.
     * @param members a symbol for a static member of the named type
     */
    getStaticSymbol(declarationFile: string, name: string, members?: string[]): StaticSymbol;
    getSymbolsOf(filePath: string): StaticSymbol[];
    private _createSymbolsOf(filePath);
    private createResolvedSymbol(sourceSymbol, topLevelPath, topLevelSymbolNames, metadata);
    private createExport(sourceSymbol, targetSymbol);
    private reportError(error, context?, path?);
    /**
     * @param module an absolute path to a module file.
     */
    private getModuleMetadata(module);
    getSymbolByModule(module: string, symbolName: string, containingFile?: string): StaticSymbol;
    private resolveModule(module, containingFile?);
}
export declare function unescapeIdentifier(identifier: string): string;
