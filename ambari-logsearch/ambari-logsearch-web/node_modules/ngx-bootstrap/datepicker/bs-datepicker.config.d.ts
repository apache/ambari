import { DatepickerFormatOptions, DatepickerRenderOptions } from './models/index';
export declare class BsDatepickerConfig implements DatepickerRenderOptions, DatepickerFormatOptions {
    value?: Date | Date[];
    isDisabled?: boolean;
    /**
     * Default min date for all date/range pickers
     */
    minDate?: Date;
    /**
     * Default max date for all date/range pickers
     */
    maxDate?: Date;
    /** CSS class which will be applied to datepicker container,
     * usually used to set color theme
     */
    containerClass: string;
    displayMonths: number;
    /**
     * Allows to hide week numbers in datepicker
     */
    showWeekNumbers: boolean;
    dateInputFormat: string;
    rangeSeparator: string;
    rangeInputFormat: string;
    /**
     * Allows to globally set default locale of datepicker,
     * see documentation on how to enable custom locales
     */
    locale: string;
    monthTitle: string;
    yearTitle: string;
    dayLabel: string;
    monthLabel: string;
    yearLabel: string;
    weekNumbers: string;
}
