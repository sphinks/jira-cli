package jiracli.common;
/**
 * Author: Zerin_IS
 * File: Sequence.java
 * Date: 11.04.2011
 */

import java.util.*;


public class Sequence {

    private Object[] items;

    public Sequence(Object[] items) {
        this(items, 0, items.length);
    }

    public Sequence(Object[] items, int index, int length) {
        this.items = new Object[length];
        System.arraycopy(items, index, this.items, 0, length);
    }
    
    public boolean equals(Object o) {
        return o instanceof Sequence ? equals((Sequence) o) : false;
    }
    
    public boolean equals(Sequence s) {
        return Arrays.equals(items, s.items);
    }
    
    public int hashCode() {
    	int r = 0;
    	for (int i = 0; i < items.length; ++i) {
    		r ^= items[i].hashCode();
    	}
        return r;
    }
    
    public int length() {
        return items.length;
    }
    
    public Object at(int index) {
        return items[index];
    }
    
    public int indexOf(Object o) {
        return indexOf(o, 0);
    }
    
    public int indexOf(Object o, int index) {
        for (int i = index; i < items.length; ++i) {
            if (items[i].equals(o)) {
                return i;
            }
        }
        return -1;
    }
    
    public int lastIndexOf(Object o) {
        return lastIndexOf(o, items.length);
    }
    
    public int lastIndexOf(Object o, int index) {
        for (int i = index; i >= 0; --i) {
            if (items[i].equals(o)) {
                return i;
            }
        }
        return -1;
    }
    
    public Sequence subSequence(int index, int length) {
        return new Sequence(items, index, length);
    }
    
    public Sequence prefix(int length) {
        return subSequence(0, length);
    }
    
    public Sequence suffix(int length) {
        return subSequence(items.length - length, length);
    }
    
    public Sequence removePrefix(int length) {
        return suffix(items.length - length);
    }
    
    public Sequence removeSuffix(int length) {
        return prefix(items.length - length);
    }
    
    public String toString() {
        if (items.length > 0) {
            StringBuffer b = new StringBuffer();
            b.append(items[0]);
            for (int i = 1; i < items.length; ++i) {
                b.append(" ").append(items[i]);
            }
            return b.toString();
        } else {
            return "e";
        }
    }
}
