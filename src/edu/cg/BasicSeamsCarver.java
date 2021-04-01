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
	int currentWidth, currentHeight;

	public BasicSeamsCarver(Logger logger, BufferedImage workingImage,
							int outWidth, int outHeight, RGBWeights rgbWeights) {
		super((s) -> logger.log("Seam carving: " + s), workingImage, rgbWeights, outWidth, outHeight);
		// TODO : Include some additional initialization procedures.
		this.currentHeight = inHeight;
		this.currentWidth = inWidth;

		grayScaledImage = greyscale();
		gradientImage = gradientMagnitude();
		initCoordinatesTable();
		costsTable = new int[this.inHeight][this.inWidth];
		availableVerticalSeams = initializeBooleanArray(gradientImage.getWidth());
		generateCostsTable(costsTable);
		seamsToDraw = new ArrayList<ArrayList<Coordinate>>();
//		originalImageCoordinates = initCoordinatesTable();

	}

	public BufferedImage carveImage(CarvingScheme carvingScheme) {
		int numberOfVerticalSeamsToCarve = Math.abs(this.outWidth - this.inWidth);
		int numberOfHorizontalSeamsToCarve = Math.abs(this.outHeight - this.inHeight);
		// TODO :  Perform Seam Carving. Overall you need to remove 'numberOfVerticalSeamsToCarve' vertical seams
		// and 'numberOfHorizontalSeamsToCarve' horizontal seams from the image.
		// Note you must consider the 'carvingScheme' parameter in your procedure.
		// Return the resulting image.

		BufferedImage image = duplicateWorkingImage();

		ArrayList<Coordinate> currentSeam = getVerticalSeam(image);


		currentWidth--;
		// removeSeamFromCoordinatesTable
		removeSeamFromCostTable(currentSeam);


		image = drawSeams(image);

//		image = costTableToImage();

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

		BufferedImage image = newEmptyInputSizedImage();
		image = drawSeams(image);
		return  image;
	}

	private void removeSeamFromCostTable(ArrayList<Coordinate> currentPath ) {
		for(Coordinate c : currentPath){
			for(int col = c.X; col < currentWidth - 1; col++){
				costsTable[c.Y][col] = costsTable[c.Y][col + 1];
			}
		}

		// update the costs
		for(Coordinate c : currentPath){
			// getGradientForPixel (c.X, c.Y)

		}
	}

	private int getCostForPixel(int x, int y){

		int pixelEnergy = getCurrentPixelEnergy(x, y);
		int costL, costR, costV;
		int cost = 0;

		if(x != 0 && x != currentWidth - 1 && y != 0 ){

			int left = getOriginalGrayscaleColor(x - 1, y).getRed();
			int right = getOriginalGrayscaleColor(x + 1, y).getRed();
			int top = getOriginalGrayscaleColor(x, y - 1).getRed();

			costL = Math.abs( top - left ) + Math.abs( right - left );
			costR = Math.abs(top - right) + Math.abs(left - right) ;
			costV = Math.abs(left - right );

			cost = pixelEnergy + Math.min(Math.min( costsTable[y - 1][x - 1] + costL, costsTable[y - 1][x] + costV ), costsTable[y - 1][x + 1] + costR );

		} else if(y == 0) {
			cost = pixelEnergy;
		} else{
			Coordinate originalTopPixel = originalImageCoordinates[y - 1][x];

			if(x == 0){
				cost = costsTable[originalTopPixel.Y][originalTopPixel.X] + 255;
			}

			if(x == currentWidth - 1){
				cost = costsTable[originalTopPixel.Y][originalTopPixel.X] + 255;
			}
		}

		return cost;
	}

	private int getCurrentPixelEnergy(int x, int y){
		/*
		Color current, rightColor, bottomColor;
		Coordinate originalPixel = originalImageCoordinates[y][x];
		Coordinate originalRightPixel = originalImageCoordinates[y][x + 1];
		Coordinate originalBottomPixel = originalImageCoordinates[y + 1][x];
		// add function for getting original grayscale values for coordinate
		// change the function to suit the recitation

		if(originalPixel.X < inWidth - 1 && originalPixel.Y < inHeight - 1){
			current = new Color(grayScaledImage.getRGB(originalPixel.X , originalPixel.Y));
			rightColor = new Color(grayScaledImage.getRGB(originalRightPixel.X, originalRightPixel.Y));
			bottomColor = new Color(grayScaledImage.getRGB(originalBottomPixel.X, originalBottomPixel.Y));
		}else{
			current = new Color(grayScaledImage.getRGB(originalPixel.X , originalPixel.Y));
			rightColor = new Color(grayScaledImage.getRGB(0, originalRightPixel.Y));
			bottomColor = new Color(grayScaledImage.getRGB(originalBottomPixel.X, 0));
		}

		int horizontalGradient = (int) Math.pow(rightColor.getRed() - current.getRed(), 2) ;
		int verticalGradient = (int) Math.pow(bottomColor.getRed() - current.getRed(), 2);

		int gradientValue = (int) (Math.sqrt(horizontalGradient + verticalGradient) / 360 * 255 ) ;

		return gradientValue;

		 */
		int verticalEvergy ;


		if( x < currentWidth - 1 ){
			verticalEvergy = Math.abs( getOriginalGrayscaleColor(x,y).getRed() - getOriginalGrayscaleColor(x + 1, y).getRed() );
		} else {
			verticalEvergy = Math.abs( getOriginalGrayscaleColor(x,y).getRed() - getOriginalGrayscaleColor(x - 1, y).getRed() );
		}

		return verticalEvergy;

	}

	private Color getOriginalGrayscaleColor(int x, int y){
		Coordinate originalPixel = originalImageCoordinates[y][x];
		Color pixelColor = new Color(grayScaledImage.getRGB(originalPixel.X, originalPixel.Y));

		return pixelColor;
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

	private void initCoordinatesTable(){
		this.originalImageCoordinates = new Coordinate[inHeight][inWidth];

		setForEachInputParameters();

		forEach((y, x) -> {
			this.originalImageCoordinates[y][x] = new Coordinate(x,y);
		});

//		return originalImageCoordinates;
	}

	private void removeSeamCoordinateTable(List<Coordinate> seamPath){
		for(Coordinate c : seamPath){
			for(int col = c.X; col < inWidth - 1; col++){
				originalImageCoordinates[c.Y][col] = originalImageCoordinates[c.Y][col + 1];
			}

		}
	}

	private BufferedImage drawSeams(BufferedImage image){

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
		ArrayList<Coordinate> originalSeam = new ArrayList<Coordinate>();
		ArrayList<Coordinate> currentSeam = new ArrayList<Coordinate>();
		int col = getMinAvailableIndex();
		currentSeam.add(0, new Coordinate(col, gradientImage.getHeight() - 1));
		originalSeam.add(0,originalImageCoordinates[gradientImage.getHeight() - 1][col]);

		for(int row = currentHeight - 1; row > 0 ; row--){


			int top = getOriginalGrayscaleColor(col, row - 1).getRed();
			int left = getOriginalGrayscaleColor(col - 1, row).getRed();
			int right = getOriginalGrayscaleColor(col + 1, row).getRed();

			int costL = Math.abs( top - left ) + Math.abs( right - left );
			int costR = Math.abs(top - right) + Math.abs(left - right) ;
			int costV = Math.abs(left - right );

			int pixelEnergy = getCurrentPixelEnergy(col, row);

			if(costsTable[row][col] == pixelEnergy + costsTable[row - 1][col - 1] + costL){
				currentSeam.add(0, new Coordinate(col - 1, row - 1));
				originalSeam.add(0, originalImageCoordinates[row - 1][col - 1]);
				col--;
			} else if (costsTable[row][col] == pixelEnergy + costsTable[row - 1][col + 1] + costR){
				currentSeam.add(0, new Coordinate(col + 1 , row - 1));
				originalSeam.add(0, originalImageCoordinates[row - 1][col + 1]);
				col++;
			}else if (costsTable[row][col] == pixelEnergy + costsTable[row - 1][col] + costV){
				currentSeam.add(0, new Coordinate(col, row - 1));
				originalSeam.add(0, originalImageCoordinates[row - 1][col]);
			}

		}

		seamsToDraw.add(originalSeam);

		return currentSeam;
	}

	// update to use current Width
	private int getMinAvailableIndex(){
		int minIndex = 0;
		int lastRow = this.currentHeight - 1;
		for(int i = 0; i < this.currentWidth; i++){
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

	private void generateCostsTable(int[][] costsTable) {
	/*	Color c;
		int costL;
		int costR;
		int costV;


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

 */

		for(int row=0; row < inHeight ; row++){
			for(int col =0;col< inWidth ;col++){
				costsTable[row][col] = getCostForPixel(col, row);
			}
		}

	}

}