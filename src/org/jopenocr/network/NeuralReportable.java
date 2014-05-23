package org.jopenocr.network;

public interface NeuralReportable {

	public void update(int retry, double totalError, double bestError);

}

