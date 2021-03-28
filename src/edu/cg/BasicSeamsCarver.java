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
	Coordinate[][] originalImageCoordinates;
	ArrayList< ArrayList<Coordinate> > seamsToDraw;

	public BasicSeamsCarver(Logger logger, BufferedImage workingImage,
							int outWidth, int outHeight, RGBWeights rgbWeights) {
		super((s) -> logger.log("Seam carving: " + s), workingImage, rgbWeights, outWidth, outHeight);
		// TODO : Include some additional initialization procedures.

		grayScaledImage = greyscale();
		gradientImage = gradientMagnitude();
		costsTable = new int[gradientImage.getHeight()][gradientImage.getWidth()];
		availableVerticalSeams = initializeBooleanArray(gradientImage.getWidth());
		generateCostsTable(costsTable);
		originalImageCoordinates = initCoordinatesTable();
		seamsToDraw = new ArrayList<ArrayList<Coordinate>>();
	}

	public BufferedImage carveImage(CarvingScheme carvingScheme) {
		int numberOfVerticalSeamsToCarve = Math.abs(this.outWidth - this.inWidth);
		int numberOfHorizontalSeamsToCarve = Math.abs(this.outHeight - this.inHeight);
		// TODO :  Perform Seam Carving. Overall you need to remove 'numberOfVerticalSeamsToCarve' vertical seams
		// and 'numberOfHorizontalSeamsToCarve' horizontal seams from the image.
		// Note you must consider the 'carvingScheme' parameter in your procedure.
		// Return the resulting image.

		BufferedImage image = duplicateWorkingImage();

		// repeat k times
		// recalculate
		List<Coordinate> path = getVerticalSeam(image);
//		drawVerticalSeam(image, path);
		// remove seam

		logger.log("image width before seam removal " + image.getWidth());
		image = removeVerticalSeam(image, path);
		removeSeamCoordinate(path);
		logger.log("image width after seam removal " + image.getWidth());
		return image;

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
		List<Coordinate> path = getVerticalSeam(image);
		image = removeVerticalSeam(image, path);
		removeSeamCoordinate(path);

		path = getVerticalSeam(image);
		image = removeVerticalSeam(image, path);
		removeSeamCoordinate(path);


		return drawSeams();
//		return image;
	}

	private BufferedImage removeVerticalSeam(BufferedImage image, List<Coordinate> path){
		BufferedImage result = newEmptyImage(image.getWidth() - 1, image.getHeight());

		for( Coordinate c : path){
			for(int col = 0; col < image.getWidth() - 1; col++){
				if(col < c.X ){
					result.setRGB(col, c.Y, image.getRGB(col, c.Y));
				} else {
					result.setRGB(col , c.Y , image.getRGB(col + 1, c.Y));
				}
			}

		}
		return result;
	}

	private Coordinate[][] initCoordinatesTable(){
		originalImageCoordinates = new Coordinate[inHeight][inWidth];

		setForEachInputParameters();

		forEach((y, x) -> {
			originalImageCoordinates[y][x] = new Coordinate(x,y);
		});

		return originalImageCoordinates;
	}

	private void removeSeamCoordinate(List<Coordinate> seamPath){
		for(Coordinate c : seamPath){
			for(int col = c.X; col < inWidth - 1; col++){
				originalImageCoordinates[c.Y][col] = originalImageCoordinates[c.Y][col + 1];
			}

		}
	}

	private BufferedImage drawSeams(){
		BufferedImage image = duplicateWorkingImage();
		for(ArrayList<Coordinate> path : seamsToDraw){
			for(Coordinate c : path){
				image.setRGB(c.X,c.Y, Color.RED.getRGB());
			}

		}
		return image;
	}

	private BufferedImage costTableToImage(){
		BufferedImage image = newEmptyInputSizedImage();
		int maxValue = 0;
		for(int row = 0; row < costsTable.length; row++){
			for(int col = 1; col < costsTable[row].length - 2; col++){
				if(costsTable[row][col] >= maxValue){
					maxValue = costsTable[row][col];
				}
			}
		}

		int x = maxValue / 255;

		for(int row = 0; row < costsTable.length; row++){
			for(int col = 1; col < costsTable[row].length - 2; col++){
//				Color c = new Color(costsTable[row][col] / x, costsTable[row][col] / x, costsTable[row][col] / x);
				image.setRGB(col, row, costsTable[row][col] / x);
			}
		}
		return image;
	}

	private ArrayList<Coordinate> getVerticalSeam(BufferedImage image){
		ArrayList<Coordinate> seamPath = new ArrayList<Coordinate>();
		int col = getMinAvailableIndex();
//		seamPath.add(new Coordinate(col, gradientImage.getHeight() - 1));
		seamPath.add(originalImageCoordinates[gradientImage.getHeight() - 1][col]);

		for(int row = gradientImage.getHeight() - 1; row > 0 ; row--){
			int top = new Color(grayScaledImage.getRGB(col, row - 1)).getRed();
			int left = new Color(grayScaledImage.getRGB(col - 1, row)).getRed();
			int right = new Color(grayScaledImage.getRGB(col + 1, row)).getRed();
			int costL = Math.abs( top - left ) + Math.abs( right - left );
			int costR = Math.abs(top - right) + Math.abs(left - right) ;
			int costV = Math.abs(left - right );
			int gradient = new Color(gradientImage.getRGB(col, row)).getRed();

			if(costsTable[row][col] == gradient + costsTable[row - 1][col - 1] + costL){
//				seamPath.add(new Coordinate(col - 1, row - 1));
				seamPath.add(originalImageCoordinates[row - 1][col - 1]);
				col--;
			} else if (costsTable[row][col] == gradient + costsTable[row - 1][col + 1] + costR){
//				seamPath.add(new Coordinate(col + 1 , row - 1));
				seamPath.add(originalImageCoordinates[row - 1][col + 1]);
				col++;
			}else if (costsTable[row][col] == gradient + costsTable[row - 1][col] + costV){
//				seamPath.add(new Coordinate(col, row - 1));
				seamPath.add(originalImageCoordinates[row - 1][col]);
			}

		}

		seamsToDraw.add(seamPath);

		return seamPath;
	}

	private Coordinate getOriginalCoordinates(int x, int y){
		return originalImageCoordinates[y][x];
	}

	private int getMinAvailableIndex(){
		int minIndex = 0;
		int lastRow = inHeight - 1
		for(int i = 0; i < inWidth; i++){
			if(costsTable[lastRow][i] <= costsTable[lastRow][minIndex] && availableVerticalSeams[i]){
				minIndex = i;
			}
		}

		availableVerticalSeams[minIndex] = false;
		return minIndex;
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
		Color c;
		int costL;
		int costR;
		int costV;
		// initializing first row

		for (int i = 0; i < costsTable[0].length; i++) {
			c = new Color(gradientImage.getRGB(i, 0));
			costsTable[0][i] = c.getRed();
		}

		for (int row = 1; row < costsTable.length; row++) {
			for (int col = 0; col < costsTable[row].length; col++) {
				int top = new Color(grayScaledImage.getRGB(col, row - 1)).getRed();
				int gradient = new Color(gradientImage.getRGB(col,row)).getRed();

				if (col != 0 && col != costsTable[row].length - 1) {
					int left = new Color(grayScaledImage.getRGB(col - 1, row)).getRed();
					int right = new Color(grayScaledImage.getRGB(col + 1, row)).getRed();
					// taking the min cost from adjacent cells and adding the gradient for the current cell
					costL = Math.abs( top - left ) + Math.abs( right - left );
					costR = Math.abs(top - right) + Math.abs(left - right) ;
					costV = Math.abs(left - right );
					costsTable[row][col] = gradient + Math.min(Math.min(costsTable[row - 1][col - 1] + costL, costsTable[row - 1][col] + costV),
							costsTable[row - 1][col + 1] + costR);
				} else {
					// padding the sides with 0 so it would be too expensive for the seam to pass there
					if (col == 0) {
						costsTable[row][col] = costsTable[row - 1][col] + 255;
					}
					if (col == costsTable[row].length - 1) {
						costsTable[row][col] = costsTable[row - 1][col] + 255;
					}
				}
			}
		}

	}

}