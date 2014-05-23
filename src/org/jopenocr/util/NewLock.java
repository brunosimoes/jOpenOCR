package org.jopenocr.util;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Delegates a {@link java.util.concurrent.locks.ReentrantReadWriteLock}.
 */
final class NewLock {

	final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(false);

	public void readLock() {
		lock.readLock().lock();
	}

	public void writeLock() {
		lock.writeLock().lock();
	}

	public void readUnlock() {
		lock.readLock().unlock();
	}

	public void writeUnlock() {
		lock.writeLock().unlock();
	}

	public boolean canSwitch() {
		return lock.getWriteHoldCount() == 1;
	}

	public void switchToReadLock() {
		if (!canSwitch())
			throw new Error();
		readLock();
		writeUnlock();
	}

}
