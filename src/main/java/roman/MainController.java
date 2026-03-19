package roman;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.Arrays;

public class MainController {

    private static final int MAX_REFERENCE_COLORS = 5;

    private Image currentImage;
    private final Color[] referenceColors = new Color[MAX_REFERENCE_COLORS];
    private int referenceColorCount = 0;

    @FXML
    private ImageView originalImageView;

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
    private Label brightnessValueLabel;

    @FXML
    private Label saturationValueLabel;

    @FXML
    private Label differenceValueLabel;

    @FXML
    private ColorPicker targetColorPicker;

    @FXML
    private Label referenceCountLabel;

    @FXML
    private HBox referenceColorsBox;

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

        blackWhiteImageView.setImage(
                convertToBlackWhite(
                        currentImage,
                        brightnessSlider.getValue(),
                        saturationSlider.getValue(),
                        (int) Math.round(differenceSlider.getValue())
                )
        );
    }

    private void updateSliderLabels() {
        brightnessValueLabel.setText(String.format("%.2f", brightnessSlider.getValue()));
        saturationValueLabel.setText(String.format("%.2f", saturationSlider.getValue()));
        differenceValueLabel.setText(String.valueOf((int) Math.round(differenceSlider.getValue())));
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

    private boolean matchesReferenceColor(Color pixelColor, int differenceThreshold) {
        if (referenceColorCount == 0) {
            return true;
        }

        int[] pixelChannels = normalizeDominantChannel(toRgb(pixelColor), differenceThreshold);

        for (int i = 0; i < referenceColorCount; i++) {
            Color referenceColor = referenceColors[i];
            if (referenceColor == null) {
                continue;
            }

            int[] referenceChannels = normalizeDominantChannel(toRgb(referenceColor), differenceThreshold);
            if (channelDifference(pixelChannels, referenceChannels) <= differenceThreshold) {
                return true;
            }
        }

        return false;
    }

    private int[] toRgb(Color color) {
        return new int[]{
                (int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255),
                (int) Math.round(color.getBlue() * 255)
        };
    }

    private int[] normalizeDominantChannel(int[] rgb, int differenceThreshold) {
        int[] normalized = Arrays.copyOf(rgb, rgb.length);
        int max = Math.max(normalized[0], Math.max(normalized[1], normalized[2]));

        for (int i = 0; i < normalized.length; i++) {
            if (max - normalized[i] > differenceThreshold) {
                normalized[i] = 0;
            }
        }

        return normalized;
    }

    private int channelDifference(int[] first, int[] second) {
        int dr = Math.abs(first[0] - second[0]);
        int dg = Math.abs(first[1] - second[1]);
        int db = Math.abs(first[2] - second[2]);
        return dr + dg + db;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("Black and White Converter");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
