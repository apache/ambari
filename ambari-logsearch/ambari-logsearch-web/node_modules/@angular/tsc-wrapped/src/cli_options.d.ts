/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
export declare class CliOptions {
    basePath: string;
    constructor({basePath}: {
        basePath?: string;
    });
}
export declare class I18nExtractionCliOptions extends CliOptions {
    i18nFormat: string | null;
    locale: string | null;
    outFile: string | null;
    constructor({i18nFormat, locale, outFile}: {
        i18nFormat?: string;
        locale?: string;
        outFile?: string;
    });
}
export declare class NgcCliOptions extends CliOptions {
    i18nFormat: string;
    i18nFile: string;
    locale: string;
    missingTranslation: string;
    constructor({i18nFormat, i18nFile, locale, missingTranslation, basePath}: {
        i18nFormat?: string;
        i18nFile?: string;
        locale?: string;
        missingTranslation?: string;
        basePath?: string;
    });
}
