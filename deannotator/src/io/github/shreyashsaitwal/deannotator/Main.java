package io.github.shreyashsaitwal.deannotator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class Main {

  /**
   * Deannotator written for Rush, removes all the annotations from the App Inventor runtime component
   * source files
   *
   * Author: Kumaraswamy B G
   */

  public static final File SOURCES = new File(System.getProperty("user.dir"), "/runtime/");

  public static void main(String[] args) throws IOException {
    // this will remove all the block annotations used in source files
    deannotate(SOURCES.listFiles());
  }

  public static void deannotate(File[] files) throws IOException {
    if (files == null) {
      return;
    }
    for (File file : files) {
      if (file.isDirectory()) {
        deannotate(file.listFiles());
      } else if (file.getName().endsWith(".java")) {
        System.out.println("Transforming file " + file);
        Files.write(
            file.toPath(),
            new Parser(
                Files.readString(file.toPath())
            ).parse()
                .getBytes()
        );
      }
    }
  }
}
