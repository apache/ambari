#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <unistd.h>

int main(int argc, char** argv)
{
   if (argc != 2) {
     exit(-1);
   }

   setuid(0);

   pid_t pid = fork();

   if ( pid == 0 ) {
     // execute command in child
     return system(argv[1]);
   }
   return 0;
}
