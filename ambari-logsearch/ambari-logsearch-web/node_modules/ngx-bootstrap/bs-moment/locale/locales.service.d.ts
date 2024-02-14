import { Locale, LocaleData } from './locale.class';
export declare function getLocale(key: string): Locale;
export declare function listLocales(): string[];
export declare function mergeConfigs(parentConfig: LocaleData, childConfig: LocaleData): {
    [key: string]: any;
};
export declare function getSetGlobalLocale(key: string, values?: LocaleData): string;
export declare function defineLocale(name: string, config?: LocaleData): Locale;
