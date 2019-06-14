# googlephotos-filestorage
Abusing Google Photos' unlimited photo storage

## What is this?

This is a Java program that encodes files into images and uploads them to google photos. These photos could then later be retrieved and processed back into files.

## How does it work?

The program takes each byte of the file and encodes it into the r, g, or b channels of each pixel. Each pixel stores 3 bytes of data (although this could easily be increased to 4 bytes by using the alpha channel).

## Usage

Run the jar file using some sort of terminal with:
``` java -jar googlephotos-filestorage-all-VERSION.jar```

Follow the prompts in the terminal.

## Plans

- [ ] Implement the alpha channel to allow for increased data density
- [x] Add checksum verification
- [x] Automatic Upload
- [x] List available files
- [x] Automatic Retrieval
- [x] Support for larger files (break down into smaller files)
- [ ] Add Error Correcting
