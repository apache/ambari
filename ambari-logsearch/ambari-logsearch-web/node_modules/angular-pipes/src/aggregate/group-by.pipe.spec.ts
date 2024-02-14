import { GroupByPipe } from './group-by.pipe';


describe('GroupByPipe', () => {

  let pipe: GroupByPipe;
  let input: Array <any>;

  beforeEach(() => {
     pipe = new GroupByPipe();
  });

  it('Should return the groupped data', () => {

    input = [
      {
        'name': 'aaa',
        'value': 'bar'
      }, {
        'name': 'bbb',
        'value': 'bar'
      }, {
        'name': 'ccc',
        'value': 'foo'
      }, {
        'name': 'ddd',
        'value': 'xyz'
      }, {
        'name': 'eee',
        'value': 'foo'
      }
    ];

     expect(pipe.transform(input, 'value')).toEqual([
      {
        'key': 'bar', 'value': [input[0], input[1]]
      },
      {
        'key': 'foo', 'value': [input[2], input[4]]
      },
      {
        'key': 'xyz', 'value': [input[3]]
      }
    ]);
  });

  it('Should return the groupped data #2', () => {
    input = [
      {
        'name': 'aaa',
        'someProperty': {
          'value': 'bar'
        }
      }, {
        'name': 'bbb',
        'someProperty': {
          'value': 'bar'
        }
      }, {
        'name': 'ccc',
        'someProperty': {
          'value': 'foo'
        }
      }, {
        'name': 'ddd',
        'someProperty': {
          'value': 'xyz'
        }
      }, {
        'name': 'eee',
        'someProperty': {
          'value': 'foo'
        }
      }, null, undefined, 1, false
    ];

    expect(pipe.transform(input, 'someProperty.value')).toEqual([
      {
        'key': 'bar', 'value': [input[0], input[1]]
      },
      {
        'key': 'foo', 'value': [input[2], input[4]]
      },
      {
        'key': 'xyz', 'value': [input[3]]
      },
      {
        'key': 'undefined', 'value': [null, undefined, 1, false]
      }
    ]);
  });

  it('Should return the data unchanged', () => {
    expect(pipe.transform(null, 'any')).toEqual(null);
  });

  it('Should return the data unchanged #2', () => {
    expect(pipe.transform([], 'any')).toEqual([]);
  });

});
