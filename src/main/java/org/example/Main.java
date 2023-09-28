package org.example;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.List;

public class Main {


    private static final String imageSource = "src/main/resources/images";
    private static final String quoteSource = "src/main/resources/quotes/quotes.txt";
    private static final String fontSource = "src/main/resources/fonts.txt";
    private static final String destination = "src/main/resources/results";
    private static final String approved = "src/main/resources/approved";

    private static final int compressor = 4;
    private static final int resolution = 1048 * compressor;
    private static final int textSpace = (resolution/8)*7;
    private static final int fontSize = 56 * compressor;

    public static void main(String[] args) throws IOException {
        ImageProcessor processor = new ImageProcessor();
        processor.prepare(approved, imageSource, quoteSource, fontSource);
        int maxCycles = Math.min(processor.getImageFiles().size(), processor.getQuotes().size());

        for(int i = 0; i < maxCycles; i++){
            String img = processor.getFirstImageAndToss();
            StringBuilder imageName = new StringBuilder(img.hashCode() + "_");
            BufferedImage image = ImageIO.read(new File(img));
            Image scaledImage = image.getScaledInstance(resolution, resolution, Image.SCALE_DEFAULT);
            image = convertToBufferedImage(scaledImage);


            Graphics2D g = image.createGraphics();
            Font font = new Font(processor.getNextFont(), Font.PLAIN, fontSize);
            FontMetrics metrics = g.getFontMetrics(font);
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

            int negativeYOffsetMultiplier =  linesToPost.size()/2;

            for (String s : linesToPost) {
                int positionX = (image.getWidth() - metrics.stringWidth(s)) / 2;
                int positionY = (image.getHeight() - metrics.getHeight()) / 2 + metrics.getAscent() - (fontSize * negativeYOffsetMultiplier);
                AttributedString attributedText = new AttributedString(s);
                attributedText.addAttribute(TextAttribute.FONT, font);
                //attributedText.addAttribute(TextAttribute.FOREGROUND, Color.GRAY);

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

                //g.drawString(attributedText.getIterator(), positionX, positionY);
                negativeYOffsetMultiplier--;
            }

            if(interpret != null && !interpret.isEmpty()){
                int positionX = (image.getWidth() - metrics.stringWidth(interpret)) / 2;
                int positionY = (image.getHeight() - metrics.getHeight()) / 2 + metrics.getAscent() - (fontSize*(negativeYOffsetMultiplier-2));
                AttributedString attributedText = new AttributedString("- " + interpret);
                attributedText.addAttribute(TextAttribute.FONT, font);
                //attributedText.addAttribute(TextAttribute.FOREGROUND, Color.GRAY);

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
            }

            scaledImage = image.getScaledInstance(resolution/compressor, resolution/compressor, Image.SCALE_DEFAULT);
            ImageIO.write(convertToBufferedImage(scaledImage), "png", new File(destination + "/" + imageName + ".png"));
        }

        System.out.println("Done");

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