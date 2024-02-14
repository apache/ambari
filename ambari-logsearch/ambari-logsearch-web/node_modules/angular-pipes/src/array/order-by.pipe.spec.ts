import { OrderByPipe } from './order-by.pipe';


class Person { 
    constructor (public firstName: string, public lastName: string, public age: number) {}
}

describe('OrderByPipe', () => {
    
    let pipe: OrderByPipe;
    
    const fruits: string[] = ["orange", "apple", "pear", "grape", "banana"];
    const numbers: number[] = [1234, 0.214, 8675309, -1, 582];
    const people: Person[] = [
  		    new Person('Linus', 'Torvalds', 46),
  			new Person('Larry', 'Ellison', 71),
            new Person('Mark', 'Zuckerberg', 31),
  			new Person('Sergey', 'Brin', 42),
            new Person('Vint', 'Cerf', 72),
            new Person('Richard', 'Stallman', 62),
            new Person('John', 'Papa', 42)
    ];
    
    beforeEach(() => {
       pipe = new OrderByPipe(); 
    });

    it ('Should return dates in ascending order', () => {

        const a = new Date();
        const b = new Date();
        const result = pipe.transform([a, b], '+');
        expect(result).toEqual([a, b]);
    });

    it ('Should return dates in descending order', () => {

        const a = new Date();
        const b = new Date();
        const result = pipe.transform([a, b], '-');
        expect(result).toEqual([b, a]);
    });
    
    it ('Should return the fruits in ascending order', () => {
       
       const result = pipe.transform(fruits, '+');
       expect(result).toEqual(['apple', 'banana', 'grape', 'orange', 'pear']); 
       expect(fruits).toEqual(["orange", "apple", "pear", "grape", "banana"]); // Check integrity
    });
    
    it ('Should return the fruits in ascending order #2', () => {
       
       const result = pipe.transform(fruits, ['+']);
       expect(result).toEqual(['apple', 'banana', 'grape', 'orange', 'pear']); 
    });
    
    it ('Should return the fruits in descending order', () => {
       
       const result = pipe.transform(fruits, '-');
       expect(result).toEqual(['pear', 'orange', 'grape', 'banana', 'apple']); 
    });
    
    it ('Should return the fruits in descending order #2', () => {
       
       const result = pipe.transform(fruits, ['-']);
       expect(result).toEqual(['pear', 'orange', 'grape', 'banana', 'apple']); 
    });
    
    
    it ('Should return the numbers in ascending order', () => {
       
       const result = pipe.transform(numbers, ['+']);
       expect(result).toEqual([-1, 0.214, 582, 1234, 8675309]); 
    });
    
    it ('Should return the numbers in descending order', () => {
       
       const result = pipe.transform(numbers, ['-']);
       expect(result).toEqual([8675309, 1234, 582, 0.214, -1]); 
    });
    
       
    
    it ('Should return the persons ordered by last name (asc)', () => {
       
       const result = pipe.transform(people, 'lastName');
       expect(result).toEqual([
           people[3], people[4], people[1], people[6], people[5], people[0], people[2]
       ]);
       
    });
    
     it ('Should return the persons ordered by last name (desc)', () => {
       
       const result = pipe.transform(people, '-lastName');
       expect(result).toEqual([
           people[2], people[0], people[5], people[6], people[1], people[4], people[3]
       ]);
       
    });
    
     it ('Should return the persons ordered by last name (asc) #2', () => {
       
       const result = pipe.transform(people, ['lastName']);
       expect(result).toEqual([
           people[3], people[4], people[1], people[6], people[5], people[0], people[2]
       ]);
       
    });
    
     it ('Should return the persons ordered by last name (desc)', () => {
       
       const result = pipe.transform(people, ['-lastName']);
       expect(result).toEqual([
           people[2], people[0], people[5], people[6], people[1], people[4], people[3]
       ]);
       
    });
    
    it ('Should return the persons ordered by age (desc) and firstName (asc)', () => {
       
       const result = pipe.transform(people, ['-age', 'firstName']);
       expect(result).toEqual([
           people[4], people[1], people[5], people[0], people[6], people[3], people[2]
       ]);
       
    });
    
    
    it ('Should return the persons ordered by age (desc) and firstName (asc)', () => {
       
       const result = pipe.transform(people, ['-age', '+firstName']);
       expect(result).toEqual([
           people[4], people[1], people[5], people[0], people[6], people[3], people[2]
       ]);
       
    });

    it('Should order by dates', () => {
        const a = new Date(2017, 1, 10);
        const b = new Date(2016, 1, 10);
        
        const values = [
            a, b
        ];
        const results = [
            b, a
        ];

        expect(pipe.transform(values, '+')).toEqual(results);
    });
    
    it('Should return the input', () => {
        expect(pipe.transform(2)).toEqual(2);
    });

    it('Should return the same array', () => {
        const values = [
            { x: 1, y: 1 },
            { x: 1, y: 1 },
            { x: 1, y: 1 },
            { x: 1, y: 1 },
            { x: 1, y: 1 }
        ];

        expect(
            pipe.transform(values, ['+x', '-y'])
        ).toEqual(values);
    });
});