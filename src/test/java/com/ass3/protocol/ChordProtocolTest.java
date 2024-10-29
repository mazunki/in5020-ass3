package com.ass3.protocol;

import java.util.List;
import java.util.StringJoiner;

import com.ass3.*;
import com.ass3.p2p.*;
import com.ass3.protocol.FingerTable;
import com.ass3.protocol.FingerTable.Finger;
import com.ass3.crypto.*;

public class ChordProtocolTest {
	private static final int m = 10;

	public static void main(String[] args) {
		try {
			testAllOverlayNetwork();
			System.out.println("✅ Overlay network passed!");
		} catch (Exception e) {
			System.out.println("💣 Overlay network exception: ");
			e.printStackTrace();
			System.exit(1);
		} catch (AssertionError e) {
			System.out.println("❌ Overlay network failed assertion: " + e.getMessage());
			System.exit(2);
		}

		try {
			testAllFingerTable();
			System.out.println("✅ Finger table passed!");
		} catch (Exception e) {
			System.out.println("💣 Finger table exception");
			e.printStackTrace();
			System.exit(1);
		} catch (AssertionError e) {
			System.out.println("❌ Finger table failed assertion: " + e.getMessage());
			System.exit(2);
		}

		try {
			testAllLookup();
			System.out.println("✅ Lookup passed!");
		} catch (Exception e) {
			System.out.println("💣 Lookup exception");
			e.printStackTrace();
			System.exit(1);
		} catch (AssertionError e) {
			System.out.println("❌ Lookup failed assertion: " + e.getMessage());
			System.exit(2);
		}
	}

	public static void testAllOverlayNetwork() {
		testEmptyNetwork();
		testSingleNodeNetwork();
		testBuildOverlayNetwork();
		testPrintAllOverlay();
		testIndexesWithinRange();
		testIndexesAreAscending();
	}

	public static void testAllFingerTable() {
		testModuloContains();
		testBuildFingerTable();
		testFingerTableSize();
		testFingerTableEntries();
	}

	public static void testAllLookup() {
		testExactMatch();
		testSameNodeLookup();
		testSingleNodeLookup();
		testLookupWithStoredData();
		testSucceedingNodeLookup();
		testWrapAroundLookup();
		testFingerTableRouting();
		testRandomNodeLookup();
	}

	/*
	 * a bunch of tests for the finger overlay network
	 */

	public static void testEmptyNetwork() {
		System.out.println("Running testEmptyNetwork...");

		int nodeCount = 0;
		Network network = Network.createNetwork("test", nodeCount);
		ChordProtocol chordProtocol = new ChordProtocol(ChordProtocolTest.m);
		chordProtocol.setNetwork(network);

		chordProtocol.buildOverlayNetwork();

		Assert.assertTrue(network.getTopology().isEmpty(), "Network should be empty");
		System.out.println("✓ testEmptyNetwork passed.");
	}

	public static void testSingleNodeNetwork() {
		System.out.println("Running testSingleNodeNetwork...");

		int nodeCount = 1;
		Network network = Network.createNetwork("test", nodeCount);
		ChordProtocol chordProtocol = new ChordProtocol(ChordProtocolTest.m);
		chordProtocol.setNetwork(network);

		chordProtocol.buildOverlayNetwork();

		NodeInterface singleNode = network.getNode("Node 1");
		Assert.assertTrue(singleNode.getSuccessor() == singleNode, "Single node should be its own successor");
		System.out.println("✓ testSingleNodeNetwork passed.");
	}

	public static void testBuildOverlayNetwork() {
		System.out.println("Running testBuildOverlayNetwork...");

		int nodeCount = 4;
		Network network = Network.createNetwork("test", nodeCount);
		ChordProtocol chordProtocol = new ChordProtocol(10); // too lazy to math out expectedSuccessorId from the m
																// value

		chordProtocol.setNetwork(network);
		chordProtocol.buildOverlayNetwork();

		NodeInterface currNode = network.getNode("Node 1");
		NodeInterface successorNode = currNode.getSuccessor();
		String node = currNode.getName();
		int nodeId = currNode.getId();
		String successor = successorNode.getName();
		int successorId = successorNode.getId();

		int expectedSuccessorId = 4; // contingent on m=10
		String expectedSuccessorName = "Node " + expectedSuccessorId;
		System.out.println(node + "(id=" + nodeId + ") successor: " + successor + "(id=" + successorId + ")");
		Assert.assertEquals(expectedSuccessorName, successor,
				"Successor of " + node + " should be " + expectedSuccessorName);

		System.out.println("✓ testBuildOverlayNetwork passed.");
	}

	public static void testPrintAllOverlay() {
		Network network = Network.createNetwork("test", 10);
		ChordProtocol chordProtocol = new ChordProtocol(ChordProtocolTest.m);

		chordProtocol.setNetwork(network);
		chordProtocol.buildOverlayNetwork();

		NodeInterface startNode = network.getNode("Node 1");
		NodeInterface successor = startNode.getSuccessor();

		System.out.print(startNode.getName() + "(id=" + startNode.getId() + ")");
		while (startNode != successor) {
			System.out.print(", " + successor.getName() + "(id=" + successor.getId() + ")");
			successor = successor.getSuccessor();
		}

		System.out.println();
	}

	public static void testIndexesWithinRange() {
		System.out.println("Running testIndexesWithinRange...");

		int nodeCount = 10;
		Network network = Network.createNetwork("test", nodeCount);
		ChordProtocol chordProtocol = new ChordProtocol(m);

		chordProtocol.setNetwork(network);
		chordProtocol.buildOverlayNetwork();

		int maxIndex = (1 << ChordProtocolTest.m) - 1; // 2^m - 1, maximum index allowed for m=10

		for (NodeInterface node : network.getTopology().values()) {
			int id = node.getId();
			System.out.println(node.getName() + " has id: " + id);
			Assert.assertTrue(id >= 0 && id <= maxIndex,
					"Node " + node.getName() + " should have an ID within [0, " + maxIndex + "]");
		}

		System.out.println("✓ testIndexesWithinRange passed.");
	}

	public static void testIndexesAreAscending() {
		System.out.println("Running testIndexesAreAscending...");

		int nodeCount = 10;
		Network network = Network.createNetwork("test", nodeCount);
		ChordProtocol chordProtocol = new ChordProtocol(ChordProtocolTest.m);

		chordProtocol.setNetwork(network);
		chordProtocol.buildOverlayNetwork();

		NodeInterface startNode = network.getNode("Node 1");
		NodeInterface currentNode = startNode;
		NodeInterface successorNode = currentNode.getSuccessor();

		boolean wrapped = false;

		do {
			int currentId = currentNode.getId();
			int successorId = successorNode.getId();

			System.out.println("Checking order: " + currentNode.getName() + "(id=" + currentId + ") -> "
					+ successorNode.getName() + "(id=" + successorId + ")");

			if (currentId > successorId) {
				Assert.assertFalse(wrapped, "Multiple wrap-arounds detected in Chord ring");
				wrapped = true;
			} else {
				Assert.assertTrue(currentId < successorId,
						"IDs should be in ascending order, found: " + currentId + " >= " + successorId);
			}

			currentNode = successorNode;
			successorNode = successorNode.getSuccessor();

		} while (currentNode != startNode);

		System.out.println("✓ testIndexesAreAscending passed.");
	}

	/*
	 * a bunch of tests for finger tables
	 */
	public static void testModuloContains() {
		System.out.println("Running testModuloContains...");
		int m = 10;
		FingerTable fingerTable = new FingerTable(m);
		Finger finger = fingerTable.new Finger(500, 200, null);

		Assert.assertTrue(FingerTable.contains(900, 500, 200, m), "900 should be between 500 and 200 (mod 1024)");
		Assert.assertTrue(fingerTable.contains(900, finger), "900 should be between 500 and 200 (mod 1024)");

		Assert.assertFalse(FingerTable.contains(400, 500, 200, m), "400 should not be between 500 and 200 (mod 1024)");
		Assert.assertFalse(fingerTable.contains(400, finger), "400 should not be between 500 and 200 (mod 1024)");

		Assert.assertTrue(FingerTable.contains(0, 0, 0, m), "0 should be between 0 and 0 (mod 1024)");
		Assert.assertTrue(FingerTable.contains(0, 0, 1023, m), "0 should be between 0 and 1023 (mod 1024)");
		Assert.assertTrue(FingerTable.contains(0, 1023, 0, m), "0 should be between 1023 and 0 (mod 1024)");

		Assert.assertFalse(FingerTable.contains(1024, 0, 1023, m), "1024 should not be between 0 and 1023 (mod 1024)");
		Assert.assertFalse(FingerTable.contains(1024, 1023, 0, m), "1024 should not be between 1023 and 0 (mod 1024)");

		System.out.println("✓ testModuloContains passed.");
	}

	public static void testBuildFingerTable() {
		System.out.println("Running testBuildFingerTable...");

		int m = ChordProtocolTest.m;
		Network network = Network.createNetwork("test", 10);
		ChordProtocol chordProtocol = new ChordProtocol(m);

		chordProtocol.setNetwork(network);
		chordProtocol.buildOverlayNetwork();
		chordProtocol.buildFingerTable();

		NodeInterface node1 = network.getNode("Node 1");
		System.out.println("Node 1 Finger Table: " + node1.getRoutingTable());
		Assert.assertTrue(node1.getRoutingTable() != null, "Finger table should be initialized");

		System.out.println("✓ testBuildFingerTable passed.");
	}

	public static void testFingerTableSize() {
		System.out.println("Running testFingerTableSize...");

		int nodeCount = 10;
		Network network = Network.createNetwork("test", nodeCount);
		ChordProtocol chordProtocol = new ChordProtocol(m);

		chordProtocol.setNetwork(network);
		chordProtocol.buildOverlayNetwork();
		chordProtocol.buildFingerTable();

		for (NodeInterface node : network.getTopology().values()) {
			int expectedSize = m;
			FingerTable fingerTable = (FingerTable) node.getRoutingTable();
			int actualSize = fingerTable.size();

			System.out.println(node.getName() + " has finger table size: " + actualSize);
			Assert.assertEquals(expectedSize, actualSize,
					"Node " + node.getName() + " should have a finger table size of " + expectedSize);
		}

		System.out.println("✓ testFingerTableSize passed.");
	}

	public static void testFingerTableEntries() {
		System.out.println("Running testFingerTableEntries...");

		int nodeCount = 10;
		Network network = Network.createNetwork("test", nodeCount);
		ChordProtocol chordProtocol = new ChordProtocol(m);

		chordProtocol.setNetwork(network);
		chordProtocol.buildOverlayNetwork();
		chordProtocol.buildFingerTable();

		int ringSize = 1 << m; // 2^m, the capacity size of node indexes

		for (NodeInterface node : network.getTopology().values()) {
			int nodeId = node.getId();
			System.out.print("Checking finger table for " + node.getName() + "(id=" + nodeId
					+ ")... asserting indexes bigger than ");
			FingerTable fingerTable = (FingerTable) node.getRoutingTable();

			StringJoiner sj = new StringJoiner(", ");
			for (int i = 1; i <= m; i++) {
				int expectedId = (nodeId + (1 << i - 1)) % ringSize; // (node id + 2^(i-1)) mod 2^m
				Finger finger = fingerTable.getFinger(i-1);
				// NodeInterface fingerNode = finger.getNode();

				sj.add(Integer.toString(expectedId));

				Assert.assertTrue(fingerTable.contains(expectedId, finger),
						"Finger " + i + " of node " + node.getName() + " should point to the closest successor of "
								+ expectedId);
			}
			System.out.println(sj);
		}

		System.out.println("✓ testFingerTableEntries passed.");
	}

	public static void testFingerTableCoverage() {
        Network network = Network.createNetwork("test", 1); // Single node to test finger table
        NodeInterface node = network.getTopology().values().iterator().next();
        FingerTable fingerTable = (FingerTable) node.getRoutingTable();

        int keyspaceSize = 1 << ChordProtocolTest.m;
        List<Finger> entries = fingerTable.getEntries();

        for (int key = 0; key < keyspaceSize; key++) {
            boolean found = false;
            for (Finger entry : entries) {
                if (fingerTable.contains(key, entry.getStart(), entry.getIntervalEnd())) {
                    found = true;
                    break;
                }
            }

            Assert.assertTrue(found, "Key " + key + " is not covered by any finger table interval.");
        }
    }

	/*
	 * a bunch of tests for lookups
	 */

	public static void testExactMatch() {
        System.out.println("Running testExactMatch...");

        Network network = Network.createNetwork("test", 4);
        ChordProtocol chordProtocol = new ChordProtocol(ChordProtocolTest.m);
        chordProtocol.setNetwork(network);
        chordProtocol.buildOverlayNetwork();
        chordProtocol.buildFingerTable();

        NodeInterface expectedNode = network.getNode("Node 2"); // we know node 2 has ID 266
        int targetKey = expectedNode.getId(); // Exact match with Node 2
		expectedNode.addData(new DataEntry(targetKey, "potato"));

        LookUpResponse response = chordProtocol.lookUp(targetKey);

        Assert.assertEquals(expectedNode.getId(), response.node_index, "Exact match failed for key " + targetKey);
        Assert.assertEquals(expectedNode.getName(), response.node_name, "Node name mismatch for exact match lookup");
    }

    public static void testFingerTableRouting() {
        System.out.println("Running testFingerTableRouting...");

        Network network = Network.createNetwork("test", 10);
        ChordProtocol chordProtocol = new ChordProtocol(ChordProtocolTest.m);
        chordProtocol.setNetwork(network);
        chordProtocol.buildOverlayNetwork();
        chordProtocol.buildFingerTable();

        int targetKey = 450; // Key not matching any node exactly
        NodeInterface expectedNode = network.getNode("Node 7"); // node7 has id 480
        LookUpResponse response = chordProtocol.lookUp(targetKey);

        Assert.assertEquals(expectedNode.getId(), response.node_index, "Finger table routing failed for key " + targetKey);
        Assert.assertEquals(expectedNode.getName(), response.node_name, "Node name mismatch for finger table routing");
    }

    public static void testWrapAroundLookup() {
        System.out.println("Running testWrapAroundLookup...");

        Network network = Network.createNetwork("test", 4);
        ChordProtocol chordProtocol = new ChordProtocol(ChordProtocolTest.m);
        chordProtocol.setNetwork(network);
        chordProtocol.buildOverlayNetwork();
        chordProtocol.buildFingerTable();

        int targetKey = 50; // key requiring wrap-around behavior
        NodeInterface expectedNode = network.getNode("Node 4"); // id=105 (lowest after wraparound)
        LookUpResponse response = chordProtocol.lookUp(targetKey);

        Assert.assertEquals(expectedNode.getId(), response.node_index, "Wrap-around lookup failed for key " + targetKey);
        Assert.assertEquals(expectedNode.getName(), response.node_name, "Node name mismatch for wrap-around lookup");
    }

    public static void testSingleNodeLookup() {
        System.out.println("Running testSingleNodeLookup...");

        Network network = Network.createNetwork("test", 1); // only one node in the network
        ChordProtocol chordProtocol = new ChordProtocol(ChordProtocolTest.m);
        chordProtocol.setNetwork(network);
        chordProtocol.buildOverlayNetwork();
        chordProtocol.buildFingerTable();

        NodeInterface singleNode = network.getTopology().values().iterator().next(); // the only node in the network
        int targetKey = 50;

		singleNode.addData("hey");

        LookUpResponse response = chordProtocol.lookUp(targetKey);

        Assert.assertEquals(singleNode.getId(), response.node_index, "Single-node lookup failed for key " + targetKey);
        Assert.assertEquals(singleNode.getName(), response.node_name, "Node name mismatch for single-node lookup");
    }

    public static void testSameNodeLookup() {
        System.out.println("Running testSameNodeLookup...");

        Network network = Network.createNetwork("test", 4);
        ChordProtocol chordProtocol = new ChordProtocol(ChordProtocolTest.m);
        chordProtocol.setNetwork(network);
        chordProtocol.buildOverlayNetwork();
        chordProtocol.buildFingerTable();

        NodeInterface node = network.getNode("Node 1"); // node 1 has index 333
        int targetKey = node.getId(); // looking for the key index at the same node index

        LookUpResponse response = chordProtocol.lookUp(targetKey);

        Assert.assertEquals(node.getId(), response.node_index, "Same node lookup failed for key " + targetKey);
        Assert.assertEquals(node.getName(), response.node_name, "Node name mismatch for same node lookup");
    }

    public static void testSucceedingNodeLookup() {
        System.out.println("Running testSucceedingNodeLookup...");

        Network network = Network.createNetwork("test", 10);
        ChordProtocol chordProtocol = new ChordProtocol(ChordProtocolTest.m);
        chordProtocol.setNetwork(network);
        chordProtocol.buildOverlayNetwork();
        chordProtocol.buildFingerTable();

        NodeInterface currentNode = network.getNode("Node 1"); // node 1 has index 333
        NodeInterface succeedingNode = network.getNode("Node 7"); // node 7 has index 480
        int targetKey = 350; // key between Node 1 (333) and Node 7 (480)
		
		currentNode.getSuccessor().addData("hehe");

		// NOTE: why does node 1 think it's responsible for 350?
		System.out.println(currentNode.getName() + currentNode.getRoutingTable());
		System.out.println(succeedingNode.getName() + succeedingNode.getRoutingTable());

        LookUpResponse response = chordProtocol.lookUp(targetKey);

        Assert.assertEquals(succeedingNode.getId(), response.node_index, "Succeeding node lookup failed for key " + targetKey);
        Assert.assertEquals(succeedingNode.getName(), response.node_name, "Node name mismatch for succeeding node lookup");
    }

    public static void testRandomNodeLookup() {
        System.out.println("Running testRandomNodeLookup...");

        Network network = Network.createNetwork("test", 10);
        ChordProtocol chordProtocol = new ChordProtocol(ChordProtocolTest.m);
        chordProtocol.setNetwork(network);
        chordProtocol.buildOverlayNetwork();
        chordProtocol.buildFingerTable();

        int targetKey = 650;
        NodeInterface expectedNode = network.getNode("Node 6"); // node 8: id 601... node 6: id 666
        LookUpResponse response = chordProtocol.lookUp(targetKey);

        Assert.assertEquals(expectedNode.getId(), response.node_index, "Random node lookup failed for key " + targetKey);
        Assert.assertEquals(expectedNode.getName(), response.node_name, "Node name mismatch for random node lookup");
    }

    public static void testLookupWithStoredData() {
        System.out.println("Running testLookupWithStoredData...");

        Network network = Network.createNetwork("test", 4);
		int m = 10;
        ChordProtocol chordProtocol = new ChordProtocol(m);
        chordProtocol.setNetwork(network);
        chordProtocol.buildOverlayNetwork();
        chordProtocol.buildFingerTable();

        NodeInterface node1 = network.getNode("Node 1"); // empirically we know node 1 has id 333
        node1.addData("three fifty");

        NodeInterface node2 = network.getNode("Node 2"); // we also know node 2 has id 266
        node2.addData("Two fifty");

        LookUpResponse response = chordProtocol.lookUp(300);
        Assert.assertEquals(node1.getId(), response.node_index, "data lookup failed for key 350");

        response = chordProtocol.lookUp(250);
        Assert.assertEquals(node2.getId(), response.node_index, "data lookup failed for key 269");
    }

}
