package com.toohill.gopro.client;

public class GoPro {

	public int curMode;
	public int startupMode;
	public int spotMeter;
	public int curTimelapseInterval;
	public int autoPowerOff;
	public int curViewAngle;
	public int curPhotoMode;
	public int curVidMode;
	public int recordMins;
	public int recordSecs;
	public int curBeepVol;
	public int ledState;
	public int otherStats;
	public int battPercent;

	public GoPro(int curMode, int startupMode, int spotMeter, int curTimelapseInterval,
			int autoPowerOff, int curViewAngle, int curPhotoMode,
			int curVidMode, int recordMins, int recordSecs, int curBeepVol,
			int ledState, int otherStats, int battPercent) {
		this.curMode = curMode;
		this.startupMode = startupMode;
		this.spotMeter = spotMeter;
		this.curTimelapseInterval = curTimelapseInterval;
		this.autoPowerOff = autoPowerOff;
		this.curViewAngle = curViewAngle;
		this.curPhotoMode = curPhotoMode;
		this.curVidMode = curVidMode;
		this.recordMins = recordSecs;
		this.curBeepVol = curBeepVol;
		this.ledState = ledState;
		this.otherStats = otherStats;
		this.battPercent = battPercent;
	}
	
	public GoPro() {}

	public int getCurMode() {
		return curMode;
	}

	public void setCurMode(int curMode) {
		this.curMode = curMode;
	}

	public int getStartupMode() {
		return startupMode;
	}

	public void setStartupMode(int startupMode) {
		this.startupMode = startupMode;
	}

	public int getSpotMeter() {
		return spotMeter;
	}

	public void setSpotMeter(int spotMeter) {
		this.spotMeter = spotMeter;
	}

	public int getCurTimelapseInterval() {
		return curTimelapseInterval;
	}

	public void setCurTimelapseInterval(int curTimelapseInterval) {
		this.curTimelapseInterval = curTimelapseInterval;
	}

	public int getAutoPowerOff() {
		return autoPowerOff;
	}

	public void setAutoPowerOff(int autoPowerOff) {
		this.autoPowerOff = autoPowerOff;
	}

	public int getCurViewAngle() {
		return curViewAngle;
	}

	public void setCurViewAngle(int curViewAngle) {
		this.curViewAngle = curViewAngle;
	}

	public int getCurPhotoMode() {
		return curPhotoMode;
	}

	public void setCurPhotoMode(int curPhotoMode) {
		this.curPhotoMode = curPhotoMode;
	}

	public int getCurVidMode() {
		return curVidMode;
	}

	public void setCurVidMode(int curVidMode) {
		this.curVidMode = curVidMode;
	}

	public int getRecordMins() {
		return recordMins;
	}

	public void setRecordMins(int recordMins) {
		this.recordMins = recordMins;
	}

	public int getRecordSecs() {
		return recordSecs;
	}

	public void setRecordSecs(int recordSecs) {
		this.recordSecs = recordSecs;
	}

	public int getCurBeepVol() {
		return curBeepVol;
	}

	public void setCurBeepVol(int curBeepVol) {
		this.curBeepVol = curBeepVol;
	}

	public int getLedState() {
		return ledState;
	}

	public void setLedState(int ledState) {
		this.ledState = ledState;
	}

	public int getOtherStats() {
		return otherStats;
	}

	public void setOtherStats(int otherStats) {
		this.otherStats = otherStats;
	}

	public int getBattPercent() {
		return battPercent;
	}

	public void setBattPercent(int battPercent) {
		this.battPercent = battPercent;
	}

}