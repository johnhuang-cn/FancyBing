package net.xdevelop.go.util;

import java.util.List;

/**
 * 轻量不重复Set
 */
public final class LightSet {
	private boolean[] slot;
	private int[] values;
	private int count;
	
	public LightSet() {
		slot = new boolean[361];
		values = new int[361];
		clear();
	}
	
	public void add(int v) {
		if (!slot[v]) {
			values[count] = v;
			slot[v] = true;
			count++;
		}
	}
	
	public void addAll(LightSet set) {
		int[] tmp = set.values();
		for (int v : tmp) {
			if (!slot[v]) {
				values[count] = v;
				slot[v] = true;
				count++;
			}
		}
	}
	
	public void addAll(int[] vs) {
		for (int v : vs) {
			if (!slot[v]) {
				values[count] = v;
				slot[v] = true;
				count++;
			}
		}
	}
	
	public void addAll(List<Integer> set) {
		for (int v : set) {
			if (!slot[v]) {
				values[count] = v;
				slot[v] = true;
				count++;
			}
		}
	}
	
	public boolean contains(int v) {
		return slot[v];
		/*
		for (int i = 0; i < count; i++) {
			if (values[i] == v) {
				return true;
			}
		}
		return false;*/
	}
	
	
	public void remove(int v) {
		for (int i = 0; i < count; i++) {
			if (values[i] == v) {
				if (i < count - 1) {
					values[i] = values[count-1];
					values[count-1] = -1;
				}
				else {
					values[i] = -1;
				}
				count--;
			}
		}
		slot[v] = false;
	}
	
	public void clear() {
		for (int i = 0; i < values.length; i++) {
//			values[i] = -1;
			slot[i] = false;
		}
		count = 0;
	}
	
	public int size() {
		return count;
	}
	
	public int get(int index) {
		return values[index];
	}
	
	public int[] values() {
		int[] tmp = new int[count];
		for (int i = 0; i < count; i++) {
			tmp[i] = values[i];
		}
		return tmp;
	}
}
