package com.ursulagis.desktop.gui;

import javafx.application.Platform;
import javafx.application.Preloader;
import javafx.application.Preloader.StateChangeNotification.Type;
import javafx.concurrent.Worker;
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
import javafx.scene.layout.BorderPane;
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

import java.io.IOException;

import com.ursulagis.desktop.tasks.UpdateTask;

import java.util.logging.Logger;
/**
 * Preloader for UrsulaGIS Desktop application
 * Shows a branded loading screen while the main application initializes
 */
public class UrsulaGISPreloader extends Preloader {
	private static final Logger logger = Logger.getLogger(UrsulaGISPreloader.class.getName());

    
    private static final String ICON_PATH = "U_nueva_3_256x256_verde.png";
    //private static final String FALLBACK_ICON_PATH = "gui/U_nueva_256x256_verde.png";
    
    private Stage preloaderStage;
    private ProgressBar progressBar;
    //private Label statusLabel;
    //private Label versionLabel;
    String message = "";

 //   private Worker<Void> loadWorker;

    private WebEngine engine;

    @Override
    public void init() throws Exception {
        message = UpdateTask.checkForUpdate();
        if(message==null)message="<html><body><h>No Network!</h></body></html>";
        logger.fine("UrsulaGISPreloader init message: " + message);
    }
    @Override
    public void start(Stage primaryStage) throws Exception {
        this.preloaderStage = primaryStage;
        
        // Create the preloader UI
       // VBox root = createPreloaderUI();
       VBox webView = createPreloaderWebView();

       double height = webView.getPrefHeight();
        double width = webView.getPrefWidth();
        progressBar=new ProgressBar();
        progressBar.setProgress(0);
        progressBar.setPrefWidth(300);
        BorderPane borderPane = new BorderPane();
        borderPane.setCenter(webView);
        borderPane.setBottom(progressBar);
       
        Scene scene = new Scene(borderPane, width-500,height-100);
        
        // Configure stage
        primaryStage.setScene(scene);

		primaryStage.setTitle(Messages.getString("UpdateTaskWelcome.Title"));//"Bienvenido!");
        //primaryStage.initStyle(StageStyle.TRANSPARENT);
        primaryStage.setResizable(true);
        primaryStage.centerOnScreen();
        
        // Set application icon
        setApplicationIcon(primaryStage);
        
        primaryStage.setOnHiding((e)->{
            engine.load("about:blank");
            
            if (this.engine.getLoadWorker().isRunning()) {
                boolean cancelled = this.engine.getLoadWorker().cancel();
                if (cancelled) {
                    logger.fine("WebEngine load cancelled successfully.");
                }
            }
            logger.fine("preloaderStage onHiding");
        });
        // Show the preloader
        primaryStage.show();
    }
    
    private VBox createPreloaderWebView() {       
        
        WebView webView = new WebView();		
		webView.autosize();
		this.engine = webView.getEngine();
       // this.loadWorker = this.engine.getLoadWorker();
		engine.loadContent(message);
        // engine.locationProperty().addListener((obs, oldLoc, newLoc) -> {
        //     if (newLoc != null && newLoc.contains("youtube.com")) {
        //        // engine.getLoadWorker().cancel();
        //       //  engine.load("about:blank");
        //         try {
        //             java.awt.Desktop.getDesktop().browse(java.net.URI.create(newLoc));
        //         } catch (IOException e) {
        //             // TODO Auto-generated catch block
        //             e.printStackTrace();
        //         }
        //     }
        // });

		VBox v = new VBox(20);
		VBox.setVgrow(webView, Priority.ALWAYS);
		VBox.setMargin(webView, new Insets(10,10,10,10));
		v.getChildren().add(webView);
        return v;
    }
    
    
    private void setApplicationIcon(Stage stage) {
        try {
            Image icon = new Image(getClass().getResourceAsStream(ICON_PATH));
            stage.getIcons().add(icon);
        } catch (Exception e) {
          e.printStackTrace();
        }
    }

    
    @Override
    public void handleApplicationNotification(PreloaderNotification info) {
        if (info instanceof ProgressNotification) {
            ProgressNotification progressInfo = (ProgressNotification) info;
            double progress = progressInfo.getProgress();
            logger.fine("handleApplicationNotification progress: " + progress);

            if(progress>0.8){
                Stage stage = JFXMain.stage;
              
                if(stage!=null ){
                    logger.fine("showing stage");   
                    stage.show();	
                    //&& stage.isShowing()
                    this.preloaderStage.toFront();
                };
            }

         //   Platform.runLater(() -> {
                if (progressBar != null) {
                    progressBar.setProgress(progress);
                    logger.fine("Progress bar updated to: " + progress);
                }
           // });

        } else if (info instanceof StateChangeNotification) {
            StateChangeNotification stateInfo = (StateChangeNotification) info;
            if (stateInfo.getType() == Type.BEFORE_INIT) {
              logger.fine("BEFORE_INIT");
            } else if (stateInfo.getType() == Type.BEFORE_LOAD) {
                logger.fine("BEFORE_LOAD");
            } else if (stateInfo.getType() == Type.BEFORE_START) {
                logger.fine("BEFORE_START");
            }
        }
    }

}
