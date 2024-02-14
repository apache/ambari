import { ChangeDetectorRef, PipeTransform, OnDestroy, NgZone } from '@angular/core';
import * as moment from 'moment';
export declare class CalendarPipe implements PipeTransform, OnDestroy {
    private cdRef;
    private ngZone;
    /**
     * @private Internal reference counter, so we can clean up when no instances are in use
     * @type {number}
     */
    private static refs;
    private static timer;
    private static midnight;
    private midnightSub;
    constructor(cdRef: ChangeDetectorRef, ngZone: NgZone);
    transform(value: Date | moment.Moment, ...args: any[]): any;
    ngOnDestroy(): void;
    private static initTimer(ngZone);
    private static removeTimer();
    private static _getMillisecondsUntilUpdate();
}
