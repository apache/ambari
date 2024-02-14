// Inspired from https://github.com/a8m/angular-filter/blob/master/src/_filter/string/truncate.js

import { Pipe, PipeTransform  } from '@angular/core';
import { isString, isUndefined } from '../utils/utils';

@Pipe({
  name: 'truncate'
})
export class TruncatePipe implements PipeTransform {
  
  transform (input: any, length?: number, suffix?: string, preserve?: boolean): any {
    
    
    if (!isString(input)) {
      return input;
    }
    
    length = isUndefined(length) ? input.length : length;
    
    if (input.length <= length) {
      return input;
    }
    
    preserve = preserve || false;
    suffix = suffix || '';
    let index = length;
    
    if (preserve) {
      if (input.indexOf(' ', length) === -1) {
        index = input.length;
      }
      else {
        index  = input.indexOf(' ', length);
      }
    }
    
    return input.substring(0, index) + suffix;
  }
}