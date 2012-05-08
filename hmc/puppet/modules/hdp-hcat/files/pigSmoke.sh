A = load 'passwd' using PigStorage(':');
B = foreach A generate \$0 as id;
store B into 'pigsmoke.out';
