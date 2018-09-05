import { TestBed, inject } from '@angular/core/testing';

import {
  getCommonTestingBedConfiguration,
  TranslationModules
} from '@app/test-config.spec';

import { AppStateService } from '@app/services/storage/app-state.service';

import { LogIndexFilterService } from './log-index-filter.service';

describe('LogIndexFilterService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule(getCommonTestingBedConfiguration({
      imports: [
        ...TranslationModules
      ],
      providers: [
        AppStateService,
        LogIndexFilterService
      ]
    }));
  });

  it('should be created', inject([LogIndexFilterService], (service: LogIndexFilterService) => {
    expect(service).toBeTruthy();
  }));
});
