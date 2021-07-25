package com.jojo.chapternameutil;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class ChapterNameUtil {

  static final Path opPath = Paths.get("output.txt");
  static final Path inPath = Paths.get("input.txt");

  public static void main(String[] args) throws IOException, InterruptedException {
    try (BufferedWriter bw =
        Files.newBufferedWriter(opPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
      final boolean getChapters = false;

      if (getChapters) getChapters(bw);
      else addNumbersToFile(bw);
    }
  }

  private static void addNumbersToFile(BufferedWriter bw) throws IOException {
    final AtomicInteger i = new AtomicInteger(1);

    try (var fileStream = Files.lines(inPath)) {
      fileStream.map(l -> i.getAndIncrement() + ":" + l).forEach(s -> write(bw, s));
    }
  }

  private static void getChapters(BufferedWriter bw) throws InterruptedException {
    final CountDownLatch latch = new CountDownLatch(1);

    final String url = "https://en.wikipedia.org/wiki/List_of_One_Piece_chapters_(595%E2%80%93806)";

    // output file

    Mono.fromFuture(
            () ->
                HttpClient.newHttpClient()
                    .sendAsync(
                        HttpRequest.newBuilder(URI.create(url)).GET().build(),
                        HttpResponse.BodyHandlers.ofLines()))
        .map(HttpResponse::body)
        .flatMapMany(Flux::fromStream)
        .filter(l -> l.contains("title=\"Lesson"))
        .map(l -> l.split("title=")[1].split("\"")[1])
        .map(
            l ->
                l.replace("Lesson ", "")
                    .replace("&#39;", "'")
                    .replace("&quot;", "\"")
                    .replace(": ", ":"))
        .map(l -> l.replace(" (page does not exist)", ""))
        .doOnTerminate(latch::countDown)
        .subscribe(s -> write(bw, s));
    latch.await();
  }

  private static void write(BufferedWriter bw, String string) {
    try {
      bw.write(string);
      bw.newLine();
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
