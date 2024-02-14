import { Pipe, PipeTransform } from '@angular/core';
import { isString } from '../utils/utils';

@Pipe({
  name: 'encodeURI'
})
export class EncodeURIPipe implements PipeTransform {
  
  transform (input: any) {
    
    if (!isString(input)) {
      return input;
    }
    
    return encodeURI(input);
  }
}