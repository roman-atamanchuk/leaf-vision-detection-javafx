# Image To Black And White

A JavaFX desktop app that finds coloured objects in a photo (for example, autumn
leaves on grass), counts them, draws a box around each one and animates a short
route that visits them all.

## Running

Requires JDK 21 and Maven.

```
mvn javafx:run          # start the app
mvn test                # run the unit tests
```

In IntelliJ, run `roman.Main`. Running `roman.App` directly fails with
"JavaFX runtime components are missing".

Supported image formats are PNG, JPEG, BMP and GIF. WebP is not supported by
JavaFX; convert such files first (`sips -s format jpeg in.webp --out out.jpg`).

## How it works

Every time an image is loaded or a control changes, the whole pipeline reruns:

1. **Colour mask.** Each pixel becomes white (kept) or black (dropped). A pixel
   is kept only if:
   - its brightness is above the **Brightness Threshold** (default 0.45),
   - its saturation is above the **Saturation Threshold** (default 0.15), and
   - it matches one of up to 5 **Reference Colours**: its strongest RGB channel
     is the same as the reference colour's, and that channel's value is within
     the **Difference Threshold** (default 80). With no reference colours,
     every pixel passes this test.
2. **Find clusters.** White pixels that touch up, down, left or right (not
   diagonally) are grouped into one cluster.
3. **Filter by size.** Clusters smaller than **Min Cluster Size** (noise) or
   larger than **Max Cluster Size** (usually background) are erased from the
   mask, and the clusters are found again.
4. **Number and display.** Clusters are numbered from largest to smallest. The
   original image shows a blue box around each one; the right-hand image shows
   the mask; the side panel lists each cluster's pixel count.

### Controls

| Control | What it does |
|---|---|
| Hover over a box | Highlights that cluster on the black-and-white image |
| Randomly Colour Clusters | Paints each cluster a random colour |
| Choose Start Cluster, then click a box | Sets the route's start (orange box) |
| Animate TSP Path | Draws the route in yellow over 5 seconds |

## Algorithms

In the complexities below, **N** is the number of pixels, **K** the number of
clusters and **R** the number of reference colours (at most 5).

### Thresholding with dominant-channel colour matching — `ColorMatcher`

Each pixel is tested against brightness, saturation and the reference colours.
Instead of a full colour distance, the match compares only the *dominant*
channel (the largest of R, G, B): a pixel and a reference colour match when the
same channel is dominant and its values differ by at most the threshold. This is
cheap and groups colours by hue family (red-ish, green-ish, blue-ish).

Time: **O(N · R)**.

### Connected-component labelling with union-find — `ClusterAnalyzer.findClusters`

1. Scan every white pixel and `union` it with its right and lower neighbours if
   they are white. Checking only those two directions still covers every
   4-connected edge exactly once.
2. Scan again and `find` each white pixel's root. Pixels with the same root form
   one cluster.
3. Sort clusters by pixel count, largest first, and number them 1..K.

Time: **O(N · α(N) + K log K)**, where α is the inverse Ackermann function
(effectively a constant), so this is practically linear in the image size.

### Size filtering — `removeSmallClusters`, `removeLargeClusters`

Every pixel of a cluster outside the size limits is set back to 0 in the matrix.
Time: **O(N)**.

### Nearest-neighbour route (TSP heuristic) — `ClusterAnalyzer.buildNearestNeighbourRoute`

Visiting every cluster by the shortest possible route is the Travelling Salesman
Problem, which is NP-hard. The app uses the greedy *nearest-neighbour* heuristic:
start at the chosen cluster, repeatedly move to the closest unvisited cluster,
and stop when none remain. Distance is the Euclidean distance between the
centres of the clusters' bounding boxes.

Time: **O(K²)**. The route is fast to build but not guaranteed to be the
shortest.

### Animation — JavaFX `Timeline`

The route is split into K − 1 segments, each shown by one `KeyFrame`, spread
evenly over 5 seconds.

## Data structures

| Structure | Where | Purpose |
|---|---|---|
| `int[][]` matrix | `MainController` | Black/white mask: `1` = kept pixel, `0` = dropped |
| Union-find (`int[] parent`, `int[] rank`) | `UnionFind` | Disjoint sets of pixels, with **path compression** and **union by rank** |
| `HashMap<Integer, ClusterInfo>` | `ClusterAnalyzer` | Maps a union-find root to its cluster while grouping pixels |
| `List<int[]>` of `{y, x}` | `ClusterInfo` | A cluster's pixels, used for erasing, colouring and highlighting |
| Bounding box (`minX`, `maxX`, `minY`, `maxY`) | `ClusterInfo` | Updated per pixel; gives the drawn box and the centre used for distances |
| `Color[5]` fixed array | `MainController` | Up to 5 reference colours |
| `List<Integer>` | `ClusterAnalyzer` | The route as cluster numbers, and the remaining unvisited clusters |
| `WritableImage` | `MainController` | Pixel buffers for the mask, random colours and highlight views |

Pixels are stored in the union-find as a single index `y * width + x`, so a
2-D image fits into flat arrays.

## Project structure

```
src/main/java/roman/
  Main.java              entry point (launches App)
  App.java               JavaFX Application, loads main.fxml
  MainController.java    UI, image processing and drawing
  ClusterAnalyzer.java   cluster finding, size filters, route
  ClusterInfo.java       one cluster: pixels, bounds, centre
  ColorMatcher.java      reference-colour matching
  UnionFind.java         disjoint-set structure
src/main/resources/roman/main.fxml   screen layout
src/test/java/roman/
  algorithm/  image/  model/         JUnit 5 tests
  benchmark/                         JMH benchmarks (run BenchmarkRunner)
```
