package edu.cg;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;


public class BasicSeamsCarver extends ImageProcessor {

	// An enum describing the carving scheme used by the seams carver.
	// VERTICAL_HORIZONTAL means vertical seams are removed first.
	// HORIZONTAL_VERTICAL means horizontal seams are removed first.
	// INTERMITTENT means seams are removed intermittently : vertical, horizontal, vertical, horizontal etc.
	public static enum CarvingScheme {
		VERTICAL_HORIZONTAL("Vertical seams first"),
		HORIZONTAL_VERTICAL("Horizontal seams first"),
		INTERMITTENT("Intermittent carving");

		public final String description;

		private CarvingScheme(String description) {
			this.description = description;
		}
	}

	// A simple coordinate class which assists the implementation.
	protected class Coordinate {
		public int X;
		public int Y;

		public Coordinate(int X, int Y) {
			this.X = X;
			this.Y = Y;
		}
	}

	// TODO :  Decide on the fields your BasicSeamsCarver should include. Refer to the recitation and homework 
	// instructions PDF to make an educated decision.
	BufferedImage grayScaledImage;
	BufferedImage gradientImage;
	int[][] costsTable;
	Boolean[] availableVerticalSeams;

	public BasicSeamsCarver(Logger logger, BufferedImage workingImage,
							int outWidth, int outHeight, RGBWeights rgbWeights) {
		super((s) -> logger.log("Seam carving: " + s), workingImage, rgbWeights, outWidth, outHeight);
		// TODO : Include some additional initialization procedures.

		grayScaledImage = greyscale();
		gradientImage = gradientMagnitude();
		costsTable = new int[gradientImage.getHeight()][gradientImage.getWidth()];
		availableVerticalSeams = initializeBooleanArray(gradientImage.getWidth());
		generateCostsTable(costsTable);
	}

	public BufferedImage carveImage(CarvingScheme carvingScheme) {
		int numberOfVerticalSeamsToCarve = Math.abs(this.outWidth - this.inWidth);
		int numberOfHorizontalSeamsToCarve = Math.abs(this.outHeight - this.inHeight);
		// TODO :  Perform Seam Carving. Overall you need to remove 'numberOfVerticalSeamsToCarve' vertical seams
		// and 'numberOfHorizontalSeamsToCarve' horizontal seams from the image.
		// Note you must consider the 'carvingScheme' parameter in your procedure.
		// Return the resulting image.
		throw new UnimplementedMethodException("showSeams");

	}


	// add function for removing minimal horizontal seam and vertical seam

	public BufferedImage showSeams(boolean showVerticalSeams, int seamColorRGB) {
		int numberOfVerticalSeamsToCarve = Math.abs(this.outWidth - this.inWidth);
		int numberOfHorizontalSeamsToCarve = Math.abs(this.outHeight - this.inHeight);
		// TODO :  Present either vertical or horizontal seams on the input image.
		// If showVerticalSeams = true, carve 'numberOfVerticalSeamsToCarve' vertical seams from the image.
		// Then, generate a new image from the input image in which you mark all of the vertical seams that
		// were chosen in the Seam Carving process.
		// This is done by painting each pixel in each seam with 'seamColorRGB' (overriding its' previous value).
		// Similarly, if showVerticalSeams = false, carve 'numberOfHorizontalSeamsToCarve' horizontal seams
		// from the image.
		// Then, generate a new image from the input image in which you mark all of the horizontal seams that
		// were chosen in the Seam Carving process.

		BufferedImage image = duplicateWorkingImage();
		List<Coordinate> path = getVerticalSeam();
		image = costTableToImage();
//		drawVerticalSeam(image, path);

		return image;
	}

	private void drawVerticalSeam(BufferedImage image, List<Coordinate> path){
		for(Coordinate c : path){
			image.setRGB(c.X,c.Y, Color.RED.getRGB());
		}

	}

	private BufferedImage costTableToImage(){
		BufferedImage image = newEmptyInputSizedImage();
		int maxValue = 0;
		for(int row = 0; row < costsTable.length; row++){
			for(int col = 0; col < costsTable[row].length; col++){
				if(costsTable[row][col] >= maxValue){
					maxValue = costsTable[row][col];
				}
			}
		}

		int x = maxValue / 255;

		for(int row = 0; row < costsTable.length; row++){
			for(int col = 0; col < costsTable[row].length; col++){
				Color c = new Color(costsTable[row][col] / x);
				image.setRGB(col, row, c.getRGB());
			}
		}

		return image;
	}

	private List<Coordinate> getVerticalSeam(){
		List<Coordinate> seamPath = new ArrayList<Coordinate>();
		int maxIndex = getMaxAvailableIndex();
		seamPath.add(new Coordinate(maxIndex, gradientImage.getHeight() - 1));

		for(int row = gradientImage.getHeight() - 1; row > 0 ; row--){
			if(costsTable[row - 1][maxIndex - 1] >= costsTable[row - 1][maxIndex] && costsTable[row - 1][maxIndex - 1] >= costsTable[row - 1][maxIndex + 1]){
				seamPath.add(new Coordinate(maxIndex - 1, row - 1));
			}
			else if(costsTable[row - 1][maxIndex] >= costsTable[row - 1][maxIndex - 1] && costsTable[row - 1][maxIndex] >= costsTable[row - 1][maxIndex + 1]){
				seamPath.add(new Coordinate(maxIndex, row - 1));
			}
			else if(costsTable[row - 1][maxIndex + 1] >= costsTable[row - 1][maxIndex - 1] && costsTable[row - 1][maxIndex + 1] >= costsTable[row - 1][maxIndex ]){
				seamPath.add(new Coordinate(maxIndex - 1, row + 1));
			}
		}

		return seamPath;


	}

	private int getMaxAvailableIndex(){
		int maxIndex = 0;
		int lastRow = gradientImage.getHeight() - 1;
		for(int i = 0; i < gradientImage.getWidth(); i++){
			if(costsTable[lastRow][i] > costsTable[lastRow][maxIndex] && availableVerticalSeams[i]){
				maxIndex = i;
			}
		}

		availableVerticalSeams[maxIndex] = false;
		return maxIndex;
	}

	private Boolean[]  initializeBooleanArray(int arrayLength){
		Boolean [] boolArray = new Boolean[arrayLength];
		for(int i=0;i<arrayLength;i++){
			boolArray[i] = true;
		}

		return boolArray;
	}

	// add function for initializing costs table
	private void generateCostsTable(int[][] costsTable) {
		// initializing first row
		Color c;
		for (int i = 0; i < costsTable[0].length; i++) {
			c = new Color(gradientImage.getRGB(i, 0));
			costsTable[0][i] = c.getRed();
		}
		for (int row = 1; row < costsTable.length; row++) {
			for (int col = 0; col < costsTable[row].length; col++) {
				if (col != 0 && col != costsTable[row].length - 1) {
					// taking the min cost from adjacent cells and adding the gradient for the current cell
					c = new Color(gradientImage.getRGB(col, row));
					costsTable[row][col] = c.getRed() + Math.max(Math.max(costsTable[row - 1][col], costsTable[row - 1][col - 1]), costsTable[row - 1][col + 1]);
				} else {
					// padding the sides with 0 so it would be too expensive for the seam to pass there
					if (col == 0) {
						costsTable[row][col] = 0;
					}
					if (col == costsTable[row].length - 1) {
						costsTable[row][col] = 0;
					}
				}
			}
		}

	}

}