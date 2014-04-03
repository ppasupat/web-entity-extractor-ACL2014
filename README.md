# Web Entity Extractor

This repository contains a toolkit for extracting entities from a given search query and web page.

## Requirements

The requirements for running the code include:

* Java 7
* Ruby 1.8.7 or 1.9
* Python 2.7

Other required libraries and resources can be downloaded using the following commands:

* `./download-dependencies core`: download required Java libraries
* `./download-dependencies ling`: download linguistic resources
* `./download-dependencies dataset_debug`: download a small dataset for testing the installation
* `./download-dependencies dataset_openweb`: download the OpenWeb dataset, which contains diverse queries and web pages

## Compiling

Run the following commands to download necessary libraries and compile:

    ./download-dependencies core
    ./download-dependencies ling
    make

## Testing

To train and test on the debug dataset (30 examples) using the default features, run

    ./download-dependencies dataset_debug
    ./web-entity-extractor @mode=main @data=debug @feat=default

For the OpenWeb dataset, make sure the system has enough RAM (~40GB recommended) and run

    ./download-dependencies dataset_openweb
    ./web-entity-extractor @memsize=high @mode=main @data=dev @feat=default -numThreads 0 -fold 3

The flag `-numThreads 0` uses all CPUs available, while `-fold 3` runs the system on 3 random splits of the dataset.
Note that the system may take a long time on the first run to cache all linguistic data.

## Interactive Mode

The interactive mode allows the user to apply the trained model on any query and web page.

To use the interactive mode, first train and save a model by adding `-saveModel [FILENAME]` to one of the commands above, and then run

    ./web-entity-extractor @mode=interactive -loadModel [FILENAME]

## License

The code is under the GNU General Public License (v2). See the `LICENSE` file for the full license.
