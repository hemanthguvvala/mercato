package com.interview.catalogservice.entities;

import jakarta.persistence.Embeddable;

@Embeddable
public class Dimensions {

	private double lengthCm;
	private double widthCm;
	private double heightCm;

	protected Dimensions() {
	}

	public Dimensions(double lengthCm, double widthCm, double heightCm) {
		this.lengthCm = lengthCm;
		this.widthCm = widthCm;
		this.heightCm = heightCm;
	}

	public double getLengthCm() {
		return lengthCm;
	}

	public double getWidthCm() {
		return widthCm;
	}

	public double getHeightCm() {
		return heightCm;
	}
}
