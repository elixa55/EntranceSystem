package processing;

import java.awt.Point;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ThinnedImage {
	
	final static int[][] nbrs = { { 0, -1 }, { 1, -1 }, { 1, 0 }, { 1, 1 }, { 0, 1 }, { -1, 1 }, { -1, 0 }, { -1, -1 },
			{ 0, -1 } };
	final static int[][][] nbrGroups = { { { 0, 2, 4 }, { 2, 4, 6 } }, { { 0, 2, 6 }, { 0, 4, 6 } } };
	public List<Point> toWhite = new ArrayList<>();
	public double [][] pixels;
	
	/**
	 * @param pixels
	 */
	public ThinnedImage(double [][] pixels) {
		this.pixels = pixels;
	}
	
	/**
	 * @throws IOException
	 */
	public void thinImage() throws IOException {
		boolean firstStep = false;
		boolean hasChanged;
		do {
			hasChanged = false;
			firstStep = !firstStep;
			for (int r = 1; r < this.pixels.length - 1; r++) {
				for (int c = 1; c < this.pixels[0].length - 1; c++) {
					if (this.pixels[r][c] != 0) {
						continue;
					}
					int nn = numNeighbors(r, c);
					if (nn < 2 || nn > 6)
						continue;
					if (numTransitions(r, c) != 1)
						continue;
					if (!atLeastOneIsWhite(r, c, firstStep ? 0 : 1))
						continue;
					toWhite.add(new Point(c, r));
					hasChanged = true;
				}
			}
			for (Point p : toWhite)
				this.pixels[p.y][p.x] = 255;
			toWhite.clear();
		} while (firstStep || hasChanged);
	}

	/**
	 * @param r
	 * @param c
	 * @return
	 */
	public int numNeighbors(int r, int c) {
		int count = 0;
		for (int i = 0; i < nbrs.length - 1; i++)
			if (this.pixels[r + nbrs[i][1]][c + nbrs[i][0]] == 0)
				count++;
		return count;
	}

	/**
	 * @param r
	 * @param c
	 * @return
	 */
	public int numTransitions(int r, int c) {
		int count = 0;
		for (int i = 0; i < nbrs.length - 1; i++)
			if (this.pixels[r + nbrs[i][1]][c + nbrs[i][0]] == 255) {
				if (this.pixels[r + nbrs[i + 1][1]][c + nbrs[i + 1][0]] == 0)
					count++;
			}
		return count;
	}

	/**
	 * @param r
	 * @param c
	 * @param step
	 * @return
	 */
	public boolean atLeastOneIsWhite(int r, int c, int step) {
		int count = 0;
		int[][] group = nbrGroups[step];
		for (int i = 0; i < 2; i++)
			for (int j = 0; j < group[i].length; j++) {
				int[] nbr = nbrs[group[i][j]];
				if (this.pixels[r + nbr[1]][c + nbr[0]] == 255) {
					count++;
					break;
				}
			}
		return count > 1;
	}

}