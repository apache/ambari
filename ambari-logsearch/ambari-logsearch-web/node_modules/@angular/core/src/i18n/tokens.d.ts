/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
import { InjectionToken } from '../di/injection_token';
/**
 * @experimental i18n support is experimental.
 */
export declare const LOCALE_ID: InjectionToken<string>;
/**
 * @experimental i18n support is experimental.
 */
export declare const TRANSLATIONS: InjectionToken<string>;
/**
 * @experimental i18n support is experimental.
 */
export declare const TRANSLATIONS_FORMAT: InjectionToken<string>;
/**
 * @experimental i18n support is experimental.
 */
export declare enum MissingTranslationStrategy {
    Error = 0,
    Warning = 1,
    Ignore = 2,
}
