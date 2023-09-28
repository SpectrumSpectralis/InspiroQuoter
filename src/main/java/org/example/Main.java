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

    private static int compressor = 4;
    private static int resolution = 1048 * compressor;
    private static int textSpace = (resolution/8)*7;

    private static int fontSize = 56 * compressor;

    public static void main(String[] args) throws IOException {
        ImageProcessor processor = new ImageProcessor();
        processor.loadResults(destination);
        processor.loadDifferentImages(imageSource);
        processor.loadDifferentQuotes(quoteSource);
        processor.loadDifferentFonts(fontSource);
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
            String text = processor.getFirstQuoteAndToss();
            String interpret = null;
            if(text.split("-").length > 1) {
                interpret = text.split("-")[1];
                text = text.split("-")[0];
            }

            double avgLines = Math.ceil(((double) metrics.stringWidth(text))/((double) textSpace));
            double avgTextLength = Math.floor(((double) metrics.stringWidth(text))/avgLines);


            List<String> linesToPost = new ArrayList<>();
            String[] textSplitted = text.trim().split("\\s+");
            imageName.append(text.hashCode());
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

            for (int j = 0; j < linesToPost.size(); j++) {
                int positionX = (image.getWidth() - metrics.stringWidth(linesToPost.get(j))) / 2;
                int positionY = (image.getHeight() - metrics.getHeight()) / 2 + metrics.getAscent() - (fontSize*negativeYOffsetMultiplier);
                AttributedString attributedText = new AttributedString(linesToPost.get(j));
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
                AttributedString attributedText = new AttributedString(interpret);
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