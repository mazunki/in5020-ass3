package com.ass3.protocol;

import com.ass3.crypto.ConsistentHashing;
import com.ass3.p2p.NetworkInterface;

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
		/*
		 * TODO: implement this logic
		 */
	}

	public void buildFingerTable() {
		/*
		 * TODO: implement this logic
		 */
	}

	public LookUpResponse lookUp(int keyIndex) {
		/*
		 * TODO: implement this logic
		 */
		return null;
	}
}
