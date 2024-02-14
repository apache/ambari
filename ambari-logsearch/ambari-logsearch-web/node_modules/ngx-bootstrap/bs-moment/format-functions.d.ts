import { Locale } from './locale/locale.class';
import { DateFormatterFn } from './types';
export declare let formatFunctions: {
    [key: string]: (date: Date, locale: Locale) => string;
};
export declare let formatTokenFunctions: {
    [key: string]: DateFormatterFn;
};
export declare const formattingTokens: RegExp;
export declare function addFormatToken(token: string, padded: {
    [key: number]: any;
}, ordinal: string, callback: DateFormatterFn): void;
export declare function makeFormatFunction(format: string): (date: Date, locale: Locale) => string;
