/*
 *
 * Copyright 2016 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.storage.db.aql.service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.zip.ZipInputStream;

/**
 * @author gidis
 */
public class FileReader {

    public static void main(String[] args) {
        try {
            FileInputStream in1 =new FileInputStream("/Users/gidis/Downloads/specs.4.8");
            double size1 = in1.getChannel().size();
            byte[] array1=new byte[(int) size1];
            in1.read(array1,0, (int) size1);
            System.out.println("size1:"+size1);
            for (int i = 0; i < size1; i++) {
                int b1 = array1[i];
                System.out.println(b1);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
