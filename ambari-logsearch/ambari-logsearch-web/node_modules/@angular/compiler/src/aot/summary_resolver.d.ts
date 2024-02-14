/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
import { Summary, SummaryResolver } from '../summary_resolver';
import { StaticSymbol, StaticSymbolCache } from './static_symbol';
export interface AotSummaryResolverHost {
    /**
     * Loads an NgModule/Directive/Pipe summary file
     */
    loadSummary(filePath: string): string | null;
    /**
     * Returns whether a file is a source file or not.
     */
    isSourceFile(sourceFilePath: string): boolean;
    /**
     * Returns the output file path of a source file.
     * E.g.
     * `some_file.ts` -> `some_file.d.ts`
     */
    getOutputFileName(sourceFilePath: string): string;
}
export declare class AotSummaryResolver implements SummaryResolver<StaticSymbol> {
    private host;
    private staticSymbolCache;
    private summaryCache;
    private loadedFilePaths;
    private importAs;
    constructor(host: AotSummaryResolverHost, staticSymbolCache: StaticSymbolCache);
    isLibraryFile(filePath: string): boolean;
    getLibraryFileName(filePath: string): string;
    resolveSummary(staticSymbol: StaticSymbol): Summary<StaticSymbol>;
    getSymbolsOf(filePath: string): StaticSymbol[];
    getImportAs(staticSymbol: StaticSymbol): StaticSymbol;
    addSummary(summary: Summary<StaticSymbol>): void;
    private _loadSummaryFile(filePath);
}
