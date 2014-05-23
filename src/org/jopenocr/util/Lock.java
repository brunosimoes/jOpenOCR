package org.jopenocr.util;


public final class Lock {

	private final NewLock lock = new NewLock();

	public boolean equals(Object obj) {
		return lock.equals(obj);
	}

	public int hashCode() {
		return lock.hashCode();
	}

	public void readLock() {
		lock.readLock();
	}

	public void readUnlock() {
		lock.readUnlock();
	}

	public String toString() {
		return lock.toString();
	}

	public void writeLock() {
		lock.writeLock();
	}

	public void writeUnlock() {
		lock.writeUnlock();
	}

	public boolean canSwitch() {
		return lock.canSwitch();
	}

	public void switchToReadLock() {
		lock.switchToReadLock();
	}

}
