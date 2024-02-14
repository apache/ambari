import { EventEmitter } from '@angular/core';
export interface Notification {
    id?: string;
    type: string;
    icon: string;
    title?: string;
    content?: string;
    override?: any;
    html?: any;
    state?: string;
    createdOn?: Date;
    destroyedOn?: Date;
    animate?: string;
    timeOut?: number;
    maxLength?: number;
    pauseOnHover?: boolean;
    clickToClose?: boolean;
    theClass?: string;
    click?: EventEmitter<{}>;
}
