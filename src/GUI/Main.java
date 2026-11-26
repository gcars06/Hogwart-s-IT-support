package GUI;

import java.io.IOException;
import java.io.UncheckedIOException;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;

public class Main extends Application {
    private static final String HOME_VIEW = "BaseUISoruce.fxml";
    private static final double DESIGN_WIDTH = 1920.0;
    private static final double DESIGN_HEIGHT = 1080.0;

    private Stage window;
    private StackPane responsiveRoot;
    private Scale contentScale;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        window = primaryStage;
        responsiveRoot = new StackPane();
        Scene scene = new Scene(responsiveRoot, 1280.0, 720.0);
        responsiveRoot.widthProperty().addListener((observable, oldWidth, newWidth) -> scaleContent());
        responsiveRoot.heightProperty().addListener((observable, oldHeight, newHeight) -> scaleContent());
        window.setScene(scene);
        window.setTitle("Hogwarts IT Support");
        setResponsiveView(loadView(HOME_VIEW));
        window.show();
    }

    @FXML
    private void navigate(ActionEvent event) {
        MenuItem menuItem = (MenuItem) event.getSource();
        String view = switch (menuItem.getText()) {
            case "Home" -> HOME_VIEW;
            case "Daily" -> "DailyOverview.fxml";
            case "Weekly" -> "WeeklyOverview.fxml";
            case "Monthly" -> "MonthlyOverview.fxml";
            case "Settings" -> "health_app_settings_page.fxml";
            default -> throw new IllegalArgumentException(
                    "No scene is configured for menu item: " + menuItem.getText());
        };

        showView(view);
    }

    @FXML
    private void openSettings() {
        showView("health_app_settings_page.fxml");
    }

    private void showView(String view) {
        try {
            setResponsiveView(loadView(view));
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to load scene: " + view, e);
        }
    }

    private void setResponsiveView(Parent view) {
        if (!(view instanceof Region region)) {
            throw new IllegalArgumentException("Scene root must be a resizable layout region.");
        }

        region.setMinSize(DESIGN_WIDTH, DESIGN_HEIGHT);
        region.setPrefSize(DESIGN_WIDTH, DESIGN_HEIGHT);
        region.setMaxSize(DESIGN_WIDTH, DESIGN_HEIGHT);

        contentScale = new Scale(1.0, 1.0, DESIGN_WIDTH / 2.0, DESIGN_HEIGHT / 2.0);
        view.getTransforms().setAll(contentScale);
        responsiveRoot.getChildren().setAll(view);
        scaleContent();
    }

    private void scaleContent() {
        if (contentScale == null || responsiveRoot.getWidth() <= 0.0
                || responsiveRoot.getHeight() <= 0.0) {
            return;
        }

        double scale = Math.min(
                responsiveRoot.getWidth() / DESIGN_WIDTH,
                responsiveRoot.getHeight() / DESIGN_HEIGHT);
        contentScale.setX(scale);
        contentScale.setY(scale);
    }

    @FXML
    private void changeImp(ActionEvent event) {
        // Handle the action for the "Imperial" menu item
        System.out.println("Imperial units selected.");
        // Add your logic here to switch to Imperial units
    }
    @FXML
    private void changeMet(ActionEvent event) {
        // Handle the action for the "Metric" menu item
        System.out.println("Metric units selected.");
        // Add your logic here to switch to Metric units
    }


    private Parent loadView(String resource) throws IOException {
        var location = Main.class.getResource(resource);
        if (location == null) {
            throw new IllegalArgumentException("Scene resource not found: " + resource);
        }

        FXMLLoader loader = new FXMLLoader(location);
        loader.setController(this);
        return loader.load();
    }
}
