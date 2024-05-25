# FileSorter, a take on automation in Java

## Java Automate file grouping

Idea: Python is the default automation language , however I want to challenge myself to achieve similar functionality in Java.

### Goal:
A directory watch that group file by file type.
- image folder
- PDF folder
- Or more specific like CST folder
- 
### Usage: 
1. `java src/Sorter`
   - Default - no args
   - By default, we will be watching ~/ Download

   we will check the new item is an image then create an "images" directory
   
   if not exists on the Desktop for Mac (not sure about Windows and Linux yet). move the new images to the "images" directory.
2. `java src/Sorter fromDir toDir`

   - fromDir is the where the system will register the directory,
   - toDir is the destination if the directory doesn't exist, system will create one.
3. `java src/Sorter fromDir toDir -k CST`
   - -k Keyword that filename contains
   
      With the keyword flag new file contains keyword will be moved to directory with the keyword if the directory is doesn't exist, system will create one.
