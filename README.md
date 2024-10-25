# IN5020 - Chord Protocol (ass 3)

To run the application explicitly, you can use the following examples. Default values for the variables is 10, and aren't necessary.

```sh
make clean all
make run NODE_COUNT=10 BIT_LENGTH=10
make run NODE_COUNT=100 BIT_LENGTH=20
make run NODE_COUNT=1000 BIT_LENGTH=20
```

You can also just `make cleansim` to build the log files for the required cases.


# Additional notes

We prefixed all the packages with  `com.ass3` beacuse an empty groupId is invalid. This required us to make subdirs of `src/`. 

The precode comes with some warning regarding unchecks casts which we fixed by modifying the precode. 