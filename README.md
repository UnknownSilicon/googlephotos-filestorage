# googlephotos-filestorage
Abusing Google Photos' unlimited photo storage
NOTE: This will not work after June 1st, 2021 as Google is no longer offering unlimited photo storage.

## What is this?

This is a Java program that encodes files into images and uploads them to google photos. These photos could then later be retrieved and processed back into files.

## How does it work?

The program takes each byte of the file and encodes it into the r, g, or b channels of each pixel. Each pixel stores 3 bytes of data (although this could easily be increased to 4 bytes by using the alpha channel).

## Usage

Run the jar file using some sort of terminal with:
``` java -jar googlephotos-filestorage-all-VERSION.jar```

The GUI version can be found [Here](https://github.com/jakebacker/GPFS-GUI)

Follow the prompts in the terminal.
