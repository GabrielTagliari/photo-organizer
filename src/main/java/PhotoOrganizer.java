import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;


public class PhotoOrganizer {
    private static final String SOURCE_DIRECTORY = "C:\\Users\\gabri\\Documents\\Backup";
    private static final String TARGET_DIRECTORY = "C:\\Users\\gabri\\Documents\\Target";
    private static final String NOT_MAPPED_DIRECTORY = "C:\\Users\\gabri\\Documents\\Target\\NoDateOriginal";
    private static final String DATE_FORMAT = "dd/MM/yyyy";

    private static long counter = 0L;

    public static void main(String[] args) throws FileNotFoundException {
        long startTime = System.currentTimeMillis();

        initializeFileLog();

        processImages(SOURCE_DIRECTORY);

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        System.out.println("\nTotal files processed: " + counter);
        System.out.println("\nExecution time: " + executionTime + " milliseconds");
    }

    private static void initializeFileLog() throws FileNotFoundException {
        File file = new File("photo_organizer_log.txt");
        PrintStream stream = new PrintStream(file);
        System.out.println("From now on "+ file.getAbsolutePath() +" will be your console");
        System.setOut(stream);
    }

    private static boolean isImageOrVideoFile(File file) {
        String fileName = file.getName().toLowerCase();
        return fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")
                || fileName.endsWith(".png") || fileName.endsWith(".bmp") || fileName.endsWith(".mpg")
                || fileName.endsWith(".mp4");
    }

    private static void processImages(String sourceDirectory) {
        File sourceFolder = new File(sourceDirectory);

        if (!sourceFolder.isDirectory()) {
            System.out.println("The specified path is not a directory!");
            return;
        }

        try {
            for (File sourceFile : Objects.requireNonNull(sourceFolder.listFiles())) {
                if (sourceFile.isFile() && isImageOrVideoFile(sourceFile)) {
                    processImageFile(sourceFile);
                } else if (sourceFile.isDirectory()) {
                    System.out.println("Directory: " + sourceFile.getAbsolutePath());
                    processImages(sourceFile.getAbsolutePath());
                } else {
                    System.out.println("Unmapped file: " + sourceFile.getAbsolutePath());
                }
            }
        } catch (IOException | ImageProcessingException e) {
            System.out.println("Error processing file: " + e.getMessage());
        }
    }

    private static void processImageFile(File sourceFile) throws IOException, ImageProcessingException {
        System.out.println("Processing file: " + sourceFile.getAbsolutePath());
        Path sourceFilePath = sourceFile.toPath();
        Metadata metadata;

        try (InputStream stream = new FileInputStream(sourceFile)) {
            metadata = ImageMetadataReader.readMetadata(stream);
        }

        Directory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
        Date date = null;

        if (directory != null) {
            date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
        }

        if (date != null) {
            Calendar calendar = getCalendar(date);

            String targetDirectoryPath = String.format("%s/%s/%s/%s", TARGET_DIRECTORY, calendar.get(Calendar.YEAR),
                    (calendar.get(Calendar.MONTH) + 1), calendar.get(Calendar.DATE));

            File targetDirectoryFile = new File(targetDirectoryPath);
            if (!targetDirectoryFile.exists()) {
                targetDirectoryFile.mkdirs();
            }

            Path targetFilePath = Paths.get(String.format("%s/%s", targetDirectoryPath, sourceFile.getName()));
            Files.copy(sourceFilePath, targetFilePath);
        } else {
            System.out.println("No ExifSubIFDDirectory found in metadata");

            copyToFallbackFolder(sourceFile);
        }
        counter++;
        System.out.println("==================================================");
    }

    private static void copyToFallbackFolder(File sourceFile) throws IOException {
        File notMappedDirectory = new File(NOT_MAPPED_DIRECTORY);
        if (!notMappedDirectory.exists()) {
            notMappedDirectory.mkdirs();
        }

        Path targetFilePath = Paths.get(notMappedDirectory + "/" + sourceFile.getName());
        Files.copy(sourceFile.toPath(), targetFilePath);
    }

    private static Calendar getCalendar(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        String formattedDate = formatter.format(calendar.getTime());

        System.out.println("Formatted date: " + formattedDate);
        return calendar;
    }
}
