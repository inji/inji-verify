import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.awt.geom.Ellipse2D;
import java.util.function.Function;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.awt.image.AffineTransformOp;
import java.awt.geom.AffineTransform;

public class QRCoodeVariationGenerator {
    static String strQRCodevariation = "Original";

    public static final int LOW_RESOLUTION = 1 << 0; // 00000001     --- To generate Low Resolution QR code variation from original QR code
    public static final int LOW_LIGHTING= 1 << 1; // 00000010    --- To generate QR code captured in low lighting variation from original QR code 
    public static final int GLOSSY = 1 << 2; // 00000100       --- To generate QR code which is printied on a glossy paper variation
    public static final int LOW_CONTRAST = 1 << 3; // 00001000   --- To generate QR code captured of high contrast variation from original QR code 
    public static final int BLURRED = 1 << 4; // 00010000     --- To generate QR code captured and is blurry
    public static final int DISTORTED = 1 << 5; // 00100000   --- To generate QR code distroyed variation from original QR code
    public static final int SCRATCHED = 1 << 6; // 01000000   --- To generate QR code variation from original QR code has some scractches
    public static final int TORN = 1 << 7; // 10000000        --- To generate QR code variation from original QR code is torned
    public static final int OBSCURED = 1 << 8; // 00000001 00000000 --- To generate QR code variation from original QR code is obscured
    public static final int NOISY = 1 << 9; // 00000010 00000000    --- To generate nosiy Resolution QR code variation from original QR code
    public static final int CURVED_SUFACE = 1 << 10; // 00000100 00000000  --- To generate Low Resolution QR code variation from original QR code
    public static final int DAMAGED = 1 << 11; // 00001000 00000000    --- To generate QR code variation from original QR code is damaged
    public static final int SIDE_ANGLE = 1 << 12; // 00010000 00000000 --- To generate QR code variation where the scanning is done not straight of original QR code 
    public static final int MATTE = 1 << 13; // 00100000 00000000    --- To generate QR code which is printied on a paper paper variation
    public static final int POORQUALITY_PAPER = 1 << 14; // 01000000 00000000  --- To generate QR code which is printied on a poor quatlity paper variation
    


    // Variable to store the combination of variations
    public static int QRCodeVariations = 0;

    public static void main(String[] args) throws IOException {
        // Check if the correct number of arguments is provided
        if (args.length < 2) {
            System.out.println("Usage: java QRCoodeVariationGenerator <inputQRCodeTemplateDirectoryPath> <outputUniqueQRCodeDataPath>");
            return;
        }

        /// Provide folder where the base template image present
        String inputQRCodeTemplateDirectoryPath =  args[0]; 

        /// Provide folder to where the generated variant images should be copied
        String outputUniqueQRCodeDataPath = args[1];

        generateQRCodeVariations(inputQRCodeTemplateDirectoryPath, outputUniqueQRCodeDataPath);
    }


    /**************************************************************
     * Generates mutlple variations based on the provided
     * input image and copies to the target folder
     ****************************************************************/
    public static void generateQRCodeVariations(String inputQRCodeTemplateDirectoryPath,
            String outputUniqueQRCodeDataPath) {
        resetQRCodeVariations();

        // Set multiple variations at once 
        setQRCodeVariations(LOW_RESOLUTION | LOW_LIGHTING  | GLOSSY | LOW_CONTRAST | BLURRED | DISTORTED | SCRATCHED | TORN | OBSCURED | NOISY | DAMAGED | CURVED_SUFACE | SIDE_ANGLE | MATTE | POORQUALITY_PAPER);

         // setQRCodeVariations( POORQUALITY_PAPER);

        try {
            // Get all file names in the directory and subdirectories
            List<String> fileNameAbsPaths = listFiles(inputQRCodeTemplateDirectoryPath);

            // Print all file names
            for (String fileNameAbsPath : fileNameAbsPaths) {
                // Convert the string path to a Path object
                Path path = Paths.get(fileNameAbsPath);

                // Get the file name
                String fileName = path.getFileName().toString();

                // Get the parent directory of the file
                Path parentPath = path.getParent();

                // Extract the last two segments from the parent path
                String segment1 = parentPath.getName(parentPath.getNameCount() - 2).toString();
                String segment2 = parentPath.getName(parentPath.getNameCount() - 1).toString();

                // Combine the segments to get the desired output
                String extractedPath = String.join(File.separator, segment1, segment2) + File.separator;

                // System.out.println("fileNameAbsPath: " + fileNameAbsPath + "
                // outputUniqueQRCodeDataPath : " + outputUniqueQRCodeDataPath + "
                // extractedPath : " + extractedPath + " File Name: " + fileName);

                generateQRCodeVariations(fileNameAbsPath, outputUniqueQRCodeDataPath, extractedPath, fileName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to set multiple variations
    public static void setQRCodeVariations(int variations) {
        QRCodeVariations |= variations;
    }

    // Method to reset all variations
    public static void resetQRCodeVariations() {
        QRCodeVariations = 0;
    }

    // Method to check if a specific variation is set
    public static boolean isVariationSet(int variation) {
        return (QRCodeVariations & variation) != 0;
    }

    public static void generateQRCodeVariations(String fileNameAbsPath,
            String outputUniqueQRCodeRelativePath, String QRCodePath, String FileName) {

        // Create a list of active Variations based on boolean flags
        List<Function<BufferedImage, BufferedImage>> activeVariations = new ArrayList<>();


        if (isVariationSet(LOW_RESOLUTION))
            activeVariations.add(QRCoodeVariationGenerator::applyLowResolution);
        if (isVariationSet(LOW_LIGHTING))
            activeVariations.add(QRCoodeVariationGenerator::applyLowLighting);
        if (isVariationSet(GLOSSY))
            activeVariations.add(QRCoodeVariationGenerator::applyGlossyEffect);
        if (isVariationSet(LOW_CONTRAST))
            activeVariations.add(QRCoodeVariationGenerator::applyLowContrast);    
        if (isVariationSet(BLURRED))
            activeVariations.add(QRCoodeVariationGenerator::applyBlurEffect);   
        if (isVariationSet(DISTORTED))
            activeVariations.add(QRCoodeVariationGenerator::applyDistortionEffect);                    
        if (isVariationSet(SCRATCHED))
            activeVariations.add(QRCoodeVariationGenerator::generateScratchedQRCode);   
        if (isVariationSet(TORN))
            activeVariations.add(QRCoodeVariationGenerator::generateTornQRCode);   
        if (isVariationSet(OBSCURED))
            activeVariations.add(QRCoodeVariationGenerator::generatePartiallyObscuredQRCode);          
        if (isVariationSet(CURVED_SUFACE))
            activeVariations.add(QRCoodeVariationGenerator::generateCurvedQRCode);
        if (isVariationSet(NOISY))
            activeVariations.add(QRCoodeVariationGenerator::generateNoisyQRCode);
        if (isVariationSet(DAMAGED))
            activeVariations.add(QRCoodeVariationGenerator::generateDamagedQRCode);   
        if (isVariationSet(SIDE_ANGLE))
            activeVariations.add(QRCoodeVariationGenerator::applySideAngleEffect);  
        if (isVariationSet(MATTE))
            activeVariations.add(QRCoodeVariationGenerator::applyMatteEffect);  
        if (isVariationSet(POORQUALITY_PAPER))
            activeVariations.add(QRCoodeVariationGenerator::applyPoorQualityEffect);  

        // Load the original QR image
        BufferedImage QRCodeImage = null;
        try {
            QRCodeImage = ImageIO.read(new File(fileNameAbsPath));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        if (QRCodeImage == null) {
            System.out.println("Failed to load QRCode image.");
            return;
        }

        strQRCodevariation = "Original";
        String outputQRCodePath = Paths.get(outputUniqueQRCodeRelativePath, QRCodePath + strQRCodevariation + "_" + FileName).toString();


        // Create directories if they do not exist
        Path outputPath = Paths.get(outputQRCodePath).getParent();

        // First copy the original image as well
        try {
            if (!Files.exists(outputPath)) {
                Files.createDirectories(outputPath);
            }
            ImageIO.write(QRCodeImage, "png", new File(outputQRCodePath));
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return;
        }

        // Apply the Variations and genearate varations for this profile
        for (int j = 1; j <= activeVariations.size(); j++) {
            //Function<BufferedImage, BufferedImage> Variation = activeVariations.get((j - 1) % activeVariations.size());
            Function<BufferedImage, BufferedImage> Variation = activeVariations.get((j - 1));
            BufferedImage QRCodeImageWithVariation = Variation.apply(QRCodeImage);

            outputQRCodePath = Paths.get(outputUniqueQRCodeRelativePath, QRCodePath + strQRCodevariation + "_" + FileName).toString();

            try {
                ImageIO.write(QRCodeImageWithVariation, "png", new File(outputQRCodePath));
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    public static List<String> listFiles(String directoryPath) throws IOException {
        List<String> fileNames = new ArrayList<>();

        // Start walking through the file tree
        Files.walkFileTree(Paths.get(directoryPath), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                // Add the file name to the list
                fileNames.add(file.toString());
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                // If a file visit fails, print the error and continue
                System.err.println("Failed to access file: " + file.toString() + " (" + exc.getMessage() + ")");
                return FileVisitResult.CONTINUE;
            }
        });
        return fileNames;
    }


    

    /**************************************************************
     * Function to generate QR code on poor quality paper
     ****************************************************************/
    public static BufferedImage applyPoorQualityEffect(BufferedImage image) {
        strQRCodevariation = "poorquality_paper";
        int width = image.getWidth();
        int height = image.getHeight();

        // Create a new image to hold the poor quality effect
        BufferedImage poorQualityImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = poorQualityImage.createGraphics();

        // Draw the original QR code onto the new image
        g2d.drawImage(image, 0, 0, null);

        // Simulate faded colors (desaturating the image slightly)
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgba = poorQualityImage.getRGB(x, y);
                Color color = new Color(rgba, true);

                // Reduce saturation and brightness
                int fadedRed = (int) (color.getRed() * 0.85);   // Fade the red
                int fadedGreen = (int) (color.getGreen() * 0.85); // Fade the green
                int fadedBlue = (int) (color.getBlue() * 0.85);  // Fade the blue

                Color newColor = new Color(fadedRed, fadedGreen, fadedBlue, color.getAlpha());
                poorQualityImage.setRGB(x, y, newColor.getRGB());
            }
        }

        // Simulate grain or noise
        Random random = new Random();
        int noiseAmount = 500; // Number of noise grains to add

        for (int i = 0; i < noiseAmount; i++) {
            int x = random.nextInt(width);
            int y = random.nextInt(height);

            // Add random black or white noise grain
            int noiseColor = random.nextBoolean() ? Color.BLACK.getRGB() : Color.WHITE.getRGB();
            poorQualityImage.setRGB(x, y, noiseColor);
        }

        // Simulate ink smudge or blur
        applySmudgeEffect(poorQualityImage);

        g2d.dispose();
        return poorQualityImage;
    }

    // Helper method to simulate minor smudge/blur effect
    private static void applySmudgeEffect(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        // Apply a very subtle blur to simulate ink bleeding on poor paper
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                // Take the average color of the surrounding pixels to create a slight blur/smudge effect
                Color pixelColor = new Color(image.getRGB(x, y));
                Color left = new Color(image.getRGB(x - 1, y));
                Color right = new Color(image.getRGB(x + 1, y));
                Color up = new Color(image.getRGB(x, y - 1));
                Color down = new Color(image.getRGB(x, y + 1));

                int avgRed = (pixelColor.getRed() + left.getRed() + right.getRed() + up.getRed() + down.getRed()) / 5;
                int avgGreen = (pixelColor.getGreen() + left.getGreen() + right.getGreen() + up.getGreen() + down.getGreen()) / 5;
                int avgBlue = (pixelColor.getBlue() + left.getBlue() + right.getBlue() + up.getBlue() + down.getBlue()) / 5;

                Color smudgedColor = new Color(avgRed, avgGreen, avgBlue, pixelColor.getAlpha());
                image.setRGB(x, y, smudgedColor.getRGB());
            }
        }
    }


    /**************************************************************
     * Function to generate QR code on matte paper
     ****************************************************************/
    public static BufferedImage applyMatteEffect(BufferedImage image) {
        strQRCodevariation = "matte";
        int width = image.getWidth();
        int height = image.getHeight();

        // Create a new image to hold the matte effect
        BufferedImage matteImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = matteImage.createGraphics();

        // Draw the original QR code onto the new image
        g2d.drawImage(image, 0, 0, null);

        // Apply a soft blur for a matte, non-glossy finish
        float[] blurKernel = {
            1f / 9f, 1f / 9f, 1f / 9f,
            1f / 9f, 1f / 9f, 1f / 9f,
            1f / 9f, 1f / 9f, 1f / 9f
        };
        ConvolveOp blurOp = new ConvolveOp(new Kernel(3, 3, blurKernel), ConvolveOp.EDGE_NO_OP, null);
        BufferedImage blurredImage = blurOp.filter(image, null);

        // Draw the blurred QR code onto the matte image
        g2d.drawImage(blurredImage, 0, 0, null);

        // Optionally, reduce color saturation for a flatter look
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgba = matteImage.getRGB(x, y);
                Color color = new Color(rgba, true);
                
                // Reduce the intensity of colors (desaturation for a matte look)
                int red = (int) (color.getRed() * 0.8);   // Decrease by 20%
                int green = (int) (color.getGreen() * 0.8); // Decrease by 20%
                int blue = (int) (color.getBlue() * 0.8);  // Decrease by 20%
                
                // Set the new color with less saturation
                Color newColor = new Color(red, green, blue, color.getAlpha());
                matteImage.setRGB(x, y, newColor.getRGB());
            }
        }

        g2d.dispose();
        return matteImage;
    }




    
    /**************************************************************
     * Function to generate a damaged QR code
     ****************************************************************/
    public static BufferedImage applySideAngleEffect(BufferedImage image) {
        strQRCodevariation = "angeled";
        int width = image.getWidth();
        int height = image.getHeight();

        // Determine the new width and height based on the transformation
        // Expand the canvas to accommodate the perspective view
        int newWidth = (int) (width * 1.5); // Expand width
        int newHeight = height;

        // Create a new image with expanded canvas to accommodate the transformation
        BufferedImage transformedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = transformedImage.createGraphics();

        // Apply perspective transformation (AffineTransform)
        AffineTransform transform = new AffineTransform();
        
        // Perspective distortion values (simulating a side view)
        double shearX = 0.5;  // Horizontal shear (higher values tilt more)
        double scaleX = 0.7;  // Horizontal scale to make the QR appear "flatter"

        // Apply translation to center the image after transformation
        transform.translate((newWidth - width) / 2, 0);

        // Apply shear and scale transformation
        transform.shear(shearX, 0);  // Shear the image horizontally
        transform.scale(scaleX, 1.0);  // Scale the image horizontally (squish it)

        // Draw the original QR code with the transformation applied
        g2d.drawImage(image, transform, null);
        g2d.dispose();  // Release the resources

        return transformedImage;
    }
    
    

    /**************************************************************
     * Function to generate a damaged QR code
     ****************************************************************/
    private static BufferedImage generateDamagedQRCode(BufferedImage qrCode) {
        strQRCodevariation = "damaged";  
        int width = qrCode.getWidth();
        int height = qrCode.getHeight();

        BufferedImage damagedQR = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = damagedQR.createGraphics();
        g.drawImage(qrCode, 0, 0, null);

        // Simulate scratches
        addScratches(g, width, height);

        // Simulate missing parts (tears)
        addTears(g, width, height);

        // Simulate blurring (smudging)
        addBlurring(damagedQR);

        g.dispose();
        return damagedQR;
    }

    // Function to add scratches to the QR code
    private static void addScratches(Graphics2D g, int width, int height) {
        g.setColor(Color.WHITE);
        Random random = new Random();

        // Draw random lines (scratches) across the image
        for (int i = 0; i < 10; i++) {
            int x1 = random.nextInt(width);
            int y1 = random.nextInt(height);
            int x2 = random.nextInt(width);
            int y2 = random.nextInt(height);

            g.setStroke(new BasicStroke(5)); // Adjust stroke width for thicker or thinner scratches
            g.drawLine(x1, y1, x2, y2);
        }
    }

    // Function to add tears (missing parts) to the QR code
    private static void addTears(Graphics2D g, int width, int height) {
        g.setColor(Color.WHITE);  // White to simulate missing parts

        Random random = new Random();
        for (int i = 0; i < 3; i++) { // Add 3 tears
            int tearWidth = random.nextInt(width / 4) + width / 10;  // Random tear size
            int tearHeight = random.nextInt(height / 4) + height / 10;
            int x = random.nextInt(width - tearWidth);
            int y = random.nextInt(height - tearHeight);

            g.fillRect(x, y, tearWidth, tearHeight); // Fill rectangle to simulate a missing part
        }
    }

    // Function to add blurring/smudging to the QR code
    private static void addBlurring(BufferedImage image) {
        int radius = 5;  // Adjust the blur radius
        for (int y = radius; y < image.getHeight() - radius; y++) {
            for (int x = radius; x < image.getWidth() - radius; x++) {
                blurPixel(image, x, y, radius);
            }
        }
    }

    // Helper function to blur a pixel and its neighbors
    private static void blurPixel(BufferedImage image, int x, int y, int radius) {
        int totalRed = 0, totalGreen = 0, totalBlue = 0, totalAlpha = 0;
        int count = 0;

        // Average the pixel values in the surrounding area
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int rgb = image.getRGB(x + dx, y + dy);
                totalRed += (rgb >> 16) & 0xFF;
                totalGreen += (rgb >> 8) & 0xFF;
                totalBlue += rgb & 0xFF;
                totalAlpha += (rgb >> 24) & 0xFF;
                count++;
            }
        }

        // Set the blurred pixel
        int avgRed = totalRed / count;
        int avgGreen = totalGreen / count;
        int avgBlue = totalBlue / count;
        int avgAlpha = totalAlpha / count;

        int blurredRGB = (avgAlpha << 24) | (avgRed << 16) | (avgGreen << 8) | avgBlue;
        image.setRGB(x, y, blurredRGB);
    }



    /**************************************************************
     * Function to generate a noisy QR code
     ****************************************************************/
    private static BufferedImage generateNoisyQRCode(BufferedImage qrCode) {
        strQRCodevariation = "noisy";  
        int width = qrCode.getWidth();
        int height = qrCode.getHeight();

        BufferedImage noisyQR = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Random random = new Random();

        // Loop over each pixel and add noise
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = qrCode.getRGB(x, y);

                // Introduce noise based on the noiseLevel
                if (random.nextDouble() < 0.05) {
                    // Flip the pixel to either black or white noise
                    rgb = random.nextBoolean() ? 0xFFFFFFFF : 0xFF000000;
                }

                noisyQR.setRGB(x, y, rgb);
            }
        }

        return noisyQR;
    }



    /**************************************************************
     *  Function to generate a curved QR code (cylindrical effect)
     ****************************************************************/
    private static BufferedImage generateCurvedQRCode(BufferedImage qrCode) {
        strQRCodevariation = "curved";  
        int width = qrCode.getWidth();
        int height = qrCode.getHeight();

        BufferedImage curvedQR = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = curvedQR.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        // Loop over the pixels of the original image
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Apply cylindrical distortion
                double curveFactor = 0.05;  // Adjust the curve factor as needed
                int newX = (int) (x + curveFactor * Math.sin(Math.PI * y / height) * width / 2);

                // Ensure newX is within bounds
                if (newX >= 0 && newX < width) {
                    int rgb = qrCode.getRGB(x, y);
                    curvedQR.setRGB(newX, y, rgb);
                }
            }
        }

        g.dispose();
        return curvedQR;
    }



    /**************************************************************
     *  Function to generate partially obscured QR code
     ****************************************************************/
    private static BufferedImage generatePartiallyObscuredQRCode(BufferedImage qrCode) {
        BufferedImage newImage = new BufferedImage(qrCode.getWidth(), qrCode.getHeight(), BufferedImage.TYPE_INT_ARGB);
        strQRCodevariation = "obscure";  
        Graphics2D g = newImage.createGraphics();
        g.drawImage(qrCode, 0, 0, null);

        // Simulate partial obscuration by drawing rectangles
        g.setColor(Color.lightGray);
        g.fillRect(50, 50, 100, 100);  // Cover part of the QR code

        g.dispose();
        return newImage;
    }



    /**************************************************************
     *  Function to generate torn QR code
     ****************************************************************/
    private static BufferedImage generateTornQRCode(BufferedImage qrCode) {
        strQRCodevariation = "torn";  
        BufferedImage newImage = new BufferedImage(qrCode.getWidth(), qrCode.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = newImage.createGraphics();
        g.drawImage(qrCode, 0, 0, null);

        // Simulate tearing by filling random polygons
        g.setColor(Color.lightGray);
        int[] xPoints = {50, 150, 100};  // Example points for a tear
        int[] yPoints = {50, 50, 150};
        g.fillPolygon(xPoints, yPoints, 3);

        g.dispose();
        return newImage;
    }



    /**************************************************************
     * Function to generate scratched QR code
     ****************************************************************/
    private static BufferedImage generateScratchedQRCode(BufferedImage qrCode) {
        strQRCodevariation = "scratched";   
        BufferedImage newImage = new BufferedImage(qrCode.getWidth(), qrCode.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = newImage.createGraphics();
        g.drawImage(qrCode, 0, 0, null);

        // Simulate scratches by drawing random lines
        g.setColor(Color.GRAY);
        for (int i = 0; i < 10; i++) {
            int x1 = (int) (Math.random() * qrCode.getWidth());
            int y1 = (int) (Math.random() * qrCode.getHeight());
            int x2 = (int) (Math.random() * qrCode.getWidth());
            int y2 = (int) (Math.random() * qrCode.getHeight());
            g.setStroke(new BasicStroke(3));  // Width of the scratch
            g.drawLine(x1, y1, x2, y2);
        }
        g.dispose();
        return newImage;
    }


    
    /**************************************************************
     * Functionality to  Distored
     ****************************************************************/
    public static BufferedImage applyDistortionEffect(BufferedImage image) {
        strQRCodevariation = "distored";        
        int width = image.getWidth();
        int height = image.getHeight();

        // Create a blank image to store the distorted QR code
        BufferedImage distortedImage = new BufferedImage(width, height, image.getType());

        // Get the Graphics2D object to apply transformations
        Graphics2D g2d = distortedImage.createGraphics();

        // Define an AffineTransform for the distortion effect
        AffineTransform affineTransform = new AffineTransform();

        // Apply random distortion factors to simulate scanning the QR code from an angle
        // You can tweak these values for different types of distortion
        double shearX = 0.2; // Horizontal shear factor (simulates stretching sideways)
        double shearY = 0.1; // Vertical shear factor (simulates stretching upwards)
        double scaleX = 0.9; // Horizontal scale factor (simulates narrowing width)
        double scaleY = 0.8; // Vertical scale factor (simulates flattening height)

        // Apply the distortion: shear and scale transformation
        affineTransform.shear(shearX, shearY);  // Shear the image
        affineTransform.scale(scaleX, scaleY);  // Scale the image

        // Draw the transformed image onto the new distorted image
        g2d.drawImage(image, affineTransform, null);
        g2d.dispose();

        return distortedImage;
    }



    /**************************************************************
     * Functionality to  blurreed
     ****************************************************************/    
    public static BufferedImage applyBlurEffect(BufferedImage image) {
        strQRCodevariation = "blurred";

        // Define a stronger Gaussian blur kernel (5x5 kernel for stronger blur)
        float[] strongerBlurKernel = {
            1f / 256f, 4f / 256f, 6f / 256f, 4f / 256f, 1f / 256f,
            4f / 256f, 16f / 256f, 24f / 256f, 16f / 256f, 4f / 256f,
            6f / 256f, 24f / 256f, 36f / 256f, 24f / 256f, 6f / 256f,
            4f / 256f, 16f / 256f, 24f / 256f, 16f / 256f, 4f / 256f,
            1f / 256f, 4f / 256f, 6f / 256f, 4f / 256f, 1f / 256f
        };

        // Create a convolution operator using the Gaussian blur kernel
        Kernel kernel = new Kernel(5, 5, strongerBlurKernel);
        ConvolveOp blurOp = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);

        // Apply the blur effect multiple times for stronger blurriness
        BufferedImage blurredImage = image;
        int blurTimes = 5;  // Apply the blur effect 5 times for a strong blur

        for (int i = 0; i < blurTimes; i++) {
            // Create a new image to store the blurred result
            BufferedImage tempImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
            // Apply the convolution operation (blurring)
            blurOp.filter(blurredImage, tempImage);
            blurredImage = tempImage; // Set the new blurred image as input for the next iteration
        }
        return blurredImage;
    }




    /**************************************************************
     * Functionality to  Simulate low contrast
     ****************************************************************/
    public static BufferedImage applyLowContrast(BufferedImage image) {
        strQRCodevariation = "low_contrast";
        int width = image.getWidth();
        int height = image.getHeight();

        // Create a new image to hold the low contrast version
        BufferedImage lowContrastImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = lowContrastImage.createGraphics();

        // Loop through each pixel of the original image
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixelColor = image.getRGB(x, y);
                Color color = new Color(pixelColor, true);

                // Adjust the contrast by lightening black and darkening white
                if (isBlack(color)) {
                    // Lighten the black areas (e.g., make them dark gray)
                    Color newColor = new Color(80, 80, 80); // Dark gray
                    lowContrastImage.setRGB(x, y, newColor.getRGB());
                } else if (isWhite(color)) {
                    // Darken the white areas (e.g., make them light gray)
                    Color newColor = new Color(200, 200, 200); // Light gray
                    lowContrastImage.setRGB(x, y, newColor.getRGB());
                } else {
                    // Preserve any non-black/white colors (if any)
                    lowContrastImage.setRGB(x, y, color.getRGB());
                }
            }
        }

        g2d.dispose();
        return lowContrastImage;
    }

    // Helper function to check if a pixel is black (with some tolerance)
    private static boolean isBlack(Color color) {
        return color.getRed() < 50 && color.getGreen() < 50 && color.getBlue() < 50;
    }

    // Helper function to check if a pixel is white (with some tolerance)
    private static boolean isWhite(Color color) {
        return color.getRed() > 200 && color.getGreen() > 200 && color.getBlue() > 200;
    }




    /**************************************************************
     * Functionality to  Simulate Glossy Effect by adding noise
     ****************************************************************/
    public static BufferedImage applyGlossyEffect(BufferedImage image) {
         strQRCodevariation = "glossy";
        int width = image.getWidth();
        int height = image.getHeight();

        // Create a new image to hold the glossy effect
        BufferedImage glossyImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = glossyImage.createGraphics();

        // Draw the original QR code onto the new image
        g2d.drawImage(image, 0, 0, null);

        // Set rendering hints for smooth graphics
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Create random highlights
        Random random = new Random();
        
        // Increase the range for more highlights
        int numHighlights = random.nextInt(15) + 10; // 10 to 24 highlights

        for (int i = 0; i < numHighlights; i++) {
            // Random position and size for the highlight
            int highlightSize = random.nextInt(80) + 30; // Highlight size from 30 to 110 pixels
            int x = random.nextInt(width - highlightSize);
            int y = random.nextInt(height - highlightSize);

            // Create a radial gradient for the highlight
            float[] dist = { 0.0f, 0.4f, 1.0f };
            Color[] colors = { new Color(255, 255, 255, 200), new Color(255, 255, 255, 100), new Color(255, 255, 255, 0) }; // Bright white center to transparent edges
            RadialGradientPaint highlight = new RadialGradientPaint(
                new Point(x + highlightSize / 2, y + highlightSize / 2), 
                highlightSize / 2, dist, colors);

            // Apply the gradient paint to simulate a glossy highlight
            g2d.setPaint(highlight);
            g2d.fill(new Ellipse2D.Double(x, y, highlightSize, highlightSize));
        }

        // Add more reflection bands (diagonal lines)
        int numBands = 5;  // Increase the number of reflection bands
        for (int i = 0; i < numBands; i++) {
            int bandWidth = random.nextInt(15) + 20;  // Band width between 20 and 35 pixels
            int xStart = random.nextInt(width / 2);
            int yStart = random.nextInt(height / 2);
            
            g2d.setPaint(new Color(255, 255, 255, 60)); // Semi-transparent white color
            g2d.setStroke(new BasicStroke(bandWidth));

            // Draw a diagonal line to simulate reflection
            g2d.drawLine(xStart, yStart, width, height);
        }

        g2d.dispose();
        return glossyImage;
    }

    public static BufferedImage applyGlossyEffect1(BufferedImage image) {
        strQRCodevariation = "glossy";
        int width = image.getWidth();
        int height = image.getHeight();

        // Create a new image to hold the glossy effect
        BufferedImage glossyImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = glossyImage.createGraphics();

        // Draw the original QR code onto the new image
        g2d.drawImage(image, 0, 0, null);

        // Set rendering hints for smooth graphics
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Create random highlights
        Random random = new Random();
        int numHighlights = random.nextInt(8) + 6; // 6 to 13 highlights

        for (int i = 0; i < numHighlights; i++) {
            // Random position and size for the highlight
            int highlightSize = random.nextInt(80) + 50; // Highlight size from 50 to 130 pixels
            int x = random.nextInt(width - highlightSize);
            int y = random.nextInt(height - highlightSize);

            // Create a radial gradient for the highlight
            float[] dist = { 0.0f, 0.4f, 1.0f };
            Color[] colors = { new Color(255, 255, 255, 200), new Color(255, 255, 255, 100), new Color(255, 255, 255, 0) }; // Bright white center to transparent edges
            RadialGradientPaint highlight = new RadialGradientPaint(
                new Point(x + highlightSize / 2, y + highlightSize / 2), 
                highlightSize / 2, dist, colors);

            // Apply the gradient paint to simulate a glossy highlight
            g2d.setPaint(highlight);
            g2d.fill(new Ellipse2D.Double(x, y, highlightSize, highlightSize));
        }

        // Add subtle reflection bands (diagonal lines)
        int numBands = 3;  // Number of reflection bands
        for (int i = 0; i < numBands; i++) {
            int bandWidth = random.nextInt(15) + 20;  // Band width between 20 and 35 pixels
            int xStart = random.nextInt(width / 2);
            int yStart = random.nextInt(height / 2);
            
            g2d.setPaint(new Color(255, 255, 255, 60)); // Semi-transparent white color
            g2d.setStroke(new BasicStroke(bandWidth));

            // Draw a diagonal line to simulate reflection
            g2d.drawLine(xStart, yStart, width, height);
        }

        g2d.dispose();
        return glossyImage;
    }



    
    /**************************************************************
     * Functionality to Apply Low Lighting Effect by reducing brightness
     ****************************************************************/
    public static BufferedImage applyLowLighting(BufferedImage image) {
        strQRCodevariation = "low_lighting";
        BufferedImage output = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                Color c = new Color(image.getRGB(x, y));
                // Reduce brightness by 40%
                int red = (int) (c.getRed() * 0.6);
                int green = (int) (c.getGreen() * 0.6);
                int blue = (int) (c.getBlue() * 0.6);
                Color newColor = new Color(red, green, blue);
                output.setRGB(x, y, newColor.getRGB());
            }
        }
        return output;
    }



    /**************************************************************
     * Functionality to Apply Low Resolution Effect by resizing the image
     ****************************************************************/
    public static BufferedImage applyLowResolution(BufferedImage image) {
        strQRCodevariation = "low_resolution";
        Image tmp = image.getScaledInstance(100, 100, Image.SCALE_SMOOTH);
        BufferedImage resized = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();
        return resized;
    }
}