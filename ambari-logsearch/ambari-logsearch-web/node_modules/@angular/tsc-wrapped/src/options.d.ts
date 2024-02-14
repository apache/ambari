/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
import * as ts from 'typescript';
interface Options extends ts.CompilerOptions {
    genDir?: string;
    basePath?: string;
    skipMetadataEmit?: boolean;
    strictMetadataEmit?: boolean;
    skipTemplateCodegen?: boolean;
    flatModuleOutFile?: string;
    flatModuleId?: string;
    generateCodeForLibraries?: boolean;
    annotateForClosureCompiler?: boolean;
    annotationsAs?: 'decorators' | 'static fields';
    trace?: boolean;
    /** @deprecated since v4 this option has no effect anymore. */
    debug?: boolean;
    enableLegacyTemplate?: boolean;
    enableSummariesForJit?: boolean;
    alwaysCompileGeneratedCode?: boolean;
    preserveWhitespaces?: boolean;
}
export default Options;
