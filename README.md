# FileSorter, a take on automation in Java

## Java Automate file grouping

Idea: Python is the default automation language , however I want to challenge myself to achieve similar functionality in Java.

Goal:
A directory watch that group file by file type.
- image folder
- PDF folder
- Or more specific like CST folder

Sequence:
1. java Sorter
2. Download folder added new image.jpg
3. Move image.jpg to directory “images”
    1. Create directory if not existed

1. java Sorter -c CST
2. Download folder add new cst-assignment-1.zip
3. Move to CST directory
    1. Create if not exist
    2. Unzip if it is a zip file
