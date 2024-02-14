import { TimeUnit } from '../types';
export declare function createDate(year?: number, month?: number, day?: number, hour?: number, minute?: number, seconds?: number): Date;
export declare function shiftDate(date: Date, unit: TimeUnit): Date;
export declare function setDate(date: Date, unit: TimeUnit): Date;
