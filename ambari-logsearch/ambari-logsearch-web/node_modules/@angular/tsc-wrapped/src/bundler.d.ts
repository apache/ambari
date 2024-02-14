import * as ts from 'typescript';
import { MetadataEntry, ModuleMetadata } from './schema';
export interface BundleEntries {
    [name: string]: MetadataEntry;
}
export interface BundlePrivateEntry {
    privateName: string;
    name: string;
    module: string;
}
export interface BundledModule {
    metadata: ModuleMetadata;
    privates: BundlePrivateEntry[];
}
export interface MetadataBundlerHost {
    getMetadataFor(moduleName: string): ModuleMetadata;
}
export declare class MetadataBundler {
    private root;
    private importAs;
    private host;
    private symbolMap;
    private metadataCache;
    private exports;
    private rootModule;
    private exported;
    constructor(root: string, importAs: string | undefined, host: MetadataBundlerHost);
    getMetadataBundle(): BundledModule;
    static resolveModule(importName: string, from: string): string;
    private getMetadata(moduleName);
    private exportAll(moduleName);
    /**
     * Fill in the canonicalSymbol which is the symbol that should be imported by factories.
     * The canonical symbol is the one exported by the index file for the bundle or definition
     * symbol for private symbols that are not exported by bundle index.
     */
    private canonicalizeSymbols(exportedSymbols);
    private canonicalizeSymbol(symbol);
    private getEntries(exportedSymbols);
    private getReExports(exportedSymbols);
    private convertSymbol(symbol);
    private convertEntry(moduleName, value);
    private convertClass(moduleName, value);
    private convertMembers(moduleName, members);
    private convertMember(moduleName, member);
    private convertStatics(moduleName, statics);
    private convertFunction(moduleName, value);
    private convertValue(moduleName, value);
    private convertExpression(moduleName, value);
    private convertError(module, value);
    private convertReference(moduleName, value);
    private convertExpressionNode(moduleName, value);
    private symbolOf(module, name);
    private canonicalSymbolOf(module, name);
}
export declare class CompilerHostAdapter implements MetadataBundlerHost {
    private host;
    private collector;
    constructor(host: ts.CompilerHost);
    getMetadataFor(fileName: string): ModuleMetadata;
}
