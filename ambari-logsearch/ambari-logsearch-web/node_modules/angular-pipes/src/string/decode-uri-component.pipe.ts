import { Pipe, PipeTransform } from '@angular/core';
import { isString } from '../utils/utils';

@Pipe({
  name: 'decodeURIComponent'
})
export class DecodeURIComponentPipe implements PipeTransform {
  
  transform (input: any) {
    
    if (!isString(input)) {
      return input;
    }
    
    return decodeURIComponent(input);
  }
}