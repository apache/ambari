import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';

@Component({
  selector: 'modal-dialog',
  templateUrl: './modal-dialog.component.html',
  styleUrls: ['./modal-dialog.component.less']
})
export class ModalDialogComponent implements OnInit {

  @Input()
  title: string;

  @Input()
  extraCssClass: string;

  @Input()
  showCloseBtn = true;

  @Input()
  showBackdrop = true;

  @Input()
  closeOnBackdropClick = true;

  @Input()
  visible = false;

  @Output()
  onCloseRequest: EventEmitter<MouseEvent> =  new EventEmitter();

  constructor() { }

  ngOnInit() {
  }

  onCloseBtnClick(event: MouseEvent) {
    this.onCloseRequest.emit(event);
  }

  onBackdropClick(event: MouseEvent) {
    if (this.closeOnBackdropClick) {
      this.onCloseRequest.emit(event);
    }
  }

}
