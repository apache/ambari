import { ChangeDetectorRef, PipeTransform, OnDestroy, NgZone } from '@angular/core';
import * as moment from 'moment';
export declare class TimeAgoPipe implements PipeTransform, OnDestroy {
    private cdRef;
    private ngZone;
    private currentTimer;
    private lastTime;
    private lastValue;
    private lastOmitSuffix;
    private lastText;
    constructor(cdRef: ChangeDetectorRef, ngZone: NgZone);
    transform(value: Date | moment.Moment, omitSuffix?: boolean): string;
    ngOnDestroy(): void;
    private createTimer();
    private removeTimer();
    private getSecondsUntilUpdate(momentInstance);
    private hasChanged(value, omitSuffix?);
    private getTime(value);
}
