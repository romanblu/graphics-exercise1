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
	int[][] costsTable;

	Coordinate[][] originalImageCoordinates;
	private ArrayList< ArrayList<Coordinate> > seamsToDraw;


	int currentWidth, currentHeight;

	public BasicSeamsCarver(Logger logger, BufferedImage workingImage,
							int outWidth, int outHeight, RGBWeights rgbWeights) {
		super((s) -> logger.log("Seam carving: " + s), workingImage, rgbWeights, outWidth, outHeight);
		// TODO : Include some additional initialization procedures.

		this.currentHeight = inHeight;
		this.currentWidth = inWidth ;
		seamsToDraw = new ArrayList<>();
		grayScaledImage = greyscale();
		initCoordinatesTable();
		generateCostsTable();
	}

	public BufferedImage carveImage(CarvingScheme carvingScheme) {
		int numberOfVerticalSeamsToCarve = Math.abs(this.outWidth - this.inWidth);
		int numberOfHorizontalSeamsToCarve = Math.abs(this.outHeight - this.inHeight);
		// TODO :  Perform Seam Carving. Overall you need to remove 'numberOfVerticalSeamsToCarve' vertical seams
		// and 'numberOfHorizontalSeamsToCarve' horizontal seams from the image.
		// Note you must consider the 'carvingScheme' parameter in your procedure.
		// Return the resulting image.

		BufferedImage image = duplicateWorkingImage();

		for(int i=0; i < numberOfVerticalSeamsToCarve; i++){
			ArrayList<Coordinate> currentSeam = getVerticalSeam(image);
			removeVerticalSeam(currentSeam);
		}

		image = getCarvedImage();

		return image;
	}




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

		for(int i=0; i < numberOfVerticalSeamsToCarve; i++){
			ArrayList<Coordinate> currentSeam = getVerticalSeam(image);
			removeVerticalSeam(currentSeam);
		}

		image = duplicateWorkingImage();
		image = drawSeams(image);
		return  image;
	}

	private BufferedImage getCarvedImage(){
		BufferedImage result = newEmptyOutputSizedImage();

		setForEachOutputParameters();

		forEach((y, x) -> {
			Coordinate currentPixelCoordinate = originalImageCoordinates[y][x];
			Color currentColor = new Color(workingImage.getRGB(currentPixelCoordinate.X, currentPixelCoordinate.Y));
			result.setRGB(x, y, currentColor.getRGB() );
		});

		return result;
	}

	private void removeVerticalSeam(ArrayList<Coordinate> currentPath){
		removeSeamFromCoordinatesTable(currentPath);
		this.currentWidth--;
		generateCostsTable();
	}

	private void removeSeamFromCoordinatesTable(ArrayList<Coordinate> currentPath ) {
		// vertically
		for( Coordinate c : currentPath ){
			for(int col = c.X; col < this.currentWidth - 1; col++){
				originalImageCoordinates[c.Y][col] = originalImageCoordinates[c.Y][col + 1];
			}
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

			if(x == 0){
				cost = costsTable[y - 1][x] + 255;
			}

			if(x == currentWidth - 1){
				cost = costsTable[y - 1][x] + 255;
			}
		}

		return cost;
	}

	private int getCurrentPixelEnergy(int x, int y){
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
	}

	private BufferedImage drawSeams(BufferedImage image){

		for(ArrayList<Coordinate> path : this.seamsToDraw){
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
				image.setRGB(col, row, costsTable[row][col] / x);
			}
		}
		return image;
	}

	private ArrayList<Coordinate> getVerticalSeam(BufferedImage image){
		ArrayList<Coordinate> originalSeam = new ArrayList<Coordinate>();
		ArrayList<Coordinate> currentSeam = new ArrayList<Coordinate>();
		int col = getMinCostIndex();
		currentSeam.add(0, new Coordinate(col, this.currentHeight - 1));
		originalSeam.add(0,originalImageCoordinates[this.currentHeight - 1][col]);

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
	private int getMinCostIndex(){
		int minIndex = 0;
		int lastRow = this.currentHeight - 1;
		for(int i = 0; i < this.currentWidth; i++){
			if(costsTable[lastRow][i] <= costsTable[lastRow][minIndex] ){
				minIndex = i;
			}
		}

		return minIndex;
	}

	private void generateCostsTable() {
		this.costsTable = new int[this.currentHeight][this.currentWidth];

		for(int row=0; row < currentHeight ; row++){
			for(int col =0;col< currentWidth ;col++){
				costsTable[row][col] = getCostForPixel(col, row);
			}
		}

	}

}