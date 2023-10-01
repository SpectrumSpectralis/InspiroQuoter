package org.example;

import java.io.*;
import java.util.*;

public class ImageProcessor {
    private final String imageSource;
    private final String rawQuoteSource;
    private final String refinedQuoteSource;
    private final String fontSource;
    private final String approvedDestination;
    private final String rawQuoteMetaDataSource;

    public ImageProcessor(String imageSource, String rawQuoteSource, String refinedQuoteSource, String fontSource, String approvedDestination, String rawQuoteMetaDataSource) {
        this.rawQuoteMetaDataSource = rawQuoteMetaDataSource;
        this.imageSource = imageSource;
        this.rawQuoteSource = rawQuoteSource;
        this.refinedQuoteSource = refinedQuoteSource;
        this.fontSource = fontSource;
        this.approvedDestination = approvedDestination;
        this.prepare();

    }

    private LinkedList<String> imageFiles;
    private LinkedList<String> quotes;
    private LinkedList<String> fonts;

    private HashSet<String> usedImages;
    private HashSet<String> usedQuotes;
    private int currentFontSelection = 0;
    protected void loadDifferentImages() {
        imageFiles = new LinkedList<>();
        File folder = new File(imageSource);
        for (final File fileEntry : Objects.requireNonNull(folder.listFiles())) {
            String fullFileName = imageSource + "/" + fileEntry.getName();
            if(!usedImages.contains(Integer.toString(fullFileName.hashCode()))) imageFiles.add(imageSource + "/" + fileEntry.getName());
        }
        Collections.shuffle(imageFiles);
    }

    public void loadDifferentQuotes() {
        quotes = new LinkedList<>();
        try(BufferedReader reader = new BufferedReader(new FileReader(refinedQuoteSource))){
            String line = reader.readLine();
            while(line != null){
                quotes.add(line);
                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Collections.shuffle(quotes);
    }

    public void loadDifferentFonts() {
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
        if(fonts.isEmpty()) throw new IllegalArgumentException("No fonts present!");
    }

    public String getNextFont(){
        String font = fonts.get(currentFontSelection);
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

    public LinkedList<String> getQuotes() {
        return quotes;
    }

    public void loadResults() {
        usedImages = new HashSet<>();
        usedQuotes = new HashSet<>();
        File folder = new File(approvedDestination);
        for (final File fileEntry : Objects.requireNonNull(folder.listFiles())) {
            String[] codes = fileEntry.getName().split("[_.]");
            usedImages.add(codes[0]);
            usedQuotes.add(codes[1].trim());
        }
    }

    private void prepare() {
        loadResults();
        cleanUpImages();
        if(hasBaseQuotesChanged() || refinedQuotesDoesntExist()){
            createQuotes();
        }
        loadDifferentImages();
        loadDifferentQuotes();
        loadDifferentFonts();
    }

    private boolean refinedQuotesDoesntExist() {
        return new File(refinedQuoteSource).exists();
    }

    private boolean hasBaseQuotesChanged() {
        if(rawQuoteMetaDataSource == null) return true;
        try(BufferedReader reader = new BufferedReader(new FileReader(rawQuoteMetaDataSource))) {
            String s = reader.readLine();
            if(s != null && !s.isEmpty()){
                return Long.parseLong(s) != new File(rawQuoteSource).length();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    /**
     * Cleans up
     */
    public void cleanUpImages(){
        File folder = new File(imageSource);
        for (final File fileEntry : Objects.requireNonNull(folder.listFiles())) {
            String fullFileName = imageSource + "/" + fileEntry.getName();
            if(usedImages.contains(Integer.toString(fullFileName.hashCode()))) {
                if(!fileEntry.delete()){
                    throw new RuntimeException("File " + fileEntry.getName() + " could not be deleted!");
                }
            }
        }
    }

    public void createQuotes(){
        List<String> rawQuotes = new ArrayList<>();
        try(BufferedReader reader = new BufferedReader(new FileReader(rawQuoteSource))){
            String line = reader.readLine();
            while(line != null){
                if(line.charAt(0) != '"'){
                    line = reader.readLine();
                    continue;
                }
                line = line.replace('—', '-');
                line = line.replace('―', '-');
                if(line.split("-").length <= 1){
                    line = reader.readLine();
                    continue;
                }
                if(!usedQuotes.contains(Integer.toString(line.split("-")[0].hashCode()))) rawQuotes.add(line);
                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try(BufferedWriter writer = new BufferedWriter(new FileWriter(this.refinedQuoteSource))){
            for(String s : rawQuotes){
                writer.write(s + "\n");
            }
        }catch (IOException e) {
            System.out.println("No File Found");
        }
    }
}
