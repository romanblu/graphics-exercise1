package edu.cg;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class ImageProcessor extends FunctioalForEachLoops {
	
	//MARK: Fields
	public final Logger logger;
	public final BufferedImage workingImage;
	public final RGBWeights rgbWeights;
	public final int inWidth;
	public final int inHeight;
	public final int workingImageType;
	public final int outWidth;
	public final int outHeight;
	
	//MARK: Constructors
	public ImageProcessor(Logger logger, BufferedImage workingImage,
			RGBWeights rgbWeights, int outWidth, int outHeight) {
		super(); //Initializing for each loops...
		
		this.logger = logger;
		this.workingImage = workingImage;
		this.rgbWeights = rgbWeights;
		inWidth = workingImage.getWidth();
		inHeight = workingImage.getHeight();
		workingImageType = workingImage.getType();
		this.outWidth = outWidth;
		this.outHeight = outHeight;
		setForEachInputParameters();
	}
	
	public ImageProcessor(Logger logger,
			BufferedImage workingImage,
			RGBWeights rgbWeights) {
		this(logger, workingImage, rgbWeights,
				workingImage.getWidth(), workingImage.getHeight());
	}
	
	//MARK: Change picture hue - example
	public BufferedImage changeHue() {
		logger.log("Prepareing for hue changing...");
		
		int r = rgbWeights.redWeight;
		int g = rgbWeights.greenWeight;
		int b = rgbWeights.blueWeight;
		int max = rgbWeights.maxWeight;
		
		BufferedImage ans = newEmptyInputSizedImage();
		
		forEach((y, x) -> {
			Color c = new Color(workingImage.getRGB(x, y));
			int red = r*c.getRed() / max;
			int green = g*c.getGreen() / max;
			int blue = b*c.getBlue() / max;
			Color color = new Color(red, green, blue);
			ans.setRGB(x, y, color.getRGB());
		});
		
		logger.log("Changing hue done!");
		
		return ans;
	}
	
	//MARK: Nearest neighbor - example
	public BufferedImage nearestNeighbor() {
		logger.log("applies nearest neighbor interpolation.");
		BufferedImage ans = newEmptyOutputSizedImage();
		
		pushForEachParameters();
		setForEachOutputParameters();
		
		forEach((y, x) -> {
			int imgX = (int)Math.round((x*inWidth) / ((float)outWidth));
			int imgY = (int)Math.round((y*inHeight) / ((float)outHeight));
			imgX = Math.min(imgX,  inWidth-1);
			imgY = Math.min(imgY, inHeight-1);
			ans.setRGB(x, y, workingImage.getRGB(imgX, imgY));
		});
		
		popForEachParameters();
		
		return ans;
	}
	
	//MARK: Unimplemented methods
	public BufferedImage greyscale() {
		BufferedImage result = newEmptyInputSizedImage();

		forEach((y, x) -> {
			Color c = new Color(workingImage.getRGB(x, y));
			int sumWeights = rgbWeights.redWeight + rgbWeights.greenWeight + rgbWeights.blueWeight;
			int red = c.getRed() * rgbWeights.redWeight / sumWeights;
			int green = c.getGreen() * rgbWeights.greenWeight / sumWeights;
			int blue = c.getBlue() * rgbWeights.blueWeight / sumWeights;
			Color newColor = new Color(red + green + blue, red + green + blue, red + green + blue);

			result.setRGB(x, y, newColor.getRGB());
		});

		logger.log("Applying grayscale ");

		return result;
	}

	public BufferedImage gradientMagnitude() {
		BufferedImage result = newEmptyInputSizedImage();
		BufferedImage grayScaledImage = this.greyscale();

		forEach((y, x) -> {
			Color current, rightColor, bottomColor;

			if(x < grayScaledImage.getWidth() - 1 && y < grayScaledImage.getHeight() - 1){
				current = new Color(grayScaledImage.getRGB(x , y));
				rightColor = new Color(grayScaledImage.getRGB(x + 1, y));
				bottomColor = new Color(grayScaledImage.getRGB(x, y + 1));
			}else{
				current = new Color(grayScaledImage.getRGB(x , y));
				rightColor = new Color(grayScaledImage.getRGB(0, y));
				bottomColor = new Color(grayScaledImage.getRGB(x, 0));
			}

			int horizontalGradient = (int) Math.pow(rightColor.getRed() - current.getRed(), 2) ;
			int verticalGradient = (int) Math.pow(bottomColor.getRed() - current.getRed(), 2);

			int gradientValue = (int) (Math.sqrt(horizontalGradient + verticalGradient) / 360 * 255 ) ;
			result.setRGB(x, y, new Color(gradientValue, gradientValue, gradientValue).getRGB());

		});

		return result;
	}

	public BufferedImage bilinear() {
		logger.log("Applying bilinear interpolation");
		BufferedImage result = newEmptyOutputSizedImage();

		pushForEachParameters();
		setForEachParameters(outWidth - 2, outHeight - 2);

		forEach((y, x) -> {
			int imgX = (int) Math.floor((x*inWidth) / ((float) outWidth));
			int imgY = (int) Math.floor((y*inHeight) / ((float) outHeight));

			float t = (float) ((x*inWidth) / ((float) outWidth) - Math.floor((x*inWidth) / ((float) outWidth)));
			float s = (float) ((y*inHeight) / ((float) outHeight) - Math.floor((y*inHeight) / ((float) outHeight)));

			Color c1 = new Color(workingImage.getRGB(imgX, imgY));
			Color c2 = new Color(workingImage.getRGB(imgX + 1, imgY));
			Color c3 = new Color(workingImage.getRGB(imgX, imgY + 1));
			Color c4 = new Color(workingImage.getRGB(imgX + 1, imgY + 1));

			Color c12 = new Color(
					(int) (c1.getRed() * (1-t) + t * c2.getRed()),
					(int) (c1.getGreen() * (1-t) + t * c2.getGreen()),
					(int) (c1.getBlue() * (1-t) + t * c2.getBlue()));

			Color c34 = new Color(
					(int) (c3.getRed() * (1-t) + t * c4.getRed()),
					(int) (c3.getGreen() * (1-t) + t * c4.getGreen()),
					(int) (c3.getBlue() * (1-t) + t * c4.getBlue()));

			Color c = new Color(
					(int) (c12.getRed() * (1 - s) + c34.getRed() * s),
					(int) (c12.getGreen() * (1 - s) + c34.getGreen() * s),
					(int) (c12.getBlue() * (1 - s) + c34.getBlue() * s)
			);

			result.setRGB(x, y, c.getRGB());
		});

		popForEachParameters();

		return result;
	}
	
	//MARK: Utilities
	public final void setForEachInputParameters() {
		setForEachParameters(inWidth, inHeight);
	}
	
	public final void setForEachOutputParameters() {
		setForEachParameters(outWidth, outHeight);
	}
	
	public final BufferedImage newEmptyInputSizedImage() {
		return newEmptyImage(inWidth, inHeight);
	}
	
	public final BufferedImage newEmptyOutputSizedImage() {
		return newEmptyImage(outWidth, outHeight);
	}
	
	public final BufferedImage newEmptyImage(int width, int height) {
		return new BufferedImage(width, height, workingImageType);
	}
	
	public final BufferedImage duplicateWorkingImage() {
		BufferedImage output = newEmptyInputSizedImage();
		
		forEach((y, x) -> 
			output.setRGB(x, y, workingImage.getRGB(x, y))
		);
		
		return output;
	}
}
