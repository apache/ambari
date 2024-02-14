import {Pipe, PipeTransform} from '@angular/core';
import {isString} from '../utils/utils';

@Pipe({ name: 'slugify' })
export class SlugifyPipe implements PipeTransform {

  transform(input: any): any {

    if (!isString(input)) {
      return input;
    }
    
    return (
      input
      .toLowerCase()
      .trim()
      .replace(/[^a-z0-9\s-]/g, ' ')
      .replace(/[\s-]+/g, '-')
    );
  }
}
