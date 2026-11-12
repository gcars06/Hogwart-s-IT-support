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
import javafx.stage.Stage;

public class Main extends Application {
    private static final String HOME_VIEW = "BaseUISoruce.fxml";

    private Stage window;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        window = primaryStage;
        window.setScene(new Scene(loadView(HOME_VIEW)));
        window.setTitle("Hogwarts IT Support");
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
            default -> throw new IllegalArgumentException(
                    "No scene is configured for menu item: " + menuItem.getText());
        };

        try {
            window.getScene().setRoot(loadView(view));
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to load scene: " + view, e);
        }
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
