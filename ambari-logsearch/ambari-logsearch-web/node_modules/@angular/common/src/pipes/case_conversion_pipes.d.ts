/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
import { PipeTransform } from '@angular/core';
/**
 * Transforms text to lowercase.
 *
 * {@example  common/pipes/ts/lowerupper_pipe.ts region='LowerUpperPipe' }
 *
 * @stable
 */
export declare class LowerCasePipe implements PipeTransform {
    transform(value: string): string;
}
/**
 * Transforms text to titlecase.
 *
 * @stable
 */
export declare class TitleCasePipe implements PipeTransform {
    transform(value: string): string;
}
/**
 * Transforms text to uppercase.
 *
 * @stable
 */
export declare class UpperCasePipe implements PipeTransform {
    transform(value: string): string;
}
