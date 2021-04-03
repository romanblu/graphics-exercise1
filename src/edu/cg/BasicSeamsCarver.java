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

	}

	public BufferedImage carveImage(CarvingScheme carvingScheme) {
		int numberOfVerticalSeamsToCarve = Math.abs(this.outWidth - this.inWidth);
		int numberOfHorizontalSeamsToCarve = Math.abs(this.outHeight - this.inHeight);
		// TODO :  Perform Seam Carving. Overall you need to remove 'numberOfVerticalSeamsToCarve' vertical seams
		// and 'numberOfHorizontalSeamsToCarve' horizontal seams from the image.
		// Note you must consider the 'carvingScheme' parameter in your procedure.
		// Return the resulting image.

		BufferedImage image = duplicateWorkingImage();

		if(carvingScheme == CarvingScheme.VERTICAL_HORIZONTAL) {
			generateCostsTableVertically();
			for (int i = 0; i < numberOfVerticalSeamsToCarve; i++) {
				ArrayList<Coordinate> currentSeam = getVerticalSeam(image);
				removeVerticalSeam(currentSeam);
			}

			generateCostsTableHorizontally();
			for (int i = 0; i < numberOfHorizontalSeamsToCarve; i++) {
				ArrayList<Coordinate> currentSeam = getHorizontalSeam(image);
				removeHorizontalSeam(currentSeam);
			}
		} else if(carvingScheme == CarvingScheme.HORIZONTAL_VERTICAL ){
			generateCostsTableHorizontally();
			for (int i = 0; i < numberOfHorizontalSeamsToCarve; i++) {
				ArrayList<Coordinate> currentSeam = getHorizontalSeam(image);
				removeHorizontalSeam(currentSeam);
			}

			generateCostsTableVertically();
			for (int i = 0; i < numberOfVerticalSeamsToCarve; i++) {
				ArrayList<Coordinate> currentSeam = getVerticalSeam(image);
				removeVerticalSeam(currentSeam);
			}

		} else {
			int carvedHorizontally = 0;
			int carvedVertically = 0;

			for( int i = 0 ; i < numberOfHorizontalSeamsToCarve + numberOfVerticalSeamsToCarve; i ++){
				if(carvedVertically < numberOfVerticalSeamsToCarve){
					generateCostsTableVertically();
					ArrayList<Coordinate> currentSeam = getVerticalSeam(image);
					removeVerticalSeam(currentSeam);
					carvedVertically ++;
				}

				if(carvedHorizontally < numberOfHorizontalSeamsToCarve){
					generateCostsTableHorizontally();
					ArrayList<Coordinate> currentSeam = getHorizontalSeam(image);
					removeHorizontalSeam(currentSeam);
					carvedHorizontally ++;
				}
			}
		}

		image = getCarvedImage();
//		BufferedImage image = costTableToImage();

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

		if(showVerticalSeams){
			generateCostsTableVertically();
			for(int i=0;i<numberOfVerticalSeamsToCarve;i++){
				ArrayList<Coordinate> currentSeam = getVerticalSeam(image);
				removeVerticalSeam(currentSeam);
			}
		}else {
			generateCostsTableHorizontally();
			for(int i=0;i<numberOfHorizontalSeamsToCarve;i++){
				ArrayList<Coordinate> currentSeam = getHorizontalSeam(image);
				removeHorizontalSeam(currentSeam);
			}
		}

		image = drawSeams(image, seamColorRGB);
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

	private void removeHorizontalSeam(ArrayList<Coordinate> currentPath){
		removeSeamFromCoordinatesTable(currentPath, false);
		this.currentHeight--;
		generateCostsTableHorizontally();
	}

	private void removeVerticalSeam(ArrayList<Coordinate> currentPath){
		removeSeamFromCoordinatesTable(currentPath, true);
		this.currentWidth--;
		generateCostsTableVertically();
	}

	private void removeSeamFromCoordinatesTable(ArrayList<Coordinate> currentPath, boolean vertically ) {
		// vertically
		if(vertically){

			for( Coordinate c : currentPath ){
				for(int col = c.X; col < this.currentWidth - 1; col++){
					originalImageCoordinates[c.Y][col] = originalImageCoordinates[c.Y][col + 1];
				}
			}
		}else{
			// horizontally
			for( Coordinate c : currentPath ){
				for(int row = c.Y; row < this.currentHeight - 1; row++){
					originalImageCoordinates[row][c.X] = originalImageCoordinates[row + 1][c.X];
				}
			}
		}
	}

	private int getCostForPixel(int x, int y){

		int pixelEnergy = getCurrentPixelEnergy(x, y, true);
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


	private int getCostForPixelHorizontally(int x, int y){

		int pixelEnergy = getCurrentPixelEnergy(x, y, false);
		int costU, costH, costD;
		int cost = 0;

		if(y != 0 && y != currentHeight - 1 && x != 0 ){

			int left = getOriginalGrayscaleColor(x - 1, y).getRed();
			int bottom = getOriginalGrayscaleColor(x , y + 1).getRed();
			int top = getOriginalGrayscaleColor(x, y - 1).getRed();

			costU = Math.abs( top - left ) + Math.abs( top - bottom );
			costD = Math.abs(bottom - left) + Math.abs(top - bottom) ;
			costH = Math.abs(top - bottom);

			cost = pixelEnergy + Math.min(Math.min( costsTable[y - 1][x - 1] + costU, costsTable[y ][x - 1] + costH ), costsTable[y + 1][x - 1] + costD );

		} else if(x == 0) {
			cost = pixelEnergy;
		} else{

			if(y == 0){
				cost = costsTable[y][x - 1] + 255;
			}

			if(y == currentHeight - 1){
				cost = costsTable[y][x - 1] + 255;
			}
		}

		return cost;
	}

	private int getCurrentPixelEnergy(int x, int y, boolean vertically){
		int verticalEvergy ;
		int horizontalEnergy;

		int width, height;

		width = currentWidth;
		height = currentHeight;

		if( x < width - 1 ){
			verticalEvergy = Math.abs( getOriginalGrayscaleColor(x,y).getRed() - getOriginalGrayscaleColor(x + 1, y).getRed() );
		} else {
			verticalEvergy = Math.abs( getOriginalGrayscaleColor(x,y).getRed() - getOriginalGrayscaleColor(x - 1, y).getRed() );
		}

		if( y < height - 1){
			horizontalEnergy = Math.abs( getOriginalGrayscaleColor(x,y).getRed() - getOriginalGrayscaleColor(x, y + 1).getRed() );
		} else {
			horizontalEnergy = Math.abs( getOriginalGrayscaleColor(x,y).getRed() - getOriginalGrayscaleColor(x, y - 1).getRed() );
		}

		int pixelEnergy = (int) Math.sqrt( Math.pow(verticalEvergy, 2) + Math.pow(horizontalEnergy, 2));

		return pixelEnergy;

	}

	private Color getOriginalGrayscaleColor(int x, int y){
		Coordinate originalPixel = originalImageCoordinates[y][x];
		Color pixelColor = new Color(grayScaledImage.getRGB(originalPixel.X, originalPixel.Y));

		return pixelColor;
	}

	private void initCoordinatesTable(){
		this.originalImageCoordinates = new Coordinate[inHeight][inWidth];

		setForEachInputParameters();

		forEach((y, x) -> {
			this.originalImageCoordinates[y][x] = new Coordinate(x,y);
		});
	}

	private BufferedImage drawSeams(BufferedImage image, int color){

		for(ArrayList<Coordinate> path : this.seamsToDraw){
			for(Coordinate c : path){
				image.setRGB(c.X,c.Y, color);
			}
		}

		return image;
	}

	private BufferedImage costTableToImage(){
		BufferedImage image = newEmptyInputSizedImage();
		int maxValue = 0;
		for(int row = 1; row < costsTable.length - 1; row++){
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
		int col = getMinVerticalCostIndex();
		currentSeam.add(0, new Coordinate(col, this.currentHeight - 1));
		originalSeam.add(0,originalImageCoordinates[this.currentHeight - 1][col]);

		for(int row = currentHeight - 1; row > 0 ; row--){

			int top = getOriginalGrayscaleColor(col, row - 1).getRed();
			int left = getOriginalGrayscaleColor(col - 1, row).getRed();
			int right = getOriginalGrayscaleColor(col + 1, row).getRed();

			int costL = Math.abs( top - left ) + Math.abs( right - left );
			int costR = Math.abs(top - right) + Math.abs(left - right) ;
			int costV = Math.abs(left - right );

			int pixelEnergy = getCurrentPixelEnergy(col, row, true);

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


	private ArrayList<Coordinate> getHorizontalSeam(BufferedImage image){
		ArrayList<Coordinate> originalSeam = new ArrayList<Coordinate>();
		ArrayList<Coordinate> currentSeam = new ArrayList<Coordinate>();
		int row = getMinHorizontalCostIndex();
		currentSeam.add(0, new Coordinate(this.currentWidth - 1, row));
		originalSeam.add(0,originalImageCoordinates[row][this.currentWidth - 1]);

		for(int col = currentWidth - 1; col > 0 ; col--){

			int left = getOriginalGrayscaleColor(col - 1, row).getRed();
			int bottom = getOriginalGrayscaleColor(col , row + 1).getRed();
			int top = getOriginalGrayscaleColor(col, row - 1).getRed();

			int costU = Math.abs( top - left ) + Math.abs( top - bottom );
			int costD = Math.abs(bottom - left) + Math.abs(top - bottom) ;
			int costH = Math.abs(top - bottom);

			int pixelEnergy = getCurrentPixelEnergy(col, row, false);

			if(costsTable[row][col] == pixelEnergy + costsTable[row][col - 1] + costH){
				currentSeam.add(0, new Coordinate(col - 1, row ));
				originalSeam.add(0, originalImageCoordinates[row ][col - 1]);
			} else if (costsTable[row][col] == pixelEnergy + costsTable[row - 1][col - 1] + costU){
				currentSeam.add(0, new Coordinate(col - 1 , row - 1));
				originalSeam.add(0, originalImageCoordinates[row - 1][col - 1]);
				row--;
			}else
//				(costsTable[row][col] == pixelEnergy + costsTable[row + 1][col - 1] + costD)   costsTable[y + 1][x - 1] + costD
				{
				currentSeam.add(0, new Coordinate(col - 1, row + 1));
				originalSeam.add(0, originalImageCoordinates[row + 1][col - 1]);
				row++;
			}
		}

		seamsToDraw.add(originalSeam);

		return currentSeam;
	}

	private int getMinVerticalCostIndex(){
		int minIndex = 0;
		int lastRow = this.currentHeight - 1;
		for(int i = 0; i < this.currentWidth; i++){
			if(costsTable[lastRow][i] <= costsTable[lastRow][minIndex] ){
				minIndex = i;
			}
		}

		return minIndex;
	}


	private int getMinHorizontalCostIndex(){
		int minIndex = 0;
		int lastCol = this.currentWidth - 1;
		for(int i = 0; i < this.currentHeight; i++){
			if(costsTable[i][lastCol] <= costsTable[minIndex][lastCol] ){
				minIndex = i;
			}
		}

		return minIndex;
	}


	private void generateCostsTableVertically() {
		this.costsTable = new int[this.currentHeight][this.currentWidth];

		for(int y=0; y < currentHeight ; y++){
			for(int x =0;x < currentWidth ;x++){
				costsTable[y][x] = getCostForPixel(x, y);
			}
		}

	}


	private void generateCostsTableHorizontally() {
		this.costsTable = new int[this.currentHeight][this.currentWidth];

		for(int x=0; x < currentWidth  ; x++){
			for(int y =0; y< currentHeight  ; y++){
				costsTable[y][x] = getCostForPixelHorizontally(x , y);
			}
		}

	}


}