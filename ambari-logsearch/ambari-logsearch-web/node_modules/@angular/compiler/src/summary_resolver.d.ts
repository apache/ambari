/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
import { Type } from '@angular/core';
import { CompileTypeSummary } from './compile_metadata';
export interface Summary<T> {
    symbol: T;
    metadata: any;
    type?: CompileTypeSummary;
}
export declare abstract class SummaryResolver<T> {
    abstract isLibraryFile(fileName: string): boolean;
    abstract getLibraryFileName(fileName: string): string | null;
    abstract resolveSummary(reference: T): Summary<T> | null;
    abstract getSymbolsOf(filePath: string): T[];
    abstract getImportAs(reference: T): T;
    abstract addSummary(summary: Summary<T>): void;
}
export declare class JitSummaryResolver implements SummaryResolver<Type<any>> {
    private _summaries;
    isLibraryFile(fileName: string): boolean;
    getLibraryFileName(fileName: string): string | null;
    resolveSummary(reference: Type<any>): Summary<Type<any>> | null;
    getSymbolsOf(filePath: string): Type<any>[];
    getImportAs(reference: Type<any>): Type<any>;
    addSummary(summary: Summary<Type<any>>): void;
}
