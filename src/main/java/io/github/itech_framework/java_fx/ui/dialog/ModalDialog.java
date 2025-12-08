package io.github.itech_framework.java_fx.ui.dialog;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import io.github.itech_framework.core.utils.DataStorageUtil;
import io.github.itech_framework.core.utils.validator.CommonValidator;
import io.github.itech_framework.java_fx.router.config.RouterConfig;
import io.github.itech_framework.java_fx.utils.node.StyleUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ModalDialog {
    private final Stage dialogStage;
    private final StackPane rootPane;
    private final VBox container;
    private final HBox buttonContainer;
    private Stage ownerStage;
    private Effect originalOwnerEffect;
    private Paint originalFill;

    private static final Duration ANIMATION_DURATION = Duration.millis(300);

    private static ModalDialog currentDialog;
    
    public static ModalDialogBuilder builder(){
        return new ModalDialogBuilder();
    }

    private ModalDialog(Stage owner) {
        dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initStyle(StageStyle.TRANSPARENT);

        container = new VBox(15);
        container.setPadding(new Insets(20));
        container.getStyleClass().add("alert-container");

        rootPane = new StackPane(container);
        rootPane.setPadding(new Insets(15));
        rootPane.setStyle("-fx-background-color: transparent;");

        // Configure rounded corners with shadow
        setupRoundedShadowEffect();

        // Button container
        buttonContainer = new HBox(10);
        buttonContainer.setAlignment(Pos.CENTER_RIGHT);

        // Scene setup
        Scene scene = new Scene(rootPane);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(Objects.requireNonNull(
                getClass().getResource("/static/css/style.css")).toExternalForm()
        );
        dialogStage.setScene(scene);

        if (owner != null) {
            ownerStage = owner;
            dialogStage.initOwner(owner);
            Scene ownerScene = owner.getScene();
            if (ownerScene != null) {
                Parent root = ownerScene.getRoot();
                originalOwnerEffect = root.getEffect();
                originalFill = ownerScene.getFill();
                applyBlurEffect(root);
            }
        }

        applyDarkMode();

        dialogStage.setOnShown(e -> playShowAnimation());
        dialogStage.setOnHidden(e -> {
            if (owner != null) {
                Scene ownerScene = owner.getScene();
                if (ownerScene != null) {
                    Parent root = ownerScene.getRoot();
                    ownerScene.setFill(originalFill);
                    restoreOwnerEffect(root);
                }
            }
        });
        currentDialog = this;
    }

    private void setupRoundedShadowEffect() {
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(rootPane.widthProperty());
        clip.heightProperty().bind(rootPane.heightProperty());
        clip.setArcWidth(30);
        clip.setArcHeight(30);
        rootPane.setClip(clip);

        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.gray(0, 0.2));
        shadow.setRadius(15);
        shadow.setSpread(0.1);
        rootPane.setEffect(shadow);
    }

    private void applyDarkMode() {
        Object darkModeValue = DataStorageUtil.load(RouterConfig.getDarkModeKey());
        if (darkModeValue != null) {
            boolean isDarkMode = Boolean.parseBoolean(darkModeValue.toString());
            rootPane.getStyleClass().remove("dark-mode");
            ownerStage.getScene().setFill(Color.WHITE);
            if (isDarkMode) {
                rootPane.getStyleClass().add("dark-mode");
                ownerStage.getScene().setFill(Color.BLACK);
            }
        }
    }


    public void applyFonts(List<String> fontFamilies) {
        if (CommonValidator.validList(fontFamilies)) {
            Node rootNode = dialogStage.getScene().getRoot();
            StyleUtils.setFontFamily(rootNode, fontFamilies);
        }
    }

    public void applyStyleSheets(List<String> stylesheets){
        dialogStage.getScene().getStylesheets().addAll(stylesheets);
    }

    private void applyBlurEffect(Node node) {
        GaussianBlur blur = new GaussianBlur(10);
        node.setEffect(blur);
    }

    private void restoreOwnerEffect(Node node) {
        node.setEffect(originalOwnerEffect);
    }

    private void playShowAnimation() {
        FadeTransition fade = new FadeTransition(ANIMATION_DURATION, rootPane);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition translate = new TranslateTransition(ANIMATION_DURATION, rootPane);
        translate.setFromY(20);
        translate.setToY(0);

        ScaleTransition scale = new ScaleTransition(ANIMATION_DURATION, rootPane);
        scale.setFromX(0.95);
        scale.setFromY(0.95);
        scale.setToX(1);
        scale.setToY(1);

        new ParallelTransition(fade, translate, scale).play();
    }

    private void playHideAnimation(Runnable onFinished) {
        FadeTransition fade = new FadeTransition(ANIMATION_DURATION, rootPane);
        fade.setFromValue(1);
        fade.setToValue(0);

        TranslateTransition translate = new TranslateTransition(ANIMATION_DURATION, rootPane);
        translate.setFromY(0);
        translate.setToY(20);

        ScaleTransition scale = new ScaleTransition(ANIMATION_DURATION, rootPane);
        scale.setFromX(1);
        scale.setFromY(1);
        scale.setToX(0.95);
        scale.setToY(0.95);

        ParallelTransition transition = new ParallelTransition(fade, translate, scale);
        transition.setOnFinished(e -> onFinished.run());
        transition.play();
    }

    public void close() {
        playHideAnimation(dialogStage::close);
    }

    public void show() {
        dialogStage.sizeToScene();
        dialogStage.centerOnScreen();
        dialogStage.showAndWait();
    }

    public static class ModalDialogBuilder {
        private final List<Node> contentNodes = new ArrayList<>();
        private final List<Button> buttons = new ArrayList<>();
        private Stage owner;
        private ModalDialog dialog;
        private final List<String> customFonts = new ArrayList<>();
        private final List<String> stylesheets = new ArrayList<>();

        public ModalDialogBuilder addContent(Node node) {
            contentNodes.add(node);
            return this;
        }

        public ModalDialogBuilder addButton(Button button) {
            buttons.add(button);
            return this;
        }

        public ModalDialogBuilder addActionButton(String text, Runnable action) {
            Button btn = new Button(text);
            btn.getStyleClass().add("dialog-action-button");
            btn.setOnAction(e -> {
                if (action != null) action.run();
            });
            buttons.add(btn);
            return this;
        }

        public ModalDialogBuilder addCloseButton(String text) {
            Button btn = new Button(text);
            btn.getStyleClass().add("disabled");
            btn.setOnAction(e -> closeDialog());
            buttons.add(btn);
            return this;
        }

        public ModalDialogBuilder addOwner(Stage owner) {
            this.owner = owner;
            return this;
        }

        public ModalDialogBuilder customFonts(String... fontFamilies) {
            Collections.addAll(customFonts, fontFamilies);
            return this;
        }

        public ModalDialogBuilder stylesheets(String... stylesheets){
            Collections.addAll(this.stylesheets, stylesheets);
            return this;
        }

        public ModalDialog build() {
            dialog = new ModalDialog(owner);

            dialog.container.getChildren().addAll(contentNodes);

            if (!buttons.isEmpty()) {
                dialog.container.getChildren().add(dialog.buttonContainer);
                dialog.buttonContainer.getChildren().addAll(buttons);
            }

            if (!customFonts.isEmpty()) {
                StyleUtils.setFontFamily(dialog.rootPane, customFonts);
            }

            if(!stylesheets.isEmpty()){
                dialog.applyStyleSheets(stylesheets);
            }

            return dialog;
        }

        private void closeDialog() {
            if (dialog != null) {
                dialog.close();
                currentDialog = null;
            }
        }
    }

	public static ModalDialog getCurrentDialog() {
		return currentDialog;
	}

	public static void setCurrentDialog(ModalDialog currentDialog) {
		ModalDialog.currentDialog = currentDialog;
	}
    
}