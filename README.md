# googlephotos-filestorage
Abusing Google Photos' unlimited photo storage

## What is this?

This is a Java program that encodes files into images and uploads them to google photos. These photos could then later be retrieved and processed back into files.

## How does it work?

The program takes each byte of the file and encodes it into the r, g, or b channels of each pixel. Each pixel stores 3 bytes of data (although this could easily be increased to 4 bytes by using the alpha channel).

## Plans

- Implement the alpha channel to allow for increased data density
- Add checksum verification
