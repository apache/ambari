export interface LocaleOptionsFormat {
    format: string[];
    standalone: string[];
    isFormat?: RegExp;
}
export declare type LocaleOptions = string[] | LocaleOptionsFormat;
export declare const defaultLocaleMonths: string[];
export declare const defaultLocaleMonthsShort: string[];
export declare const defaultLocaleWeekdays: string[];
export declare const defaultLocaleWeekdaysShort: string[];
export declare const defaultLocaleWeekdaysMin: string[];
export declare const defaultLongDateFormat: {
    [index: string]: string;
};
export interface LocaleData {
    [key: string]: any;
    invalidDate?: string;
    abbr?: string;
    months?: LocaleOptions;
    monthsShort?: LocaleOptions;
    weekdays?: LocaleOptions;
    weekdaysMin?: string[];
    weekdaysShort?: string[];
    week?: {
        dow: number;
        doy: number;
    };
    dayOfMonthOrdinalParse?: RegExp;
    meridiemParse?: RegExp;
    ordinal?(num: number, token?: string): string;
    postformat?(num: string): string;
}
export declare class Locale {
    [key: string]: any;
    _abbr: string;
    _config: LocaleData;
    invalidDate: string;
    private _months;
    private _monthsShort;
    private _weekdays;
    private _weekdaysShort;
    private _weekdaysMin;
    private _week;
    private _ordinal;
    constructor(config: LocaleData);
    set(config: LocaleData): void;
    months(date?: Date, format?: string): string | string[];
    monthsShort(date?: Date, format?: string): string | string[];
    weekdays(date?: Date, format?: string): string | string[];
    weekdaysMin(date?: Date): string | string[];
    weekdaysShort(date?: Date): string | string[];
    week(date: Date): number;
    firstDayOfWeek(): number;
    firstDayOfYear(): number;
    meridiem(hours: number, minutes: number, isLower: boolean): string;
    ordinal(num: number, token?: string): string;
    preparse(str: string): string;
    postformat(str: string): string;
    longDateFormat(key: string): string;
}
