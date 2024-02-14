import './locale';
import './units';
import { Locale } from './locale/locale.class';
export declare function formatDate(date: Date, format: string, locale?: string): string;
export declare function formatMoment(date: Date, format: string, locale: Locale): string;
export declare function expandFormat(format: string, locale: Locale): string;
