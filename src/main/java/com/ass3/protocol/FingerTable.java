package com.ass3.protocol;

import com.ass3.p2p.NodeInterface;
import java.util.ArrayList;
import java.util.List;

public class FingerTable {
	private final List<Finger> entries;
	private final int m;

	public FingerTable(int m) {
		entries = new ArrayList<>();
		this.m = m;
	}

	public void addEntry(int start, int end, NodeInterface successor) {
		this.entries.add(new Finger(start, end, successor));
	}

	// fingers are 1-indexed
	public Finger getFinger(int index) {
		return this.entries.get(index-1);
	}

	public Finger getResponsibleFinger(int keyIndex) {
		for (Finger finger : this.entries) {
			if (this.contains(keyIndex, finger)) {
				return finger;
			}
		}
		
		throw new NoSuchFieldError(this.toString() + Integer.toString(keyIndex));
	}

	public Finger getClosestPrecedingFinger(int keyIndex) {
		for (int i = entries.size() - 1; i >= 0; i--) {
			Finger finger = entries.get(i);
			NodeInterface fingerNode = finger.getNode();
			if (fingerNode.getId() != keyIndex && contains(fingerNode.getId(), finger.getStart(), finger.getIntervalEnd())) {
				if (fingerNode.getId() < keyIndex || (finger.getStart() < finger.getIntervalEnd())) {
					return finger;
				}
			}
		}
		return entries.get(0);  // fallback to the first entry if no preceding finger found
	}

	public int size() {
		return this.entries.size();
	}

	public List<Finger> getEntries() {
		return entries;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("FingerTable{\n");
		for (Finger entry : entries) {
			sb.append("  ").append(entry).append("\n");
		}
		sb.append("}");
		return sb.toString();
	}

	public static boolean contains(int index, int start, int end, int m) {
		int ringSize = 1 << m;
		if (start == end) {
			return start == index;
		} else if (start < end) {
			return start <= index && index <= end;
		} else {
			return (index >= start && index < ringSize) || (index >= 0 && index <= end);
		}
	}

	public boolean contains(int index, int start, int end) {
		return contains(index, start, end, this.m);
	}

	public boolean contains(int index, Finger entry) {
		return this.contains(index, entry.start, entry.intervalEnd);
	}


	public class Finger {
		private final int start;            // (node ID + 2^i) mod 2^m
		private final int intervalEnd;      // next finger's start - 1 (next finger uses same formula but ++i)
		private final NodeInterface successor;

		public Finger(int start, int intervalEnd, NodeInterface successor) {
			this.start = start;
			this.intervalEnd = intervalEnd;
			this.successor = successor;
		}

		public int getStart() {
			return start;
		}

		public int getIntervalEnd() {
			return intervalEnd;
		}

		public NodeInterface getNode() {
			return successor;
		}

		@Override
		public String toString() {
			return "Finger{" +
			"start=" + start +
			", interval=(" + start + "," + intervalEnd + ")" +
			", node=" + successor.getName() + "(id=" + successor.getId() + ")" +
			'}';
		}
	}


}

