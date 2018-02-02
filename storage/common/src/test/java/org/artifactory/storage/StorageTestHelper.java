package org.artifactory.storage;

import org.jfrog.common.ResourceUtils;

import java.io.BufferedReader;
import java.io.FileReader;

/**
 * @author gidis
 */
public class StorageTestHelper {

    public static int getFileNumOfLines(String filePath) {
        int lineCount = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(ResourceUtils.getResourceAsFile(filePath)))){
            while ((br.readLine()) != null) {
                lineCount++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lineCount;
    }

    public static int getKeyPositionLine(String filePath, String key) {
        int lineCount = 0;
        try (BufferedReader br = new BufferedReader(new FileReader((ResourceUtils.getResourceAsFile(filePath))))){
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith(key)) {
                    lineCount++;
                    return lineCount;
                }
                lineCount++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lineCount;
    }
}
