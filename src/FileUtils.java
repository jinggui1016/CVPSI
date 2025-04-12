import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {
    public static List<String> readFile(String filePath){
        List<String> data = new ArrayList<>();

        BufferedReader reader = null;
        String line;

        try {
            reader = new BufferedReader(new FileReader(filePath));
            reader.readLine();

            while ((line = reader.readLine()) != null){
                data.add(line);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }finally {
            try {
//                if (reader != null) {
//                    reader.close();
//                }
                reader.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return data;
    }



    public static void storeFile(String filePath, List<String> result){
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(filePath);

            for (String s : result){
                writer.println(s);
            }
            writer.flush();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }finally {
            writer.close();
        }
    }
}
