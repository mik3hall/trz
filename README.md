# trz
Java file system related, currently mostly nio.2 for OS X.

As of recent jdk releases. (Currently working with a jdk18 early access). The DefaultFileSystemProvider that this 
includes works with my HalfPipe application. This is a fairly involved application with my own and 3rd party code that 
should give the file system a pretty good work out.  
  
This is currently a active Eclipse project that I haven't determined how to connect to this git project. I am manually
using diff to keep the DefaultFileSystemProvider part in sync. I am manually doing this for now on the off chance that 
someone might be looking for a working example of a DefaultFileSystemProvider other than the test pass-through one that 
the jdk provides.  
  
My provider pretty much overrides the default solely for the purpose of adding additional Mac native FileAttributeView's.  
  
Anything watch service related is old with unresolved issues that I may or may not get back to sometime.

