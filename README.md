# IN5020 - Chord Protocol (ass 3)

To run the application explicitly, you can use the following examples. Default values for the variables is 10, and aren't necessary.

```sh
make clean all
make run NODE_COUNT=10 BIT_LENGTH=10
make run NODE_COUNT=100 BIT_LENGTH=20
make run NODE_COUNT=1000 BIT_LENGTH=20
```

You can also just `make clean sim` to build the log files for the required cases.


# Additional notes

We prefixed all the packages with  `com.ass3` beacuse an empty groupId is invalid. This required us to make subdirs of `src/`. 

The precode comes with some warning regarding unchecks casts which we fixed by modifying the precode's `ChordProtocolSimulator.checkResponse` function.

We also made a bunch of test files to make it easier to verify our diferent implementations. On top of the required implementation functions, we also made a `FingerTable` class with a `FingerTableEntry` wrapper to make printing smooth and easy. We also have a `DataEntry` wrapper for our key-value pairs. Both of these are just `Object` types in the precode example, so casting has to be done with care.


# Implementation
The assignment suggested to fill out the implementation for three functions: `buildOverlayNetwork`, `buildFingerTable`, and `lookUp`. 

The first step, to build the overlay network, we get all the nodes available in the network topology, give them the indexes which correspond to the hash of their name, and link them to each other in a node-to-successor fashion. This ends up forming a ring, where each node can potentially reach any other node. No node will be left alone and depressed.

A finger table is essentially a map for the nodes to figure out the quickest way to find someone responsible for a key. Each node has its own set of M fingers in its table (per the protocol specification), where the fingers can both point to the same successor, or even point to itself. Each finger has a start and an end value, both inclusive, which represents the range of indexes which correspond to the successor in its row. The way these ranges are build up makes it so there is no overlap of ranges, but all values in the index pool will be in exactly one of the M fingers.

The lookup function is what allows us to go from a single position to any other position in our ring by jumping between nodes. There are basically three scenarios here. If the index of our node matches the index for the key we're looking for, it means we can immediatelly return. Otherwise, we have to go either forward or backwards. If the key is between us and the next "bus", it means the next bus will have to pick up the key, and we can return it early. Otherwise, we have to backtrack through our fingertable to find someone closer, iteratively.


# Contribution
As usual, Lise and Mazunki worked au pair. Mazunki mostly set up the test files and makefile scripting stuff, while Lise researched how to implement the protocol correctly. Mouaz verified the output and delivery was up to standard, plus some additional cleanup here and there.

