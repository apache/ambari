/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
import { MissingTranslationStrategy, ViewEncapsulation } from '@angular/core';
export declare class CompilerConfig {
    defaultEncapsulation: ViewEncapsulation | null;
    enableLegacyTemplate: boolean;
    useJit: boolean;
    missingTranslation: MissingTranslationStrategy | null;
    preserveWhitespaces: boolean;
    constructor({defaultEncapsulation, useJit, missingTranslation, enableLegacyTemplate, preserveWhitespaces}?: {
        defaultEncapsulation?: ViewEncapsulation;
        useJit?: boolean;
        missingTranslation?: MissingTranslationStrategy;
        enableLegacyTemplate?: boolean;
        preserveWhitespaces?: boolean;
    });
}
export declare function preserveWhitespacesDefault(preserveWhitespacesOption: boolean | null, defaultSetting?: boolean): boolean;
