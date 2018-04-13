package net.xdevelop.go.util;

import java.util.List;

/**
 * A light list, the capacity is fixed 361
 */
public final class LightList {
	private int[] values;
	private int count;
	
	public LightList() {
		values = new int[361];
		clear();
	}
	
	public LightList(int size) {
		values = new int[size];
		clear();
	}
	
	public void add(int v) {
		values[count] = v;
		count++;
	}
	
	public int get(int i) {
		return values[i];
	}
	
	public void addAll(LightList list) {
		int[] tmp = list.values();
		for (int v : tmp) {
			values[count] = v;
			count++;
		}
	}
	
	public void addAll(int[] arr) {
		for (int v : arr) {
			values[count] = v;
			count++;
		}
	}
	
	public void addAll(List<Integer> set) {
		for (int v : set) {
			values[count] = v;
			count++;
		}
	}
	
	public boolean contains(int v) {
		for (int i = 0; i < count; i++) {
			if (values[i] == v) {
				return true;
			}
		}
		return false;
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
	}
	
	public void clear() {
//		for (int i = 0; i < values.length; i++) {
//			values[i] = -1;
//		}
		count = 0;
	}
	
	public int size() {
		return count;
	}
	
	public int[] values() {
		int[] tmp = new int[count];
		for (int i = 0; i < count; i++) {
			tmp[i] = values[i];
		}
		return tmp;
	}
	
	public int[] descValues() {
		int[] tmp = new int[count];
		for (int i = 0; i < count; i++) {
			tmp[count - i - 1] = values[i];
		}
		return tmp;
	}
}
