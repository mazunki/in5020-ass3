package com.ass3.protocol;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.ass3.crypto.ConsistentHashing;
import com.ass3.p2p.NetworkInterface;
import com.ass3.p2p.NodeInterface;

/**
 * This class implements the chord protocol. The protocol is tested using the custom built simulator.
 */
public class ChordProtocol implements Protocol{

	// length of the identifier that is used for consistent hashing
	public int m;

	// network object
	public NetworkInterface network;

	// consisent hasing object
	public ConsistentHashing ch;

	// key indexes. tuples of (<key name>, <key index>)
	public HashMap<String, Integer> keyIndexes;

	public ChordProtocol(int m){
		this.m = m;
		setHashFunction();
		this.keyIndexes = new HashMap<>();
	}

	/**
	 * sets the hash function
	 */
	public void setHashFunction(){
		this.ch = new ConsistentHashing(this.m);
	}

	/**
	 * sets the network
	 * @param network the network object
	 */
        @Override
	public void setNetwork(NetworkInterface network){
		this.network = network;
	}

	/**
	 * sets the key indexes. Those key indexes can be used to test the lookup operation.
	 * @param keyIndexes - indexes of keys
	 */
    @Override
	public void setKeys(HashMap<String, Integer> keyIndexes){
		this.keyIndexes = keyIndexes;
	}

	/**
	 *
	 * @return the network object
	 */
    @Override
	public NetworkInterface getNetwork(){
		return this.network;
	}

	/**
	 * This method builds the overlay network.  It assumes the network object has already been set. It generates indexes
	 *     for all the nodes in the network. Based on the indexes it constructs the ring and places nodes on the ring.
	 *         algorithm:
	 *           1) for each node:
	 *           2)     find neighbor based on consistent hash (neighbor should be next to the current node in the ring)
	 *           3)     add neighbor to the peer (uses Peer.addNeighbor() method)
	 */
    @Override
	public void buildOverlayNetwork(){
		LinkedHashMap<String, NodeInterface> topology = this.network.getTopology();
		Map<Integer, NodeInterface> ringNodes = new TreeMap<>();  // treemap guarantees nodes are sorted order by their index
		
		for(NodeInterface node : topology.values()){
			int nodeIndex = this.ch.hash(node.getName());
			
			node.setId(nodeIndex);
			ringNodes.put(nodeIndex, node);	
		}


		/*
		 * Every successor tells its predecessor it's their successor in a ring fashion
		 */
		NodeInterface prev=null, first=null;
		for (NodeInterface node: ringNodes.values()){
			if (first == null) {
				first = node;
			}
			
			if (prev != null) {
				prev.addNeighbor(node.getName(), node);
			}

			prev = node;
		}

		// complete the ring
		if (prev != null && first != null) {
			prev.addNeighbor(first.getName(), first);
		}
	}


	/**
	 * This method builds the finger table. The finger table is the routing table used in the chord protocol to perform
	 * lookup operations. The finger table stores m-entries. Each ith entry points to the ith finger of the node.
	 * Each ith entry stores the information of it's neighbor that is responsible for indexes ((n+2^i-1) mod 2^m).
	 * i = 1,...,m.
	 *
	 *Each finger table entry should consists of
	 *     1) start value - (n+2^i-1) mod 2^m. i = 1,...,m
	 *     2) interval - [finger[i].start, finger[i+1].start)
	 *     3) node - first node in the ring that is responsible for indexes in the interval
	 */
	@Override
	public void buildFingerTable() {
		// Convert topology values to a list for easy access by index
		List<NodeInterface> nodes = new ArrayList<>(this.network.getTopology().values());

		int poolSize = (int) Math.pow(2, m);

		for (NodeInterface node : nodes) {
			List<FingerEntry> fingerTable = new ArrayList<>();
			Integer nodeIndex = node.getId();

			for (int i = 1; i <= m; i++) {
				int start = (nodeIndex + (int) Math.pow(2, i - 1)) % poolSize;
				int end = (nodeIndex + (int) Math.pow(2, i) - 1) % poolSize;

				if (i == m) {
					end = fingerTable.getFirst().start - 1;
				}

				NodeInterface successor = null;
				for (NodeInterface n : nodes) {
					if (n.getId() >= start) {
						successor = n;
						break;
					}
				}

				if (successor == null) {
					successor = nodes.get(0); // wrap around the circle
				}

				fingerTable.add(new FingerEntry(start, end, successor));
			}

			node.setRoutingTable(fingerTable);
		}
	}


	/**
	 * This method performs the lookup operation.
	 *  Given the key index, it starts with one of the node in the network and follows through the finger table.
	 *  The correct successors would be identified and the request would be checked in their finger tables successively.
	 *   Finally the request will reach the node that contains the data item.
	 *
	 * @param keyIndex index of the key
	 * @return names of nodes that have been searched and the final node that contains the key
	 */
	@Override
	public LookUpResponse lookUp(int keyIndex) {
		System.err.println("Looking up key index: " + keyIndex);
		LinkedHashSet<String> route = new LinkedHashSet<>();
		int hopCount = 0;

		NodeInterface curr = this.network.getTopology().firstEntry().getValue(); // starting node

		System.err.println("Starting from node: " + curr.getName());
		route.add(curr.getName());

		HashSet<String> visited = new HashSet<>();
		while (!visited.contains(curr.getName())) {
			visited.add(curr.getName());
			NodeInterface successor = curr.getSuccessor();

			if (FingerEntry.contains(keyIndex, curr.getId(), successor.getId())) {
					System.err.println("Found the responsible node: " + successor.getName());
					curr = successor;
					break; // we own it!
			}

			hopCount++;

			List<FingerEntry> fingerTable = new ArrayList<>();
			if (curr.getRoutingTable() instanceof List table) {
				for (Object entry : table) {
                    fingerTable.add((FingerEntry) entry);
				}
			} else {
				throw new RuntimeException("Routing table is not a list of FingerEntry");
			}

			boolean found = false;
			for (int i = fingerTable.size() - 1; i >= 0; i--) {
				FingerEntry candidate = fingerTable.get(i);
				if (candidate.contains(keyIndex)) {
					curr = candidate.successor;
					System.err.println("Found finger table for key index: " + keyIndex + " at node: " + curr.getName());
					found = true;
					break;
				}
			}

			if (!found) {
				curr = successor;
				System.err.println("Using successor node: " + curr.getName());
			}
			// if (curr == successor) {
			// 	throw new RuntimeException("Failed to find the responsible node for key index: " + keyIndex);
			// }
			route.add(curr.getName());

			System.err.println("Adding to route: " + curr.getName());
		}

		LookUpResponse response = new LookUpResponse(route, hopCount, curr.getName());
		System.err.println("Route: " + route);
		return response;
	}

	private static class FingerEntry {
		int start;
		int end;
		NodeInterface successor;

		public FingerEntry(int start, int end, NodeInterface successor) {
			this.start = start;
			this.end = end;
			this.successor = successor;
		}

        @Override
        public String toString() {
			return "FingerEntry{" +
					"start=" + start +
					",interval=[" + start + "–" + end + "]"+
					",successor=" + successor.getName() +
					'}';
		        }

		public boolean contains(int keyIndex) {
			return contains(keyIndex, this.start, this.end);
		}

		public static boolean contains(int keyIndex, int start, int end) {
			if (start == end) {
				return start == keyIndex;
			} else if (start > end) {
				return start <= keyIndex || keyIndex < end;
			} else {
				return start <= keyIndex && keyIndex < end;
			}
		}


	}



}
