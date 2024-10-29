package com.ass3.protocol;

import java.util.LinkedHashSet;
import java.util.StringJoiner;

/**
 * This class prints the response of the lookup. This is class prints the names
 * of the nodes whose finger table has been checked, the destination node
 * index, its name and hop count.
 */
public class LookUpResponse {
	public LinkedHashSet<String> peers_looked_up;
	public int node_index;
	public String node_name;

	public LookUpResponse(LinkedHashSet<String> peers_looked_up, int node_index, String node_name) {
		this.peers_looked_up = peers_looked_up;
		this.node_index = node_index;
		this.node_name = node_name;
	}

	public String toString() {
		StringJoiner output = new StringJoiner("; ");

		StringJoiner peers = new StringJoiner(", ");
		for (String peer : peers_looked_up) {
			peers.add("("+peer+")");
		}

		output.add("hops: " + peers_looked_up.size());
		output.add("node_index: " + node_index);
		output.add("node_name: " + node_name);
		output.add("peers: " + peers);
		return output.toString();
	}
}
