package com.ass3.protocol;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

import com.ass3.crypto.ConsistentHashing;
import com.ass3.p2p.DataEntry;
import com.ass3.p2p.NetworkInterface;
import com.ass3.p2p.NodeInterface;
import com.ass3.protocol.FingerTable.Finger;

/**
 * This class implements the chord protocol. The protocol is tested using the
 * custom built simulator.
 */
public class ChordProtocol implements Protocol {

	// length of the identifier that is used for consistent hashing
	public int m;

	// network object
	public NetworkInterface network;

	// consisent hasing object
	public ConsistentHashing ch;

	// key indexes. tuples of (<key name>, <key index>)
	public HashMap<String, Integer> keyIndexes;

	public ChordProtocol(int m) {
		if (m > 30) {
			throw new IllegalArgumentException("int is max 2^32-1. m=30 max because signed");
		}
		this.m = m;
		setHashFunction();
		this.keyIndexes = new HashMap<>();
	}

	public void setHashFunction() {
		this.ch = new ConsistentHashing(this.m);
	}

    @Override
	public void setNetwork(NetworkInterface network) {
		this.network = network;
	}

    @Override
	public void setKeys(HashMap<String, Integer> keyIndexes) {
		this.keyIndexes = keyIndexes;
	}

    @Override
	public NetworkInterface getNetwork() {
		return this.network;
	}

     @Override
	public void buildOverlayNetwork() {
		// Get the topology of the network
		LinkedHashMap<String, NodeInterface> topology = this.network.getTopology();
		List<NodeInterface> ringNodes = new ArrayList<>();

		// Go through all the nodes in the network.
		// Ensure that the nodes have a id, add them to a ring which will be sorted
		for (NodeInterface node : topology.values()) {

			// The node index uses a consistent hashing (ch) function to generate the index, 
			//which is importenat to evenly spread the nodes in the ring
			int nodeIndex = this.ch.hash(node.getName()); 

			node.setId(nodeIndex); 
			ringNodes.add(node);
		}

		// We need to guarantee that the nodes are sorted in the order of their index
		ringNodes.sort(Comparator.comparingInt(NodeInterface::getId));

		// Tells every trailing node to grab the succeeding node's tail as a neighbor
		NodeInterface prev = null, first = null;
		for (NodeInterface node : ringNodes) {
			if (first == null) { 
				first = node;
			}

			if (prev == null) {
				prev = node;
				continue;
			}

			if (!prev.getNeighbors().contains(node)) {
				prev.addNeighbor(node.getName(), node);
			}
			prev = node;
		}

		if (prev == null || first == null) {
			return;  // meaning there was no nodes in the network
		}

		// complete the ring
		if (!prev.getNeighbors().contains(first)) {
			prev.addNeighbor(first.getName(), first);
		}
	}

    @Override
	public void buildFingerTable() {
		int ringSize = 1 << this.m;
		LinkedHashMap<String, NodeInterface> topology = this.network.getTopology();


		/*
		 * we build the finger table in a similar fashion as we grab busses
		 * in the real world. if we arrive at 11:47, the bus responsible for us
		 * will be the immediate next one.
		 * 
		 * start = index + 2^(i-1) mod 2^m
		 * end   = index + 2^i     mod 2^m
		 */
		for (NodeInterface node : topology.values()) {
			FingerTable fingerTable = new FingerTable(this.m);
			int index = node.getId();

			for (int i = 1; i <= m; i++) {
				int thisStart = (index + (1 << i - 1)) % ringSize;
				int nextStart = (index + (1 << i)) % ringSize;
				int thisEnd = (nextStart - 1 + ringSize) % ringSize;

				if (i == m) { // last finger of the entry
					thisEnd = (fingerTable.getEntries().getFirst().getStart() - 1) % ringSize;
				}
				
				if (thisEnd < 0) { // an edge case where the pool is too small and causes end to become -1 instead
					thisEnd = (1<<i) - 1;
				}

				NodeInterface successor = getImmediateNextNodeInNetwork(thisStart);
				fingerTable.addEntry(thisStart, thisEnd, successor);
			}

			node.setRoutingTable((Object) fingerTable);
		}
	}

	/**
	 * This method returns the immediate next node in the network. This is useful because
	 * we often want keys which are not in the network, and we don't know ahead of time where
	 * the succeeding node is.
	 * 
	 * @param index the index of the current node
	 * @return the immediate next node in the network
	 */
	private NodeInterface getImmediateNextNodeInNetwork(int index) {
		List<NodeInterface> sortedNodes = new ArrayList<>(this.network.getTopology().values());
		sortedNodes.sort(Comparator.comparingInt(NodeInterface::getId));

		// as soon as we find a node bigger or equal to our treasure, we return it
		for (NodeInterface candidate : sortedNodes) {
			if (candidate.getId() >= index) {
				return candidate;
			}
		}

		// otherwise we loop around
		return sortedNodes.getFirst();
	}

    @Override
	public LookUpResponse lookUp(int keyIndex) {
		LinkedHashSet<String> historyNodes = new LinkedHashSet<>();
		NodeInterface current = this.network.getTopology().values().iterator().next();

		while (!historyNodes.contains(current.getName())) {
			historyNodes.add(current.getName());

			NodeInterface successor = current.getSuccessor();

			FingerTable fingerTable = (FingerTable) current.getRoutingTable();

			// trivial case where we ARE the key
			if (current.getId() == keyIndex) {
				return new LookUpResponse(historyNodes, current.getId(), current.getName());
			}

			// our successor is the key
			boolean ourResponsibility = FingerTable.contains(keyIndex, current.getId(), successor.getId(), this.m);
			if (ourResponsibility) {
				return new LookUpResponse(historyNodes, successor.getId(), successor.getName());
			}

			/** 
			 * otherwise we want to jump the closest to the target, which is best done
			 * by walking backwards
			 */
			Finger closestPreceding = null;
			for (int i = this.m - 1; i >= 0; i--) {
				Finger finger = fingerTable.getFinger(i);
				if (FingerTable.contains(keyIndex, finger.getNode().getId(), current.getId(), this.m)) {
					closestPreceding = finger;
					break;
				}
			}

			if (closestPreceding != null) {
				current = closestPreceding.getNode();
			} else {
				current = successor;
			}
		}

		return new LookUpResponse(historyNodes, current.getId(), current.getName());
	}

	/**
	 * wrapper function for the lookup function, which returns the data
	 * associated with the key index, which is guaranteed to exist
	 * at the node which is returned by the lookup function
	 */
	public Object getDataByKey(int keyIndex) {
		LookUpResponse response = this.lookUp(keyIndex);
		NodeInterface node = this.network.getNode(response.node_name);
		if (node.getData() instanceof LinkedHashSet<?> dataItems) {
			for (Object entry : dataItems) {
				if (entry instanceof DataEntry data) {
					if (data.getKey()== keyIndex) {
						return data.getValue();
					}
				}
			}
		}

		return null;
	}
}
