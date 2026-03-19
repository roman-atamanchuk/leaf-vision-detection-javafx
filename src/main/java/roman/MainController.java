package roman;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainController {

    private static final int MAX_REFERENCE_COLORS = 5;

    private Image currentImage;
    private Image currentBlackWhiteImage;
    private int[][] currentMatrix;
    private List<ClusterData> currentClusters = List.of();
    private final Color[] referenceColors = new Color[MAX_REFERENCE_COLORS];
    private int referenceColorCount = 0;
    private Integer hoveredClusterNumber;

    @FXML
    private ImageView originalImageView;

    @FXML
    private Pane originalOverlayPane;

    @FXML
    private ImageView blackWhiteImageView;

    @FXML
    private Label statusLabel;

    @FXML
    private Slider brightnessSlider;

    @FXML
    private Slider saturationSlider;

    @FXML
    private Slider differenceSlider;

    @FXML
    private Slider minClusterSizeSlider;

    @FXML
    private Label brightnessValueLabel;

    @FXML
    private Label saturationValueLabel;

    @FXML
    private Label differenceValueLabel;

    @FXML
    private Label minClusterSizeValueLabel;

    @FXML
    private ColorPicker targetColorPicker;

    @FXML
    private Label referenceCountLabel;

    @FXML
    private HBox referenceColorsBox;

    @FXML
    private Label clusterCountLabel;

    @FXML
    private TextArea clusterInfoTextArea;

    @FXML
    public void initialize() {
        targetColorPicker.setValue(Color.ORANGE);
        updateSliderLabels();
        renderReferenceColors();

        brightnessSlider.valueProperty().addListener((obs, oldValue, newValue) -> {
            updateSliderLabels();
            updateBlackWhiteImage();
        });

        saturationSlider.valueProperty().addListener((obs, oldValue, newValue) -> {
            updateSliderLabels();
            updateBlackWhiteImage();
        });

        differenceSlider.valueProperty().addListener((obs, oldValue, newValue) -> {
            updateSliderLabels();
            updateBlackWhiteImage();
        });

        minClusterSizeSlider.valueProperty().addListener((obs, oldValue, newValue) -> {
            updateSliderLabels();
            updateBlackWhiteImage();
        });
    }

    @FXML
    private void onAddReferenceColor() {
        if (referenceColorCount >= MAX_REFERENCE_COLORS) {
            statusLabel.setText("You can store only 5 reference colors.");
            return;
        }

        referenceColors[referenceColorCount] = targetColorPicker.getValue();
        referenceColorCount++;
        renderReferenceColors();
        statusLabel.setText("Added reference color " + referenceColorCount + " of " + MAX_REFERENCE_COLORS);
    }

    @FXML
    private void onClearReferenceColors() {
        Arrays.fill(referenceColors, null);
        referenceColorCount = 0;
        renderReferenceColors();
        statusLabel.setText("Reference colors cleared.");
    }

    @FXML
    private void onLoadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Image");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif")
        );

        Stage stage = (Stage) originalImageView.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file == null) {
            return;
        }

        try {
            Image image = new Image(file.toURI().toString());

            if (image.isError()) {
                showError("Could not load the selected image.");
                return;
            }

            currentImage = image;
            originalImageView.setImage(image);
            updateBlackWhiteImage();
            statusLabel.setText("Loaded: " + file.getName());

        } catch (Exception exception) {
            showError("Error loading image: " + exception.getMessage());
        }
    }

    private void updateBlackWhiteImage() {
        if (currentImage == null) {
            return;
        }

        hoveredClusterNumber = null;
        currentBlackWhiteImage = convertToBlackWhite(
                currentImage,
                brightnessSlider.getValue(),
                saturationSlider.getValue(),
                (int) Math.round(differenceSlider.getValue())
        );

        currentMatrix = convertImageToMatrix(currentBlackWhiteImage);
        List<ClusterData> clusters = findClusters(currentMatrix);
        removeSmallClusters(currentMatrix, clusters, (int) Math.round(minClusterSizeSlider.getValue()));
        List<ClusterData> filteredClusters = findClusters(currentMatrix);
        currentClusters = filteredClusters;
        currentBlackWhiteImage = matrixToImage(currentMatrix);

        blackWhiteImageView.setImage(currentBlackWhiteImage);
        showClusterInfo(filteredClusters);
        drawClusterRectangles(filteredClusters);
    }

    private void updateSliderLabels() {
        brightnessValueLabel.setText(String.format("%.2f", brightnessSlider.getValue()));
        saturationValueLabel.setText(String.format("%.2f", saturationSlider.getValue()));
        differenceValueLabel.setText(String.valueOf((int) Math.round(differenceSlider.getValue())));
        minClusterSizeValueLabel.setText(String.valueOf((int) Math.round(minClusterSizeSlider.getValue())));
    }

    private void renderReferenceColors() {
        referenceColorsBox.getChildren().clear();

        for (int i = 0; i < MAX_REFERENCE_COLORS; i++) {
            Color color = referenceColors[i] == null ? Color.TRANSPARENT : referenceColors[i];
            Rectangle swatch = new Rectangle(30, 30, color);
            swatch.setArcWidth(6);
            swatch.setArcHeight(6);
            swatch.setStroke(Color.BLACK);
            swatch.setStrokeWidth(1);
            referenceColorsBox.getChildren().add(swatch);
        }

        referenceCountLabel.setText(referenceColorCount + " / " + MAX_REFERENCE_COLORS);
    }

    private Image convertToBlackWhite(Image image,
                                      double brightnessThreshold,
                                      double saturationThreshold,
                                      int differenceThreshold) {
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();

        WritableImage result = new WritableImage(width, height);
        PixelReader reader = image.getPixelReader();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = reader.getColor(x, y);

                double brightness = color.getBrightness();
                double saturation = color.getSaturation();

                if (brightness > brightnessThreshold
                        && saturation > saturationThreshold
                        && matchesReferenceColor(color, differenceThreshold)) {
                    result.getPixelWriter().setColor(x, y, Color.WHITE);
                } else {
                    result.getPixelWriter().setColor(x, y, Color.BLACK);
                }
            }
        }

        return result;
    }

    private int[][] convertImageToMatrix(Image image) {
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();

        int[][] matrix = new int[height][width];
        PixelReader reader = image.getPixelReader();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = reader.getColor(x, y);
                matrix[y][x] = color.getBrightness() > 0.5 ? 1 : 0;
            }
        }

        return matrix;
    }

    private Image matrixToImage(int[][] matrix) {
        int height = matrix.length;
        int width = matrix[0].length;
        WritableImage image = new WritableImage(width, height);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.getPixelWriter().setColor(x, y, matrix[y][x] == 1 ? Color.WHITE : Color.BLACK);
            }
        }

        return image;
    }

    private Image matrixToImageWithHighlight(int[][] matrix, ClusterData cluster, Color highlightColor) {
        int height = matrix.length;
        int width = matrix[0].length;
        WritableImage image = new WritableImage(width, height);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.getPixelWriter().setColor(x, y, matrix[y][x] == 1 ? Color.WHITE : Color.BLACK);
            }
        }

        if (cluster != null) {
            for (int[] pixel : cluster.pixels) {
                image.getPixelWriter().setColor(pixel[1], pixel[0], highlightColor);
            }
        }

        return image;
    }

    private List<ClusterData> findClusters(int[][] matrix) {
        int height = matrix.length;
        int width = matrix[0].length;
        UnionFind unionFind = new UnionFind(width * height);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (matrix[y][x] != 1) {
                    continue;
                }

                if (x + 1 < width && matrix[y][x + 1] == 1) {
                    unionFind.union(index(y, x, width), index(y, x + 1, width));
                }
                if (y + 1 < height && matrix[y + 1][x] == 1) {
                    unionFind.union(index(y, x, width), index(y + 1, x, width));
                }
            }
        }

        Map<Integer, ClusterData> clusterMap = new HashMap<>();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (matrix[y][x] != 1) {
                    continue;
                }

                int root = unionFind.find(index(y, x, width));
                clusterMap.computeIfAbsent(root, ignored -> new ClusterData(root)).addPixel(y, x);
            }
        }

        return clusterMap.values().stream()
                .sorted(Comparator.comparingInt(ClusterData::getPixelCount).reversed())
                .toList();
    }

    private void removeSmallClusters(int[][] matrix, List<ClusterData> clusters, int minClusterSize) {
        for (ClusterData cluster : clusters) {
            if (cluster.getPixelCount() >= minClusterSize) {
                continue;
            }

            for (int[] pixel : cluster.pixels) {
                matrix[pixel[0]][pixel[1]] = 0;
            }
        }
    }

    private void showClusterInfo(List<ClusterData> clusters) {
        clusterCountLabel.setText(String.valueOf(clusters.size()));

        StringBuilder builder = new StringBuilder();
        builder.append("Total clusters: ").append(clusters.size()).append("\n\n");

        for (int i = 0; i < clusters.size(); i++) {
            ClusterData cluster = clusters.get(i);
            builder.append("Cluster ").append(i + 1)
                    .append(" -> ")
                    .append(cluster.getPixelCount())
                    .append(" pixels\n");
        }

        clusterInfoTextArea.setText(builder.toString());
    }

    private void drawClusterRectangles(List<ClusterData> clusters) {
        originalOverlayPane.getChildren().clear();

        if (currentImage == null) {
            return;
        }

        double imageWidth = currentImage.getWidth();
        double imageHeight = currentImage.getHeight();
        double viewWidth = originalImageView.getFitWidth();
        double viewHeight = originalImageView.getFitHeight();
        double scale = Math.min(viewWidth / imageWidth, viewHeight / imageHeight);
        double displayedWidth = imageWidth * scale;
        double displayedHeight = imageHeight * scale;
        double offsetX = (viewWidth - displayedWidth) / 2.0;
        double offsetY = (viewHeight - displayedHeight) / 2.0;

        originalOverlayPane.setPrefWidth(viewWidth);
        originalOverlayPane.setPrefHeight(viewHeight);

        for (int i = 0; i < clusters.size(); i++) {
            ClusterData cluster = clusters.get(i);
            int clusterNumber = i + 1;
            Rectangle rectangle = new Rectangle(
                    offsetX + cluster.minX * scale,
                    offsetY + cluster.minY * scale,
                    (cluster.maxX - cluster.minX + 1) * scale,
                    (cluster.maxY - cluster.minY + 1) * scale
            );
            rectangle.setFill(Color.TRANSPARENT);
            rectangle.setStroke(Color.DODGERBLUE);
            rectangle.setStrokeWidth(2);
            rectangle.setOnMouseEntered(event -> highlightClusterOnBlackWhite(clusterNumber));
            rectangle.setOnMouseExited(event -> clearClusterHighlight());
            Tooltip.install(rectangle, new Tooltip(
                    "Cluster " + clusterNumber + "\nPixels: " + cluster.getPixelCount()
            ));
            originalOverlayPane.getChildren().add(rectangle);
        }
    }

    private void highlightClusterOnBlackWhite(int clusterNumber) {
        if (currentMatrix == null || currentClusters.isEmpty()) {
            return;
        }
        if (Integer.valueOf(clusterNumber).equals(hoveredClusterNumber)) {
            return;
        }

        hoveredClusterNumber = clusterNumber;
        ClusterData cluster = currentClusters.get(clusterNumber - 1);
        blackWhiteImageView.setImage(matrixToImageWithHighlight(currentMatrix, cluster, getHighlightColor()));
    }

    private void clearClusterHighlight() {
        if (hoveredClusterNumber == null) {
            return;
        }

        hoveredClusterNumber = null;
        blackWhiteImageView.setImage(currentBlackWhiteImage);
    }

    private int index(int y, int x, int width) {
        return y * width + x;
    }

    private boolean matchesReferenceColor(Color pixelColor, int differenceThreshold) {
        if (referenceColorCount == 0) {
            return true;
        }

        int[] pixelChannels = toRgb(pixelColor);
        int pixelDominantIndex = dominantChannelIndex(pixelChannels);
        int pixelDominantValue = pixelChannels[pixelDominantIndex];

        for (int i = 0; i < referenceColorCount; i++) {
            Color referenceColor = referenceColors[i];
            if (referenceColor == null) {
                continue;
            }

            int[] referenceChannels = toRgb(referenceColor);
            int referenceDominantIndex = dominantChannelIndex(referenceChannels);
            int referenceDominantValue = referenceChannels[referenceDominantIndex];

            if (pixelDominantIndex == referenceDominantIndex
                    && Math.abs(pixelDominantValue - referenceDominantValue) <= differenceThreshold) {
                return true;
            }
        }

        return false;
    }

    private Color getHighlightColor() {
        if (referenceColorCount > 0 && referenceColors[0] != null) {
            Color referenceColor = referenceColors[0];
            return Color.hsb(referenceColor.getHue(), Math.max(0.8, referenceColor.getSaturation()), 1.0);
        }
        return Color.ORANGE;
    }

    private int[] toRgb(Color color) {
        return new int[]{
                (int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255),
                (int) Math.round(color.getBlue() * 255)
        };
    }

    private int dominantChannelIndex(int[] rgb) {
        if (rgb[0] >= rgb[1] && rgb[0] >= rgb[2]) {
            return 0;
        }
        if (rgb[1] >= rgb[0] && rgb[1] >= rgb[2]) {
            return 1;
        }
        return 2;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("Black and White Converter");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static class ClusterData {
        private final int root;
        private final List<int[]> pixels = new ArrayList<>();
        private int minX = Integer.MAX_VALUE;
        private int maxX = Integer.MIN_VALUE;
        private int minY = Integer.MAX_VALUE;
        private int maxY = Integer.MIN_VALUE;

        private ClusterData(int root) {
            this.root = root;
        }

        private void addPixel(int y, int x) {
            pixels.add(new int[]{y, x});
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }

        private int getPixelCount() {
            return pixels.size();
        }
    }

    private static class UnionFind {
        private final int[] parent;
        private final int[] rank;

        private UnionFind(int size) {
            parent = new int[size];
            rank = new int[size];

            for (int i = 0; i < size; i++) {
                parent[i] = i;
            }
        }

        private int find(int value) {
            if (parent[value] != value) {
                parent[value] = find(parent[value]);
            }
            return parent[value];
        }

        private void union(int first, int second) {
            int rootFirst = find(first);
            int rootSecond = find(second);

            if (rootFirst == rootSecond) {
                return;
            }

            if (rank[rootFirst] < rank[rootSecond]) {
                parent[rootFirst] = rootSecond;
            } else if (rank[rootFirst] > rank[rootSecond]) {
                parent[rootSecond] = rootFirst;
            } else {
                parent[rootSecond] = rootFirst;
                rank[rootFirst]++;
            }
        }
    }
}
