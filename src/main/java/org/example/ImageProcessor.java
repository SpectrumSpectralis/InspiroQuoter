package org.example;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class ImageProcessor {
    private LinkedList<String> imageFiles;
    private LinkedList<String> quotes;
    private LinkedList<String> fonts;

    private HashSet<String> usedImages;
    private HashSet<String> usedQuotes;
    private int currentFontSelection = 0;
    protected void loadDifferentImages(String imageSource) {
        imageFiles = new LinkedList<>();
        File folder = new File(imageSource);
        for (final File fileEntry : Objects.requireNonNull(folder.listFiles())) {
            String fullFileName = imageSource + "/" + fileEntry.getName();
            if(!usedImages.contains(Integer.toString(fullFileName.hashCode()))) imageFiles.add(imageSource + "/" + fileEntry.getName());
        }
        Collections.shuffle(imageFiles);
    }

    public void loadDifferentQuotes(String quoteSource) {
        quotes = new LinkedList<>();
        try(BufferedReader reader = new BufferedReader(new FileReader(quoteSource))){
            String line = reader.readLine();
            while(line != null){
                if(!usedQuotes.contains(Integer.toString(line.split("-")[0].hashCode()))) quotes.add(line);
                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Collections.shuffle(quotes);
    }

    public void loadDifferentFonts(String fontSource) {
        fonts = new LinkedList<>();
        try(BufferedReader reader = new BufferedReader(new FileReader(fontSource))){
                String line = reader.readLine();
                while(line != null){
                    fonts.add(line);
                    line = reader.readLine();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Collections.shuffle(fonts);
    }

    public String getNextFont(){
        String font = fonts.get(currentFontSelection);;
        currentFontSelection++;
        if(currentFontSelection >= fonts.size()){
            currentFontSelection = 0;
        }
        return font;
    }

    public String getFirstImageAndToss(){
        return imageFiles.removeFirst();
    }

    public String getFirstQuoteAndToss(){
        return quotes.removeFirst();
    }

    public LinkedList<String> getImageFiles() {
        return imageFiles;
    }

    public void setImageFiles(LinkedList<String> imageFiles) {
        this.imageFiles = imageFiles;
    }

    public LinkedList<String> getQuotes() {
        return quotes;
    }

    public void setQuotes(LinkedList<String> quotes) {
        this.quotes = quotes;
    }

    public void loadResults(String destination) {
        usedImages = new HashSet<>();
        usedQuotes = new HashSet<>();
        File folder = new File(destination);
        for (final File fileEntry : Objects.requireNonNull(folder.listFiles())) {
            String[] codes = fileEntry.getName().split("[_.]");
            usedImages.add(codes[0]);
            usedQuotes.add(codes[1].trim());
        }
    }
}
