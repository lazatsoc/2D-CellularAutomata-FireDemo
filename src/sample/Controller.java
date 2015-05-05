package sample;

import javafx.animation.Animation;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import javafx.util.Callback;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    @FXML
    VBox TimeVBox;
    @FXML
    TextField calenTextField;
    @FXML
    Slider calenSlider, zoomSlider;
    @FXML
    Button CreateButton, StepButton;
    @FXML
    ScrollPane RightScrollPane;
    @FXML
    Canvas canvas;
    @FXML
    ListView<Cell.BedType> FirebedListView;
    @FXML
    Button RunToggleButton;

    IntegerProperty calen = new SimpleIntegerProperty();
    Cell[][] cells;
    BooleanProperty runningProperty = new SimpleBooleanProperty(true);
    BooleanProperty runToggledProperty = new SimpleBooleanProperty(false);


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        MakeBindings();
        List<String> rulesList= new ArrayList<>(256);
        for (int i=0;i<256;i++) rulesList.add(Integer.toString(i));
        calenSlider.setMax(200); calenSlider.setMin(20);
        zoomSlider.setMax(3.125); zoomSlider.setMin(0.125); zoomSlider.setValue(1);
        calenSlider.setValue(50);
        CreateButton.setOnAction(event -> create());
        StepButton.setOnAction(event -> step());
    }

    private void create() {
        canvas.setVisible(false);
        canvas.getGraphicsContext2D().clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        canvas.setHeight(0);
        canvas.setWidth(0);
        TimeVBox.setVisible(true);
        cells = new Cell[calen.get()][];

        for (int i=0;i<cells.length;i++) {
            cells[i] = new Cell[cells.length];
            for (int j=0;j<cells.length;j++) {
                final Cell cell = new Cell(16, 16);
                cell.widthProperty().bind(zoomSlider.valueProperty().multiply(16));
                cell.heightProperty().bind(zoomSlider.valueProperty().multiply(16));
                cell.setStroke(Color.BLACK);
                cell.setStrokeWidth(1);

                cell.setOnMouseClicked(event1 -> {
                    Cell.BedType type = FirebedListView.getSelectionModel().getSelectedItem();
                    Cell source = (Cell) event1.getSource();
                    source.setBed(type);
                });

                cell.setOnMouseEntered(event1 -> {
                    if (event1.isControlDown()) {
                        Cell.BedType type = FirebedListView.getSelectionModel().getSelectedItem();
                        Cell source = (Cell) event1.getSource();
                        source.setBed(type);
                    }
                });

                cells[i][j] = cell;
            }
        }
        TimeVBox.getChildren().clear();
        HBox cabox;
        for (Rectangle[] cr : cells) {
            cabox=new HBox();
            cabox.getChildren().addAll(cr);
            TimeVBox.getChildren().add(cabox);
        }
        runningProperty.set(false);
    }

    private void step() {
        int n = cells.length;
        final GraphicsContext gc = canvas.getGraphicsContext2D();
        if (canvas.getWidth() == 0) { //Make Canvas of same size
            TimeVBox.setVisible(false);
            canvas.setVisible(true);
            canvas.setWidth(16 * n + 16);
            canvas.setHeight(16 * n + 16);

        }
        canvas.setHeight(canvas.getHeight() + 16);
        runningProperty.set(true);

        Task<Cell[][]> task = new Task<Cell[][]>() {
            @Override
            protected Cell[][] call() throws Exception {
                return UpdateCells();
            }
        };
        task.setOnSucceeded(event -> {
            UpdateUI(n, gc);
            runningProperty.set(false);
        });
        Thread t = new Thread(task);
        t.start();

    }

    private Cell[][] UpdateCells() {
        int n = cells.length;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                float sum = 0, port, sqrtf = (float) Math.sqrt(2);
                for (int ii = i - 1; ii <= i + 1; ii++)
                    for (int jj = j - 1; jj <= j + 1; jj++) {
                        if (ii > 0 && jj > 0 && ii < n && jj < n) {
                            port = cells[ii][jj].burned * 1f / 9f;
                            if (ii != i && jj != j) port /= sqrtf;
                            sum += port;
                        }
                    }
                //firebed adjustment
                float flam=cells[i][j].getFlammablePercentage();
                float burned = 0.24f * cells[i][j].getBurned() + sum + flam*sum;
                burned = burned > 1f ? 1f : burned;
                //float burned = (1f-flam)*0.24f * cells[i][j].getBurned() + flam*sum;
                cells[i][j].setNewBurned(burned);
            }
        }
        return cells;
    }

    private void UpdateUI(int n, GraphicsContext gc) {
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                cells[i][j].commit();
                gc.setFill(cells[i][j].getComputedFill());
                //gc.setFill(Color.BLACK);
                gc.fillRect(16 * j, 16 * i, 16, 16);
            }
        }

    }

    private void MakeBindings() {

        calenTextField.textProperty().bind(Bindings.createStringBinding(
                        () -> Integer.toString((int) calenSlider.getValue()),
                        calenSlider.valueProperty())
        );
        calen.bind(Bindings.createObjectBinding(() -> (int) calenSlider.getValue(), calenSlider.valueProperty()));
        Scale scale=new Scale(1,1);
        scale.xProperty().bind(zoomSlider.valueProperty());
        scale.yProperty().bind(zoomSlider.valueProperty());
        canvas.getTransforms().add(scale);
        StepButton.disableProperty().bind(runningProperty);
        FirebedListView.setCellFactory(new Callback<ListView<Cell.BedType>, ListCell<Cell.BedType>>() {
            @Override
            public ListCell<Cell.BedType> call(ListView<Cell.BedType> param) {
                return new ListCell<Cell.BedType>() {
                    @Override
                    protected void updateItem(Cell.BedType item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) return;
                        HBox hbox = new HBox();
                        hbox.setAlignment(Pos.CENTER_RIGHT);
                        hbox.setSpacing(10);
                        Label text = new Label(item.name());
                        Rectangle rect = new Rectangle(20, 20);
                        rect.setStrokeWidth(1);
                        rect.setStroke(Color.BLACK);
                        rect.setFill(Cell.BedPaints[item.ordinal()]);
                        hbox.getChildren().addAll(text, rect);
                        setGraphic(hbox);
                    }
                };
            }
        });
        FirebedListView.setItems(FXCollections.observableArrayList(Cell.BedType.values()));
        RunToggleButton.setOnAction(event -> {
            Boolean value = runToggledProperty.getValue();
            if (value) ((Button) event.getSource()).setText("Run");
            else ((Button) event.getSource()).setText("Stop");
            runToggledProperty.setValue(!value);
            int n = cells.length;
            GraphicsContext gc = canvas.getGraphicsContext2D();
            Platform.runLater(() -> {


                if (canvas.getWidth() == 0) { //Make Canvas of same size
                    TimeVBox.setVisible(false);
                    canvas.setVisible(true);
                    canvas.setWidth(16 * n + 16);
                    canvas.setHeight(16 * n + 16);

                }
                canvas.setHeight(canvas.getHeight() + 16);
                runningProperty.set(true);
            });
            Task<Boolean> task = new Task<Boolean>() {
                @Override
                protected Boolean call() throws Exception {
                    while (runToggledProperty.get()) {
                        int steps=0;
                        UpdateCells();
                        if (steps++%5==0)
                            Platform.runLater(()->UpdateUI(n,gc));
                        try {
                            Thread.sleep(200);
                        } catch(InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    return true;
                }
            };
            task.setOnSucceeded(event1 -> {
                UpdateUI(n, gc);
                runningProperty.set(false);
            });
            Thread t = new Thread(task);
            t.start();
        });
    }

}
