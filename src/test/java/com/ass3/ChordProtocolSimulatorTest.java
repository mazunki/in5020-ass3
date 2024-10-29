package com.ass3;

import com.ass3.ChordProtocolSimulator;
import com.ass3.p2p.*;
import com.ass3.protocol.*;
import com.ass3.crypto.*;

public class ChordProtocolSimulatorTest {

    public static void main(String[] args) {
        testCreateInstance();
        testAssignKeys();
        System.out.println("All ChordProtocolSimulator tests passed.");
    }

    public static void testCreateInstance() {
        Network network = Network.createNetwork("test", 10);
        ChordProtocolSimulator simulator = ChordProtocolSimulator.getInstance(network, 5, 20);
        Assert.assertTrue(simulator != null, "ChordProtocolSimulator instance should be created");
        Assert.assertEquals(10, simulator.nodeCount, "Node count should be set to 10");
    }

    public static void testAssignKeys() {
        Network network = Network.createNetwork("test", 10);
        ChordProtocolSimulator simulator = ChordProtocolSimulator.getInstance(network, 5, 20);
        simulator.assignKeys();
		NodeInterface node = network.getNode("Node 1");
		Object data = node.getData();
		System.out.println(data);
    }
}

