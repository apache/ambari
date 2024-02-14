import { EventEmitter } from '@angular/core';
import { BsDatepickerViewMode, BsNavigationDirection, DaysCalendarViewModel } from '../../models/index';
export declare class BsDatepickerNavigationViewComponent {
    calendar: DaysCalendarViewModel;
    onNavigate: EventEmitter<BsNavigationDirection>;
    onViewMode: EventEmitter<BsDatepickerViewMode>;
    navTo(down: boolean): void;
    view(viewMode: BsDatepickerViewMode): void;
}
