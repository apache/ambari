import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import {
  getCommonTestingBedConfiguration, MockHttpRequestModules,
  TranslationModules
} from '@app/test-config.spec';

import { ModalDialogComponent } from './modal-dialog.component';

describe('ModalDialogComponent', () => {
  let component: ModalDialogComponent;
  let fixture: ComponentFixture<ModalDialogComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule(getCommonTestingBedConfiguration({
      imports: [
        ...TranslationModules
      ],
      declarations: [ ModalDialogComponent ]
    }))
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ModalDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
