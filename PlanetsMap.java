package com.universe.defender;

/**
 * Hastable to quickly say if a pixel is occupied or not by a Planet. 
 */
public final class PlanetsMap {

	public Planet[] planets;
	private int width;
	private int height;
	private byte[] pixelToPlanet;

	public PlanetsMap(GalaxyThread progressReporter, Planet[] planets,
			int sWidth, int sHeight, int forcedRadius) {
		this.planets = planets;
		this.width = sWidth;
		this.height = sHeight;
		this.pixelToPlanet = new byte[this.width * this.height];
		int planetsCount = planets.length;
		for (int i = 0; i < planetsCount; i++) {
			this.savePlanet(planets[i], forcedRadius, (byte) (i + 1));
			progressReporter.sendMessageLoad("" + (i + 1) + " / "
					+ planetsCount);
		}
	}

	private void savePlanet(Planet p, int forcedRadius, byte id) {
		byte[] arrayToSet = this.pixelToPlanet;
		int px = p.X;
		int py = p.Y;
		int radius = p.Radius;
		if (forcedRadius > 0) {
			radius = forcedRadius;
		}
		int radiusSquare = radius * radius;
		int width = this.width;
		int height = this.height;
		for (int i = -radius; i <= radius; i++) {
			for (int j = -radius; j <= radius; j++) {
				int ditanceSquareCurrentPlanet = i * i + j * j;
				if (ditanceSquareCurrentPlanet <= radiusSquare) {
					int x = px + i;
					int y = py + j;
					if (x >= 0 && x < width) {
						if (y >= 0 && y < height) {
							int index = width * y + x;
							byte oldValue = arrayToSet[index];
							if (oldValue == 0) {
								arrayToSet[index] = id;
							} else {
								Planet otherPlanet = this.planets[arrayToSet[index] - 1];
								int dXOtherPlanet = x - otherPlanet.X;
								int dYOtherPlanet = y - otherPlanet.Y;
								int distanceSquareOtherPlanet = dXOtherPlanet
										* dXOtherPlanet + dYOtherPlanet
										* dYOtherPlanet;
								if (ditanceSquareCurrentPlanet < distanceSquareOtherPlanet) {
									arrayToSet[index] = id;
								} // /else the otherPlanet is nearest.
							}
						}
					}
				}
			}
		}
	}

	public Planet GetPlanet(int x, int y) {
		if (x >= 0 && x < width) {
			if (y >= 0 && y < height) {
				byte id = this.pixelToPlanet[x + y * this.width];
				if (id == 0) {
					return null;
				}
				return this.planets[id - 1];
			}
		}
		return null;
	}
}
