import { TakeWhilePipe } from './take-while.pipe';


describe('TakeWhilePipe', () => {
    
    let pipe: TakeWhilePipe;
    
    beforeEach(() => {
       pipe = new TakeWhilePipe(); 
    });
    
    it ('Should return while the condition is met', () => {

      const predicate = (item) => item < 4;

      expect(pipe.transform([1, 2, 3, 4, 1, 5], predicate)).toEqual([1, 2, 3]);
    });

      it ('Should return while the condition is met #2', () => {

      const predicate = (item) => item < 6;

      expect(pipe.transform([1, 2, 3, 4, 5], predicate)).toEqual([1, 2, 3, 4, 5]);
    });


    it('Should return the objects while the condition is met', () => {
       
       const predicate = (item: any) => item.a < 5;

       const array = [{a:2}, {a:3}, {a:5}, {a:4}, {a:1}];
       
       expect(pipe.transform(array, predicate)).toEqual([{a:2}, {a:3}]);
    });

    
    
    it('Should return the input', () => {
        expect(pipe.transform([1])).toEqual([1]);
    });
    
    it('Should return the value unchanged', () => {
       expect(pipe.transform('a', null)).toEqual('a'); 
    });
});