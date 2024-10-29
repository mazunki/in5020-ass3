package com.ass3.protocol;

import com.ass3.crypto.ConsistentHashing;
import com.ass3.p2p.DataEntry;
import com.ass3.p2p.NetworkInterface;
import com.ass3.p2p.NodeInterface;
import com.ass3.protocol.FingerTable.Finger;

import java.util.*;

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
		this.keyIndexes = new HashMap<String, Integer>();
	}

	public void setHashFunction() {
		this.ch = new ConsistentHashing(this.m);
	}

	public void setNetwork(NetworkInterface network) {
		this.network = network;
	}

	public void setKeys(HashMap<String, Integer> keyIndexes) {
		this.keyIndexes = keyIndexes;
	}

	public NetworkInterface getNetwork() {
		return this.network;
	}

	public void buildOverlayNetwork() {
		LinkedHashMap<String, NodeInterface> topology = this.network.getTopology();
		List<NodeInterface> ringNodes = new ArrayList<>();

		for (NodeInterface node : topology.values()) {
			int nodeIndex = this.ch.hash(node.getName());

			node.setId(nodeIndex);
			ringNodes.add(node);
		}

		ringNodes.sort(Comparator.comparingInt(NodeInterface::getId));

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

	public void buildFingerTable() {
		int ringSize = 1 << this.m;
		LinkedHashMap<String, NodeInterface> topology = this.network.getTopology();

		for (NodeInterface node : topology.values()) {
			FingerTable fingerTable = new FingerTable(this.m);
			int index = node.getId();

			for (int i = 1; i <= m; i++) {
				int thisStart = (index + (1 << i - 1)) % ringSize;
				int nextStart = (index + (1 << i)) % ringSize;
				int thisEnd = (nextStart - 1 + ringSize) % ringSize;
				if (i == m) {
					thisEnd = (fingerTable.getEntries().getFirst().getStart() - 1) % ringSize;
				}
				
				if (thisEnd < 0) {
					thisEnd = (1<<i) - 1;
				}

				NodeInterface successor = getImmediateNextNodeInNetwork(thisStart);
				fingerTable.addEntry(thisStart, thisEnd, successor);
			}

			node.setRoutingTable((Object) fingerTable);
		}
	}

	private final NodeInterface getImmediateNextNodeInNetwork(int index) {
		List<NodeInterface> sortedNodes = new ArrayList<>(this.network.getTopology().values());
		sortedNodes.sort(Comparator.comparingInt(NodeInterface::getId));

		for (NodeInterface candidate : sortedNodes) {
			if (candidate.getId() >= index) {
				return candidate;
			}
		}

		return sortedNodes.getFirst();
	}

	public LookUpResponse lookUp(int keyIndex) {
		LinkedHashSet<String> historyNodes = new LinkedHashSet<>();
		NodeInterface current = this.network.getTopology().values().iterator().next();

		while (!historyNodes.contains(current.getName())) {
			historyNodes.add(current.getName());

			NodeInterface successor = current.getSuccessor();

			FingerTable fingerTable = (FingerTable) current.getRoutingTable();

			if (current.getId() == keyIndex) {
				return new LookUpResponse(historyNodes, current.getId(), current.getName());
			}

			boolean ourResponsibility = FingerTable.contains(keyIndex, current.getId(), successor.getId(), this.m);
			if (ourResponsibility) {
				return new LookUpResponse(historyNodes, successor.getId(), successor.getName());
			}

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
		//throw new RuntimeException("no such key: " + keyIndex + "history: " + historyNodes + ". current: " + current.getName());
	}

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
