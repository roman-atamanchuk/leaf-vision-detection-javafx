package roman;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class MainController {

    private static final int MAX_REFERENCE_COLORS = 5;
    private static final double PATH_ANIMATION_SECONDS = 5.0;

    private final ClusterAnalyzer clusterAnalyzer = new ClusterAnalyzer();

    private Image currentImage;
    private Image currentBlackWhiteImage;
    private Image currentRandomClusterImage;
    private int[][] currentMatrix;
    private List<ClusterInfo> currentClusters = List.of();
    private final Color[] referenceColors = new Color[MAX_REFERENCE_COLORS];
    private int referenceColorCount = 0;
    private Integer hoveredClusterNumber;
    private Integer startClusterNumber;
    private boolean randomClusterMode;
    private boolean chooseStartMode;
    private Timeline pathAnimation;
    private List<Integer> animatedRoute = List.of();
    private int visibleRouteSegments;
    private Integer activeAnimatedClusterNumber;

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
    private Slider maxClusterSizeSlider;

    @FXML
    private Label brightnessValueLabel;

    @FXML
    private Label saturationValueLabel;

    @FXML
    private Label differenceValueLabel;

    @FXML
    private Label minClusterSizeValueLabel;

    @FXML
    private Label maxClusterSizeValueLabel;

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

        maxClusterSizeSlider.valueProperty().addListener((obs, oldValue, newValue) -> {
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
        updateBlackWhiteImage();
        statusLabel.setText("Added reference color " + referenceColorCount + " of " + MAX_REFERENCE_COLORS);
    }

    @FXML
    private void onClearReferenceColors() {
        Arrays.fill(referenceColors, null);
        referenceColorCount = 0;
        renderReferenceColors();
        updateBlackWhiteImage();
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
            randomClusterMode = false;
            startClusterNumber = null;
            chooseStartMode = false;
            stopPathAnimation();
            updateBlackWhiteImage();
            statusLabel.setText("Loaded: " + file.getName());

        } catch (Exception exception) {
            showError("Error loading image: " + exception.getMessage());
        }
    }

    @FXML
    private void onExitApplication() {
        Stage stage = (Stage) originalImageView.getScene().getWindow();
        stage.close();
    }

    private void updateBlackWhiteImage() {
        if (currentImage == null) {
            return;
        }

        stopPathAnimation();
        hoveredClusterNumber = null;
        currentBlackWhiteImage = convertToBlackWhite(
                currentImage,
                brightnessSlider.getValue(),
                saturationSlider.getValue(),
                (int) Math.round(differenceSlider.getValue())
        );

        currentMatrix = convertImageToMatrix(currentBlackWhiteImage);
        List<ClusterInfo> clusters = clusterAnalyzer.findClusters(currentMatrix);
        clusterAnalyzer.removeSmallClusters(currentMatrix, clusters, (int) Math.round(minClusterSizeSlider.getValue()));
        clusterAnalyzer.removeLargeClusters(currentMatrix, clusters, (int) Math.round(maxClusterSizeSlider.getValue()));
        List<ClusterInfo> filteredClusters = clusterAnalyzer.findClusters(currentMatrix);
        currentClusters = filteredClusters;
        if (startClusterNumber != null && startClusterNumber > currentClusters.size()) {
            startClusterNumber = null;
        }
        currentBlackWhiteImage = matrixToImage(currentMatrix);
        currentRandomClusterImage = createRandomClusterImage(currentMatrix, filteredClusters);

        blackWhiteImageView.setImage(getBaseProcessedImage());
        showClusterInfo(filteredClusters);
        drawClusterRectangles(filteredClusters);
    }

    @FXML
    private void onRandomlyColorClusters() {
        if (currentMatrix == null || currentClusters.isEmpty()) {
            statusLabel.setText("Load and process an image first.");
            return;
        }

        randomClusterMode = true;
        currentRandomClusterImage = createRandomClusterImage(currentMatrix, currentClusters);
        blackWhiteImageView.setImage(currentRandomClusterImage);
        statusLabel.setText("Applied random colours to the clusters.");
    }

    @FXML
    private void onChooseStartCluster() {
        if (currentClusters.isEmpty()) {
            statusLabel.setText("Load and process an image first.");
            return;
        }

        chooseStartMode = true;
        statusLabel.setText("Click a blue rectangle on the original image to choose the TSP start cluster.");
    }

    @FXML
    private void onAnimatePath() {
        if (currentClusters.size() < 2) {
            statusLabel.setText("Need at least two clusters to animate a path.");
            return;
        }

        if (startClusterNumber == null) {
            startClusterNumber = 1;
        }

        animatedRoute = clusterAnalyzer.buildNearestNeighbourRoute(currentClusters, startClusterNumber);
        visibleRouteSegments = 0;
        activeAnimatedClusterNumber = startClusterNumber;
        drawClusterRectangles(currentClusters);

        double secondsPerStep = PATH_ANIMATION_SECONDS / Math.max(1, animatedRoute.size() - 1);
        pathAnimation = new Timeline();

        for (int segment = 1; segment < animatedRoute.size(); segment++) {
            final int segmentIndex = segment;
            pathAnimation.getKeyFrames().add(new KeyFrame(Duration.seconds(secondsPerStep * segment), event -> {
                visibleRouteSegments = segmentIndex;
                activeAnimatedClusterNumber = animatedRoute.get(segmentIndex);
                drawClusterRectangles(currentClusters);
            }));
        }

        pathAnimation.setOnFinished(event -> {
            activeAnimatedClusterNumber = null;
            chooseStartMode = false;
            drawClusterRectangles(currentClusters);
            statusLabel.setText("TSP animation complete.");
        });
        pathAnimation.playFromStart();
        statusLabel.setText("Animating TSP path from cluster " + startClusterNumber + ".");
    }

    private void updateSliderLabels() {
        brightnessValueLabel.setText(String.format("%.2f", brightnessSlider.getValue()));
        saturationValueLabel.setText(String.format("%.2f", saturationSlider.getValue()));
        differenceValueLabel.setText(String.valueOf((int) Math.round(differenceSlider.getValue())));
        minClusterSizeValueLabel.setText(String.valueOf((int) Math.round(minClusterSizeSlider.getValue())));
        maxClusterSizeValueLabel.setText(String.valueOf((int) Math.round(maxClusterSizeSlider.getValue())));
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
                        && ColorMatcher.matchesReferenceColor(color, referenceColors, referenceColorCount, differenceThreshold)) {
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

    private Image createRandomClusterImage(int[][] matrix, List<ClusterInfo> clusters) {
        int height = matrix.length;
        int width = matrix[0].length;
        WritableImage image = new WritableImage(width, height);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.getPixelWriter().setColor(x, y, matrix[y][x] == 1 ? Color.WHITE : Color.BLACK);
            }
        }

        Random random = new Random();
        for (ClusterInfo cluster : clusters) {
            Color color = Color.hsb(random.nextDouble() * 360.0, 0.8, 1.0);
            for (int[] pixel : cluster.pixels()) {
                image.getPixelWriter().setColor(pixel[1], pixel[0], color);
            }
        }

        return image;
    }

    private Image matrixToImageWithHighlight(int[][] matrix, ClusterInfo cluster, Color highlightColor) {
        int height = matrix.length;
        int width = matrix[0].length;
        WritableImage image = new WritableImage(width, height);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.getPixelWriter().setColor(x, y, matrix[y][x] == 1 ? Color.WHITE : Color.BLACK);
            }
        }

        if (cluster != null) {
            for (int[] pixel : cluster.pixels()) {
                image.getPixelWriter().setColor(pixel[1], pixel[0], highlightColor);
            }
        }

        return image;
    }

    private void showClusterInfo(List<ClusterInfo> clusters) {
        clusterCountLabel.setText(String.valueOf(clusters.size()));

        StringBuilder builder = new StringBuilder();
        builder.append("Total clusters: ").append(clusters.size()).append("\n\n");
        builder.append("Start cluster: ").append(startClusterNumber == null ? "-" : startClusterNumber).append("\n\n");
        builder.append("Min cluster size: ").append((int) Math.round(minClusterSizeSlider.getValue())).append("\n");
        builder.append("Max cluster size: ").append((int) Math.round(maxClusterSizeSlider.getValue())).append("\n\n");

        for (int i = 0; i < clusters.size(); i++) {
            ClusterInfo cluster = clusters.get(i);
            builder.append("Cluster ").append(i + 1)
                    .append(" -> ")
                    .append(cluster.pixelCount())
                    .append(" pixels\n");
        }

        clusterInfoTextArea.setText(builder.toString());
    }

    private void drawClusterRectangles(List<ClusterInfo> clusters) {
        originalOverlayPane.getChildren().clear();

        if (currentImage == null) {
            return;
        }

        double imageWidth = currentImage.getWidth();
        double imageHeight = currentImage.getHeight();
        double viewWidth = originalImageView.getBoundsInLocal().getWidth();
        double viewHeight = originalImageView.getBoundsInLocal().getHeight();
        double scale = Math.min(viewWidth / imageWidth, viewHeight / imageHeight);
        double displayedWidth = imageWidth * scale;
        double displayedHeight = imageHeight * scale;
        double offsetX = (viewWidth - displayedWidth) / 2.0;
        double offsetY = (viewHeight - displayedHeight) / 2.0;

        originalOverlayPane.setPrefWidth(displayedWidth);
        originalOverlayPane.setPrefHeight(displayedHeight);
        originalOverlayPane.setMaxWidth(displayedWidth);
        originalOverlayPane.setMaxHeight(displayedHeight);
        originalOverlayPane.setTranslateX(offsetX);
        originalOverlayPane.setTranslateY(offsetY);

        drawAnimatedPath(scale);

        for (int i = 0; i < clusters.size(); i++) {
            ClusterInfo cluster = clusters.get(i);
            int clusterNumber = i + 1;
            Rectangle rectangle = new Rectangle(
                    cluster.minX() * scale,
                    cluster.minY() * scale,
                    (cluster.maxX() - cluster.minX() + 1) * scale,
                    (cluster.maxY() - cluster.minY() + 1) * scale
            );
            rectangle.setFill(Color.TRANSPARENT);
            rectangle.setStroke(determineStrokeColor(clusterNumber));
            rectangle.setStrokeWidth(clusterNumber == startClusterNumberValue() ? 3 : 2);
            rectangle.setOnMouseEntered(event -> highlightClusterOnBlackWhite(clusterNumber));
            rectangle.setOnMouseExited(event -> clearClusterHighlight());
            rectangle.setOnMouseClicked(event -> selectStartCluster(clusterNumber));
            Tooltip.install(rectangle, new Tooltip(
                    "Cluster " + clusterNumber + "\nPixels: " + cluster.pixelCount()
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
        ClusterInfo cluster = currentClusters.get(clusterNumber - 1);
        blackWhiteImageView.setImage(matrixToImageWithHighlight(currentMatrix, cluster, getHighlightColor()));
    }

    private void clearClusterHighlight() {
        if (hoveredClusterNumber == null) {
            return;
        }

        hoveredClusterNumber = null;
        blackWhiteImageView.setImage(getBaseProcessedImage());
    }

    private Image getBaseProcessedImage() {
        return randomClusterMode && currentRandomClusterImage != null ? currentRandomClusterImage : currentBlackWhiteImage;
    }

    private void selectStartCluster(int clusterNumber) {
        if (!chooseStartMode) {
            return;
        }

        startClusterNumber = clusterNumber;
        chooseStartMode = false;
        stopPathAnimation();
        drawClusterRectangles(currentClusters);
        showClusterInfo(currentClusters);
        statusLabel.setText("Start cluster set to " + clusterNumber + ".");
    }

    private void drawAnimatedPath(double scale) {
        if (animatedRoute.isEmpty() || visibleRouteSegments == 0) {
            return;
        }

        for (int i = 1; i <= visibleRouteSegments && i < animatedRoute.size(); i++) {
            ClusterInfo previous = currentClusters.get(animatedRoute.get(i - 1) - 1);
            ClusterInfo current = currentClusters.get(animatedRoute.get(i) - 1);
            Line line = new Line(
                    previous.centerX() * scale,
                    previous.centerY() * scale,
                    current.centerX() * scale,
                    current.centerY() * scale
            );
            line.setStroke(Color.YELLOW);
            line.setStrokeWidth(2.5);
            originalOverlayPane.getChildren().add(line);
        }
    }

    private Color determineStrokeColor(int clusterNumber) {
        if (activeAnimatedClusterNumber != null && clusterNumber == activeAnimatedClusterNumber) {
            return Color.YELLOW;
        }
        if (startClusterNumber != null && clusterNumber == startClusterNumber) {
            return Color.ORANGE;
        }
        return Color.DODGERBLUE;
    }

    private int startClusterNumberValue() {
        return startClusterNumber == null ? -1 : startClusterNumber;
    }

    private void stopPathAnimation() {
        if (pathAnimation != null) {
            pathAnimation.stop();
        }
        pathAnimation = null;
        animatedRoute = List.of();
        visibleRouteSegments = 0;
        activeAnimatedClusterNumber = null;
    }

    private Color getHighlightColor() {
        if (referenceColorCount > 0 && referenceColors[0] != null) {
            Color referenceColor = referenceColors[0];
            return Color.hsb(referenceColor.getHue(), Math.max(0.8, referenceColor.getSaturation()), 1.0);
        }
        return Color.ORANGE;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("Black and White Converter");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
