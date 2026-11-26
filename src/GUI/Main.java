package GUI;

/**
 * Starts the Hogwarts IT Support JavaFX application and manages its main screens.
 *
 * <p>This class loads FXML views, routes navigation actions, scales each view to fit
 * the window, and keeps the selected measurement system reflected in the settings
 * page's units menu.
 * 
 * @author Gabriel Villalpando
 */


import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;


public class Main extends Application {
    private static final String HOME_VIEW = "BaseUISoruce.fxml";
    // FXML layouts use this shared design canvas as their reference size.
    private static final double DESIGN_WIDTH = 1920.0;
    private static final double DESIGN_HEIGHT = 1080.0;

    private Stage window;
    private StackPane responsiveRoot;
    private Scale contentScale;
    private UnitSystem unitSystem = UnitSystem.METRIC;

    @FXML
    private Menu unitsMenu;

    @FXML
    private TitledPane dateTitlePane;

    @FXML
    private Label dailyStepsValue, dailyWaterValue, dailyCaloriesValue, dailySleepValue;
    private Label dailyStepsGoalValue, dailyWaterGoalValue, dailyCaloriesGoalValue, dailySleepGoalValue;
    private Label weeklyStepsValue, weeklyWaterValue, weeklyCaloriesValue, weeklySleepValue;
    private Label weeklyStepsGoalValue, weeklyWaterGoalValue, weeklyCaloriesGoalValue, weeklySleepGoalValue;
    private Label monthlyStepsValue, monthlyWaterValue, monthlyCaloriesValue, monthlySleepValue;
    private Label monthlyStepsGoalValue, monthlyWaterGoalValue, monthlyCaloriesGoalValue, monthlySleepGoalValue;

    private record MetricPlaceholders(
            int steps, double waterLiters, int calories, double sleepHours) {}

    private record PeriodPlaceholders(MetricPlaceholders values, MetricPlaceholders goals) {}

    private static final PeriodPlaceholders DAILY_PLACEHOLDERS = new PeriodPlaceholders(
            new MetricPlaceholders(8420, 2.4, 2150, 7.5),
            new MetricPlaceholders(10000, 2.5, 2000, 8.0));
    private static final PeriodPlaceholders WEEKLY_PLACEHOLDERS = new PeriodPlaceholders(
            new MetricPlaceholders(58940, 16.8, 15050, 52.5),
            new MetricPlaceholders(70000, 17.5, 14000, 56.0));
    private static final PeriodPlaceholders MONTHLY_PLACEHOLDERS = new PeriodPlaceholders(
            new MetricPlaceholders(252600, 72.0, 64500, 225.0),
            new MetricPlaceholders(300000, 75.0, 60000, 240.0));

    private enum UnitSystem {
        METRIC,
        IMPERIAL
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        window = primaryStage;
        responsiveRoot = new StackPane();
        Scene scene = new Scene(responsiveRoot, 1280.0, 720.0);
        // Recalculate the uniform scale whenever the available scene area changes.
        responsiveRoot.widthProperty().addListener((observable, oldWidth, newWidth) -> scaleContent());
        responsiveRoot.heightProperty().addListener((observable, oldHeight, newHeight) -> scaleContent());
        window.setScene(scene);
        window.setTitle("Hogwarts IT Support");
        setResponsiveView(loadView(HOME_VIEW));
        window.show();
    }

    @FXML
    private void navigate(ActionEvent event) {
        Object source = event.getSource();
        String label = switch (source) {
            case MenuItem menuItem -> menuItem.getText();
            case Button button -> button.getText();
            default -> throw new IllegalArgumentException(
                    "Navigation requires a MenuItem or Button.");
        };

        String view = switch (label) {
            case "Home" -> HOME_VIEW;
            case "Daily" -> "DailyOverview.fxml";
            case "Weekly" -> "WeeklyOverview.fxml";
            case "Monthly" -> "MonthlyOverview.fxml";
            case "Settings" -> "health_app_settings_page.fxml";
            default -> throw new IllegalArgumentException(
                    "No scene is configured for navigation label: " + label);
        };

        showView(view);
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

        // Keep each FXML root on the same canvas so all screens scale consistently.
        region.setMinSize(DESIGN_WIDTH, DESIGN_HEIGHT);
        region.setPrefSize(DESIGN_WIDTH, DESIGN_HEIGHT);
        region.setMaxSize(DESIGN_WIDTH, DESIGN_HEIGHT);

        // Scale around the canvas center to preserve proportions and avoid clipping.
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

        // Fit the whole design canvas in the window without stretching its contents.
        double scale = Math.min(
                responsiveRoot.getWidth() / DESIGN_WIDTH,
                responsiveRoot.getHeight() / DESIGN_HEIGHT);
        contentScale.setX(scale);
        contentScale.setY(scale);
    }

    @FXML
    private void changeImp(ActionEvent event) {
        setUnitSystem(UnitSystem.IMPERIAL);
    }

    @FXML
    private void changeMet(ActionEvent event) {
        setUnitSystem(UnitSystem.METRIC);
    }

    private void setUnitSystem(UnitSystem unitSystem) {
        this.unitSystem = unitSystem;
        if (unitsMenu != null) {
            unitsMenu.setText(unitSystem == UnitSystem.METRIC ? "Metric" : "Imperial");
        }
    }

    @FXML
    private void initialize() {
        setUnitSystem(unitSystem);
        updateDateTitle();
        updatePlaceholderValues();
    }

    private void updatePlaceholderValues() {
        if (dailyStepsValue == null || dailyStepsGoalValue == null) {
            return;
        }

        setPeriodValues(DAILY_PLACEHOLDERS, dailyStepsValue, dailyWaterValue,
                dailyCaloriesValue, dailySleepValue, dailyStepsGoalValue, dailyWaterGoalValue,
                dailyCaloriesGoalValue, dailySleepGoalValue);
        setPeriodValues(WEEKLY_PLACEHOLDERS, weeklyStepsValue, weeklyWaterValue,
                weeklyCaloriesValue, weeklySleepValue, weeklyStepsGoalValue, weeklyWaterGoalValue,
                weeklyCaloriesGoalValue, weeklySleepGoalValue);
        setPeriodValues(MONTHLY_PLACEHOLDERS, monthlyStepsValue, monthlyWaterValue,
                monthlyCaloriesValue, monthlySleepValue, monthlyStepsGoalValue, monthlyWaterGoalValue,
                monthlyCaloriesGoalValue, monthlySleepGoalValue);
    }

    private void setMetricValues(MetricPlaceholders values, Label steps, Label water,
            Label calories, Label sleep) {
        steps.setText("%,d steps".formatted(values.steps()));
        water.setText("%.1f L".formatted(values.waterLiters()));
        calories.setText("%,d kcal".formatted(values.calories()));
        sleep.setText("%.1f h".formatted(values.sleepHours()));
    }

    private void setPeriodValues(PeriodPlaceholders period, Label steps, Label water,
            Label calories, Label sleep, Label stepsGoal, Label waterGoal,
            Label caloriesGoal, Label sleepGoal) {
        setMetricValues(period.values(), steps, water, calories, sleep);
        setMetricValues(period.goals(), stepsGoal, waterGoal, caloriesGoal, sleepGoal);
    }

    private void updateDateTitle() {
        if (dateTitlePane != null) {
            String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
            dateTitlePane.setText("Date: " + currentDate);
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
