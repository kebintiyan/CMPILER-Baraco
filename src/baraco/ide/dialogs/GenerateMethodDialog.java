package baraco.ide.dialogs;

import baraco.execution.commands.MethodList;
import baraco.templates.BaracoMethodTemplate;
import baraco.templates.BaracoMethodTemplateParameter;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Optional;

public class GenerateMethodDialog {

    private static final String TYPE_VOID = "void";
    private static final String TYPE_STRING = "string";
    private static final String TYPE_INT = "int";
    private static final String TYPE_DECIMAL = "decimal";
    private static final String TYPE_BOOL = "bool";
    private static final String TYPE_STRING_ARRAY = "string[]";
    private static final String TYPE_INT_ARRAY = "int[]";
    private static final String TYPE_DECIMAL_ARRAY = "decimal[]";
    private static final String TYPE_BOOL_ARRAY = "bool[]";

    private Dialog<String> dialog;
    private TabPane tabPane;
    private Node addThisButton;
    private Node addAllButton;

    public GenerateMethodDialog() {
        this.dialog = new Dialog<>();
        this.setupDialog();
    }

    private void setupDialog() {
        dialog.setTitle("Generate Method");
        dialog.setHeaderText(null);


        // Set the button types.
        ButtonType addAllButtonType = new ButtonType("Add All", ButtonBar.ButtonData.OK_DONE);
        ButtonType addThisButtonType = new ButtonType("Add This", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, addAllButtonType, addThisButtonType);

        addThisButton = dialog.getDialogPane().lookupButton(addThisButtonType);
        addAllButton = dialog.getDialogPane().lookupButton(addAllButtonType);
        //addThisButton.setDisable(true);

        VBox root = new VBox();

        tabPane = new TabPane();
        Tab tab = new Tab();
        tab.setText("Function 1");
        tab.setContent(new GenerateMethodGrid(tabPane, addThisButton, addAllButton));
        tabPane.getTabs().add(tab);

        tab.setOnClosed(event -> {
            validateInput();
        });

        tabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            GenerateMethodGrid grid = (GenerateMethodGrid) newValue.getContent();
            grid.validateInput();
        });


        Button addButton = new Button("Anotha One. DJ KHALED!");
        addButton.setOnAction(event -> {
            Tab newTab = new Tab("Function " + (tabPane.getTabs().size() + 1));
            newTab.setContent(new GenerateMethodGrid(tabPane, addThisButton, addAllButton));
            newTab.setOnClosed(closeEvent -> {
                validateInput();
            });
            tabPane.getTabs().add(newTab);
            tabPane.getSelectionModel().select(newTab);
        });

        root.setMargin(addButton, new Insets(10));

        root.getChildren().addAll(tabPane, addButton);


        dialog.getDialogPane().setContent(root);

        // Convert the result to a string
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addThisButtonType) {
                //return generateSampleString();
                System.out.println("Add This Pressed");
                GenerateMethodGrid grid = (GenerateMethodGrid) tabPane.getSelectionModel().getSelectedItem().getContent();
                return grid.generateSampleString();
            }
            else if (dialogButton == addAllButtonType) {
                String output = "";
                System.out.println("Add All Pressed");
                for (Tab functionTab : tabPane.getTabs()) {
                    GenerateMethodGrid grid = (GenerateMethodGrid) functionTab.getContent();
                    output += grid.generateSampleString();
                    output += "\n\n";
                }

                return output;
            }
            return null;
        });
    }


    public String showGenerateMethodDialog() {
        Optional<String> result = dialog.showAndWait();

        return result.get();
    }

    public void validateInput() {
        addThisButton.setDisable(true);
        addAllButton.setDisable(true);

        for (Tab tab : tabPane.getTabs()) {
            if (tab == null)
                continue;

            GenerateMethodGrid grid = (GenerateMethodGrid) tab.getContent();
            grid.validateInput();
        }
    }
}
