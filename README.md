# open-sempre

**open-sempre** is a framework for extracting entities from a given search query and web page.

## Requirements

open-sempre requires the following:

* Java 7
* Ruby 1.8.7 or 1.9
* Python 2.7

Other required libraries and resources can be downloaded using the following commands:

* `./download-depenencies core`: download required Java libraries
* `./download-depenencies ling`: download linguistic resources
* `./download-depenencies dataset_demo`: download a small dataset for testing the installation
* `./download-depenencies dataset_openweb`: download the OpenWeb dataset, which contains diverse queries and web pages

## Compiling

Run the following commands to compile:

    ./download-depenencies core
    ./download-depenencies ling
    make

## Testing

To train and test on the demo dataset using the default features, run

    ./download-depenencies dataset_demo
    ./open-sempre @mode=main @data=demo @feat=test

For the OpenWeb dataset, make sure the system has enough RAM (~40GB recommended) and run

    ./download-depenencies dataset_openweb
    ./open-sempre @memsize=high @mode=main @data=dev @feat=test -numThreads 0 -fold 3

The flag `-numThreads 0` uses all CPUs available, while `-fold 3` runs the system on 3 random splits of the dataset. Note that the system may take a long time on the first run to cache all linguistic data.

## Interactive Mode

The interactive mode allows the user to apply the trained model on any query and web page.

To use the interactive mode, first train and save a model by adding `-saveModel [FILENAME]` to one of the commands above, and then run

    ./open-sempre @mode=interactive -loadModel [FILENAME]

## License

open-sempre is under the GNU General Public License (v2). See the `LICENSE` file for the full license.
