package org.example;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.List;

public class Main {

    private static final String inspiroFolderName = "Images For Inspiration";
    private static final String approvedFolderName = "approved";
    private static final String rawImagesFolderName = "rawImages";
    private static final String resultsFolderName = "results";
    private static final String quotesFolderName = "quotes";
    private static final String refinedQuotesFileName = "refinedQuotes.txt";
    private static final String rawQuotesFileName = "rawQuotes.txt";
    private static final String rawQuotesMetaFileName = "rawQuotesMetaFileName.txt";
    private static final String fontsFileName = "fonts.txt";
    private static final String desktopPath = System.getProperty("user.home") + File.separator +"Desktop";
    private static final String imageSource = desktopPath+"/"+inspiroFolderName+"/"+rawImagesFolderName;
    private static final String quoteFolderSource = desktopPath+"/"+inspiroFolderName+"/"+quotesFolderName;
    private static final String refinedQuoteSource = quoteFolderSource + "/" + refinedQuotesFileName;
    private static final String rawQuoteSource = quoteFolderSource + "/" + rawQuotesFileName;
    private static final String rawquoteMetafile = quoteFolderSource + "/" + rawQuotesMetaFileName;
    private static final String fontSource = quoteFolderSource+"/"+fontsFileName;
    private static final String destinationFolderSource = desktopPath+"/"+inspiroFolderName+"/"+resultsFolderName;
    private static final String approvedFolderSource = desktopPath+"/"+inspiroFolderName+"/"+approvedFolderName;
    private static final boolean debugMode = false;

    private static final int resolution = 1048;
    private static final int textSpace = (resolution/8)*7;
    private static final int fontSize = 56;

    public static void main(String[] args) throws IOException {
        if(!prepareFolders()) {
            System.out.println("Program not operational!");
            return;
        }
        
        ImageProcessor processor = new ImageProcessor(imageSource, rawQuoteSource, refinedQuoteSource, fontSource, approvedFolderSource, rawquoteMetafile);
        int maxCycles = Math.min(processor.getImageFiles().size(), processor.getQuotes().size());

        //Creates one Image per Cycle. Max Cycles = max number of images / quotes available
        for(int i = 0; i < maxCycles; i++){
            String img = processor.getFirstImageAndToss();
            StringBuilder imageName = new StringBuilder(img.hashCode() + "_");
            BufferedImage image = ImageIO.read(new File(img));
            Image scaledImage = image.getScaledInstance(resolution, resolution, Image.SCALE_DEFAULT);
            image = convertToBufferedImage(scaledImage);

            //Takes the next available line from the list of available quotes. Removes that quoote from said List.
            //Splits the quote via '-' and rebuilds it into the finishedText and interpret Variables.
            String text  = processor.getFirstQuoteAndToss();
            String interpret = null;
            String[] splittedLine = text.split("-");
            StringBuilder textBuilder = new StringBuilder();
            if (splittedLine.length > 2) {
                for (int j = 0; j < splittedLine.length; j++) {
                    if(splittedLine.length == j+1){
                        interpret = splittedLine[j];
                    }else{
                        textBuilder.append(splittedLine[j]);
                        if(splittedLine.length != j+2){
                            textBuilder.append("; ");
                        }
                    }
                }
            }else{
                interpret = splittedLine[1];
                textBuilder.append(splittedLine[0]);
            }
            String finishedText = textBuilder.toString();

            //Sets the Font for the image and calculates the necessary amount of lines needed to fit
            //the quote inside the image.
            Graphics2D g = image.createGraphics();
            Font font = new Font(processor.getNextFont(), Font.PLAIN, fontSize);
            FontMetrics metrics = g.getFontMetrics(font);
            double avgLines = Math.ceil(((double) metrics.stringWidth(finishedText))/((double) textSpace));
            double avgTextLength = Math.floor(((double) metrics.stringWidth(finishedText))/avgLines);

            List<String> linesToPost = new ArrayList<>();
            String[] textSplitted = finishedText.trim().split("\\s+");
            imageName.append(finishedText.hashCode());
            int k = 0;
            while(k < textSplitted.length){
                StringBuilder line = new StringBuilder();
                while(k < textSplitted.length && (avgTextLength > metrics.stringWidth(line+textSplitted[k]))) {
                    line.append(textSplitted[k]);
                    k++;
                    if(k < textSplitted.length && avgTextLength > (line+textSplitted[k]).length()) line.append(" ");
                }
                linesToPost.add(line.toString());
            }

            //Adds the author into the mix
            int negativeYOffsetMultiplier =  linesToPost.size()/2;
            linesToPost.add("- "+interpret);
            if(debugMode) linesToPost.add("Font: " + font.getName() + " | Size: " + fontSize);

            //Prints the Quote and author onto the image
            for (int j = 0; j < linesToPost.size(); j++) {
                String s = linesToPost.get(j);
                int positionX = (image.getWidth() - metrics.stringWidth(s)) / 2;
                int positionY = (image.getHeight() - metrics.getHeight()) / 2 + metrics.getAscent() - (fontSize * negativeYOffsetMultiplier);
                AttributedString attributedText = new AttributedString(s);
                attributedText.addAttribute(TextAttribute.FONT, font);

                TextLayout textLayout = new TextLayout(attributedText.getIterator(), g.getFontMetrics().getFontRenderContext());
                Shape shape = textLayout.getOutline(AffineTransform.getTranslateInstance(positionX, positionY));
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                        RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                g.setColor(Color.BLACK);
                g.setStroke(new BasicStroke(font.getSize2D() / 8.0f));
                g.draw(shape);
                g.setColor(Color.LIGHT_GRAY);
                g.fill(shape);


                if(j+2 == linesToPost.size()){
                    negativeYOffsetMultiplier-=3;
                }else{
                    negativeYOffsetMultiplier--;
                }
            }

            //Downscales the image (if necessary) and saves it
            scaledImage = image.getScaledInstance(resolution, resolution, Image.SCALE_DEFAULT);

            ImageIO.write(convertToBufferedImage(scaledImage), "png", new File(destinationFolderSource + "/" + imageName + ".png"));
        }

        try(BufferedWriter writer = new BufferedWriter(new FileWriter(rawquoteMetafile))){
            writer.write(Long.toString(new File(rawQuoteSource).length()));
        }

        System.out.println("Done");

    }

    private static boolean prepareFolders() throws IOException {
        boolean operational = true;

        Path baseDir = Paths.get(desktopPath + "/" + inspiroFolderName);
        if(!Files.exists(baseDir)){
            operational = false;
            Files.createDirectory(baseDir);
        }

        Path rawimgDir = Paths.get(imageSource);
        if(!Files.exists(rawimgDir)){
            operational = false;
            Files.createDirectory(rawimgDir);
        }

        Path resultImgDir = Paths.get(destinationFolderSource);
        if(!Files.exists(resultImgDir)){
            Files.createDirectory(resultImgDir);
        }

        Path approvedImgDir = Paths.get(approvedFolderSource);
        if(!Files.exists(approvedImgDir)){
            Files.createDirectory(approvedImgDir);
        }

        Path quotesDir = Paths.get(quoteFolderSource);
        if(!Files.exists(quotesDir)){
            operational = false;
            Files.createDirectory(quotesDir);
        }

        Path rawQuotes = Paths.get(quoteFolderSource + "/" + rawQuotesFileName);
        Path rawQuotesMetaFile = Paths.get(quoteFolderSource + "/" + rawQuotesMetaFileName);
        if(!Files.exists(rawQuotes)){
            Files.createFile(rawQuotes);
            Files.createFile(rawQuotesMetaFile);
            operational = false;
        }else{
            if(!Files.exists(rawQuotesMetaFile)){
                Files.createFile(rawQuotesMetaFile);
            }
        }

        Path fonts = Paths.get(quoteFolderSource + "/" + fontsFileName);
        if(!Files.exists(fonts)){
            operational = false;
            Files.createFile(fonts);
        }

        return operational;
    }

    public static BufferedImage convertToBufferedImage(Image img) {

        if (img instanceof BufferedImage) {
            return (BufferedImage) img;
        }

        // Create a buffered image with transparency
        BufferedImage bi = new BufferedImage(
                img.getWidth(null), img.getHeight(null),
                BufferedImage.TYPE_INT_ARGB);

        Graphics2D graphics2D = bi.createGraphics();
        graphics2D.drawImage(img, 0, 0, null);
        graphics2D.dispose();

        return bi;
    }

}