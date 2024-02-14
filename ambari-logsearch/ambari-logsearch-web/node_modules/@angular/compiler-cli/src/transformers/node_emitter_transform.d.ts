/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
import { GeneratedFile } from '@angular/compiler';
import * as ts from 'typescript';
export declare function getAngularEmitterTransformFactory(generatedFiles: GeneratedFile[]): () => (sourceFile: ts.SourceFile) => ts.SourceFile;
