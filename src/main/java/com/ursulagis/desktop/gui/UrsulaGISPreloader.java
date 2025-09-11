package com.ursulagis.desktop.gui;

import javafx.application.Platform;
import javafx.application.Preloader;
import javafx.application.Preloader.StateChangeNotification.Type;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import com.ursulagis.desktop.tasks.UpdateTask;

/**
 * Preloader for UrsulaGIS Desktop application
 * Shows a branded loading screen while the main application initializes
 */
public class UrsulaGISPreloader extends Preloader {
    
    private static final String ICON_PATH = "U_nueva_3_256x256_verde.png";
    private static final String FALLBACK_ICON_PATH = "gui/U_nueva_256x256_verde.png";
    
    private Stage preloaderStage;
    private ProgressBar progressBar;
    private Label statusLabel;
    private Label versionLabel;
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        this.preloaderStage = primaryStage;
        
        // Create the preloader UI
       // VBox root = createPreloaderUI();
       VBox webView = createPreloaderWebView();

       double height = webView.getPrefHeight();
       double width = webView.getPrefWidth();
       Scene scene = new Scene(webView, width-500,height-100);

        // Create scene with a nice background
       //Scene scene = new Scene(webView, 500, 400);
        //scene.setFill(Color.TRANSPARENT);
        
        // Configure stage
        primaryStage.setScene(scene);
        //primaryStage.setTitle("UrsulaGIS Desktop - Loading...");
        //primaryStage.getIcons().addAll(JFXMain.stage.getIcons());//dependo de JFXMain.stage para iniciar el preloader?
		primaryStage.setTitle(Messages.getString("UpdateTaskWelcome.Title"));//"Bienvenido!");
        //primaryStage.initStyle(StageStyle.TRANSPARENT);
        primaryStage.setResizable(true);
        primaryStage.centerOnScreen();
        
        // Set application icon
        setApplicationIcon(primaryStage);
        
        // Show the preloader
        primaryStage.show();
    }
    
    private VBox createPreloaderWebView() {
        String message = UpdateTask.checkForUpdate();
        WebView webView = new WebView();
		// webView.setPrefSize(600, 400);
		webView.autosize();
		WebEngine engine = webView.getEngine();
		engine.loadContent(message);

		VBox v = new VBox(20);
        // v.setBackground(new Background(new BackgroundFill(
        //     Color.web("#2E7D32"), // Dark green background
        //     new CornerRadii(15),
        //     Insets.EMPTY
        // )));
		VBox.setVgrow(webView, Priority.ALWAYS);
		VBox.setMargin(webView, new Insets(10,10,10,10));
		v.getChildren().add(webView);
        return v;
		/*
        Stage welcomeStage = new Stage();
				
		double height = webView.getPrefHeight();
		double width = webView.getPrefWidth();
		Scene scene = new Scene(v, width-150+60,height-100+90);
		welcomeStage.setScene(scene);
		welcomeStage.initOwner(JFXMain.stage);
		welcomeStage.getIcons().addAll(JFXMain.stage.getIcons());
		welcomeStage.setTitle(Messages.getString("UpdateTaskWelcome.Title"));//"Bienvenido!");
		welcomeStage.show();		
        */      
    }
/*
    private VBox createPreloaderUI() {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setBackground(new Background(new BackgroundFill(
            Color.web("#2E7D32"), // Dark green background
            new CornerRadii(15),
            Insets.EMPTY
        )));
        
        // Application logo
        ImageView logoView = createLogo();
        root.getChildren().add(logoView);
        
        // Application title
        Label titleLabel = new Label("UrsulaGIS Desktop");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        titleLabel.setTextFill(Color.WHITE);
        root.getChildren().add(titleLabel);
        
        // Version label - not used in WebView implementation
        // versionLabel = new Label("Version " + JFXMain.VERSION);
        // versionLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        // versionLabel.setTextFill(Color.web("#E8F5E8"));
        // root.getChildren().add(versionLabel);
        
        // Progress bar
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(300);
        progressBar.setPrefHeight(8);
        progressBar.setStyle(
            "-fx-accent: #4CAF50; " +
            "-fx-background-color: rgba(255,255,255,0.3); " +
            "-fx-background-radius: 4; " +
            "-fx-border-radius: 4;"
        );
        root.getChildren().add(progressBar);
        
        // Status label
        statusLabel = new Label("Initializing application...");
        statusLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        statusLabel.setTextFill(Color.WHITE);
        root.getChildren().add(statusLabel);
        
        // Loading animation dots
        Label dotsLabel = new Label("...");
        dotsLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        dotsLabel.setTextFill(Color.WHITE);
        root.getChildren().add(dotsLabel);
        
        return root;
    }*/
    
    private ImageView createLogo() {
        ImageView logoView = new ImageView();
        logoView.setFitWidth(120);
        logoView.setFitHeight(120);
        logoView.setPreserveRatio(true);
        
        // Try to load the main icon
        try {
            Image logo = new Image(getClass().getResourceAsStream(ICON_PATH));
            logoView.setImage(logo);
        } catch (Exception e) {
            // Try fallback icon
            try {
                Image fallbackLogo = new Image(getClass().getResourceAsStream(FALLBACK_ICON_PATH));
                logoView.setImage(fallbackLogo);
            } catch (Exception ex) {
                // If no icon is found, create a simple placeholder
                logoView.setStyle("-fx-background-color: white; -fx-background-radius: 60;");
            }
        }
        
        return logoView;
    }
    
    private void setApplicationIcon(Stage stage) {
        try {
            Image icon = new Image(getClass().getResourceAsStream(ICON_PATH));
            stage.getIcons().add(icon);
        } catch (Exception e) {
            try {
                Image fallbackIcon = new Image(getClass().getResourceAsStream(FALLBACK_ICON_PATH));
                stage.getIcons().add(fallbackIcon);
            } catch (Exception ex) {
                // Icon loading failed, continue without icon
                System.out.println("Warning: Could not load application icon for preloader");
            }
        }
    }
    
    @Override
    public void handleStateChangeNotification(StateChangeNotification stateChangeNotification) {
        if (stateChangeNotification.getType() == Type.BEFORE_START) {
            // Update status before main application starts
            updateStatus("Starting main application...", 1.0);
            
            // Close preloader after a short delay to show the final status
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            JFXMain  mainApp = (JFXMain) stateChangeNotification.getApplication();
            if(mainApp.stage!=null){
                mainApp.stage.toBack();
                System.out.println("send mainApp.stage to back");
            }            
            if (preloaderStage != null) {
                System.out.println("preloaderStage not null");
                //TODO mantain on top of main view until closed
             //   preloaderStage.hide();
            }
        }
    }
    
    @Override
    public void handleProgressNotification(ProgressNotification progressNotification) {
        double progress = progressNotification.getProgress();
        updateStatus(getStatusMessage(progress), progress);
    }
    
    @Override
    public void handleApplicationNotification(PreloaderNotification info) {
        if (info instanceof ProgressNotification) {
            ProgressNotification progressInfo = (ProgressNotification) info;
            double progress = progressInfo.getProgress();
            updateStatus(getStatusMessage(progress), progress);
        } else if (info instanceof StateChangeNotification) {
            StateChangeNotification stateInfo = (StateChangeNotification) info;
            if (stateInfo.getType() == Type.BEFORE_INIT) {
                updateStatus("Initializing JavaFX...", 0.1);
            } else if (stateInfo.getType() == Type.BEFORE_LOAD) {
                updateStatus("Loading application resources...", 0.3);
            } else if (stateInfo.getType() == Type.BEFORE_START) {
                updateStatus("Finalizing startup...", 0.9);
            }
        }
    }
    
    private void updateStatus(String message, double progress) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
        if (progressBar != null) {
            progressBar.setProgress(progress);
        }
    }
    
    private String getStatusMessage(double progress) {
        if (progress < 0.1) {
            return "Initializing JavaFX...";
        } else if (progress < 0.2) {
            return "Loading configuration...";
        } else if (progress < 0.3) {
            return "Initializing WorldWind...";
        } else if (progress < 0.4) {
            return "Setting up database connection...";
        } else if (progress < 0.5) {
            return "Loading GUI components...";
        } else if (progress < 0.6) {
            return "Initializing map layers...";
        } else if (progress < 0.7) {
            return "Loading active polygons...";
        } else if (progress < 0.8) {
            return "Loading NDVI data...";
        } else if (progress < 0.9) {
            return "Finalizing initialization...";
        } else {
            return "Starting main application...";
        }
    }

	/**
	 * metodo que muestra el mensaje de bienvenida
	 * @param message
	 */
    /*
	private static void showWelcomeMessage(String message) {			
			WebView webView = new WebView();
			// webView.setPrefSize(600, 400);
			webView.autosize();
			WebEngine engine = webView.getEngine();
			engine.loadContent(message);

			VBox v = new VBox();
			VBox.setVgrow(webView, Priority.ALWAYS);
			VBox.setMargin(webView, new Insets(10,10,10,10));
			v.getChildren().add(webView);
			Stage welcomeStage = new Stage();
			
			double height = webView.getPrefHeight();
			double width = webView.getPrefWidth();
			Scene scene = new Scene(v, width-150+60,height-100+90);
			welcomeStage.setScene(scene);
			welcomeStage.initOwner(JFXMain.stage);
			//welcomeStage.getIcons().addAll(JFXMain.stage.getIcons());
			welcomeStage.setTitle(Messages.getString("UpdateTaskWelcome.Title"));//"Bienvenido!");
			welcomeStage.show();		
	}*/

	/**
	 * Main method to test the UrsulaGISPreloader class
	 * This allows running the preloader independently for testing purposes
	 */
	public static void main(String[] args) {
		// Launch JavaFX application
		javafx.application.Application.launch(UrsulaGISPreloader.class, args);
	}
}
