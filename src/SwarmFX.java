import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.LinkedList;

//import com.sun.glass.events.MouseEvent;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * A local-client front-end for Liberty Swarm
 * 
 * @author Steve Cina
 * @since April 2020
 *
 */
public class SwarmFX extends Application {
	private static String APPLICATION_HOST;
	private static final String TITLE_LBL = "Liberty Swarm";
	private static final int SIZE = 600;
	private static String PID = "p1045";
	private static final int ZOMBIE_REACH = 5;
	private static int KillCount;
	private static URL getUrl;
	private static HttpURLConnection conn;
	private static LinkedList<Sprite> sprites;
	private static Canvas canvas;
	private static GraphicsContext gc;
	private static Scene mainScene;
	private static Player player;
	private final static boolean[] buttonsPressed = {false, false, false, false};
	private static AnimationTimer timer;
	
	//call init(), then start()
	public static void main(String[] args) {
		
		launch(args);
	}
	
	//init is called first
	@Override
	public void init() {
		KillCount = 0;
		player = new Player(PID, 500, 500);
		sprites = new LinkedList<Sprite>();
		canvas = new Canvas(SIZE, SIZE);
		gc = canvas.getGraphicsContext2D();
		
	}
	//start is called after init
	@Override
	public void start(Stage primaryStage) throws IOException {
		enableDevModePrompt();
		getBoard();
		primaryStage.setTitle(TITLE_LBL);
		
		//this sets up our frame updates
		timer = new AnimationTimer() {
            @Override
            public void handle(long now) {   
            	
				getBoard();
				checkDead();
				draw();
				movePlayer();
				updatePos();
				
            }
        };
        timer.start();
		
		
		//Setup the components of our main scene.
		BorderPane pane = new BorderPane();
		pane.setPrefSize(SIZE+200, SIZE);
		pane.getChildren().add(canvas);
		Button clearBtn = new Button("Clear");
		clearBtn.setPrefWidth(190);
		clearBtn.setOnMouseClicked(e -> {
			clear();
		});
		Button newBtn = new Button("New");
		newBtn.setPrefWidth(190);
		newBtn.setOnMouseClicked(e -> {
			clear();
			for (int i = 0; i < buttonsPressed.length; i++) buttonsPressed[i] = false;
			KillCount = 0;
			timer.start();
		});
		VBox rightPane = new VBox();
		rightPane.setPrefWidth(200);
		rightPane.getChildren().addAll(newBtn, clearBtn);
		pane.setRight(rightPane);
		
		mainScene = new Scene(pane);
		//start movement
		mainScene.addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {

			@Override
			public void handle(KeyEvent event) {
				switch(event.getText()) {
				case "s":
					buttonsPressed[0] = true; //down
					break;
				case "w":
					buttonsPressed[1] = true; //up
					break;
				case "a":
					buttonsPressed[2] = true; //left
					break;
				case "d":
					buttonsPressed[3] = true; //right
					break;
				}
				
			}
			
		});
		//stop movement
		mainScene.addEventHandler(KeyEvent.KEY_RELEASED, new EventHandler<KeyEvent>() {

			@Override
			public void handle(KeyEvent event) {
				switch(event.getText()) {
				case "s":
					buttonsPressed[0] = false; //down
					break;
				case "w":
					buttonsPressed[1] = false; //up
					break;
				case "a":
					buttonsPressed[2] = false; //left
					break;
				case "d":
					buttonsPressed[3] = false; //right
					break;
				}
				
			}
			
		});
		//Shoot em up
		mainScene.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent click1) {
				int x = (int) Math.round(click1.getX());
				int y = (int) Math.round(click1.getY());
								
				for (Sprite s: sprites) {
					int sX = s.getX();
					int sY = s.getY();
					
					if (Math.abs(sX-x) <= 5 && Math.abs(sY-y) <= 5) {
						if (kill(s.getId())) {
							KillCount++;
						}
						break;
					}
				}
						
				
			}
			
		});
		
		primaryStage.setScene(mainScene);
		primaryStage.show();
		
	}
	
	
	
	public void draw() {
		getBoard();
		//Background Color/Image
		gc.setFill((Paint) Color.BLACK);
		gc.fillRect(0, 0, SIZE, SIZE);
		
		//NPC sprites
		for (Sprite s : sprites) {
			if (s.type == 1) { //if zombie
				gc.setFill((Paint) Color.FORESTGREEN);
				gc.fillOval(s.getX(), s.getY(), 5, 5);
			}
			else { //if other player (benign)
				gc.setFill((Paint) Color.PALEVIOLETRED);
				gc.fillOval(s.getX(), s.getY(), 5, 5);
			}
		}
		
		//Player 
		int px = player.getX();
		int py = player.getY();
		gc.setFill((Paint) Color.GHOSTWHITE);
		gc.fillOval(px, py, 5, 5);
		
		
	}
	
	public static void getBoard() {
			try {
				getUrl = new URL("http://" + APPLICATION_HOST + ":9081/Swarm");
				conn = (HttpURLConnection) getUrl.openConnection();
				conn.setRequestMethod("GET");
				conn.setRequestProperty("Accept", "application/json");
				if (conn.getResponseCode() != 200) {
					throw new RuntimeException("getBoard Failed: HTTP " + conn.getResponseCode());
				}
				BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				
				String output = br.readLine();
				
				String[] objList = output.split("&");
				sprites = new LinkedList<Sprite>();
				for (int i = 1; i < objList.length; i++) {
					String[] objProps = objList[i].split("-");
					sprites.add(new Sprite(objProps[0], 
							Integer.parseInt(objProps[1]), Integer.parseInt(objProps[2])));
				}
				
			} catch (IllegalStateException ise) {
				try {
					Thread.sleep(5);
					} catch (InterruptedException e) {	};
				
			} catch (ProtocolException e) {
				e.printStackTrace();
				
			} catch (RuntimeException rte) {
				try {
					Thread.sleep(5);
				} catch (InterruptedException e) {
					//
				}
				getBoard();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				conn.disconnect();
			}
		
	}
	
	public static void updatePos() {
			
		try {
			URL putUrl = new URL("http://" + APPLICATION_HOST + ":9081/Swarm?id=" + player.getId() + "&newX=" + player.getX() + "&newY=" + player.getY());
			HttpURLConnection conn = (HttpURLConnection) putUrl.openConnection();
			conn.setRequestMethod("PUT");
			conn.setRequestProperty("Accept","application/json");
			
			if (conn.getResponseCode() != 200) {
				throw new RuntimeException("updatePos Failed: HTTP " + conn.getResponseCode());
			}
			
		} catch (IllegalStateException ise) {
			updatePos();
			
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (RuntimeException rte) {
			try {
				Thread.sleep(5);
				updatePos();
			} catch (InterruptedException e) {
				//
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			conn.disconnect();
		}
		
	}
	public static void movePlayer() {
		// 0 = down
		// 1 = up
		// 2 = left
		// 3 = right
		int speed = player.getSpeed();
		if (buttonsPressed[0] && player.getY() + player.getSpeed() < SIZE) {
			player.setY(player.getY() + speed);
		}
		if (buttonsPressed[1] && player.getY() - player.getSpeed() > 0) {
			player.setY(player.getY() - speed);
		}
		if (buttonsPressed[2] && player.getX() - player.getSpeed() > 0) {
			player.setX(player.getX() - speed);
		}
		if (buttonsPressed[3] && player.getX() + player.getSpeed() < SIZE) {
			player.setX(player.getX() + speed);
		}
	}
	
	public static void checkDead() {
		for (Sprite s : sprites) {
			if (s.type==1) {
				if (s.getX()==player.getX() && s.getY()==player.getY()) {
					kill(player.getId());
					youLose();
				}
				if (Math.abs(s.getX()-player.getX()) < ZOMBIE_REACH &&	Math.abs(s.getY()-player.getY()) < ZOMBIE_REACH) {
					kill(player.getId());
					youLose();
				}
			}
		}
	}
	
	public static boolean kill(String id) {
		try {
			URL delUrl = new URL("http://" + APPLICATION_HOST + ":9081/Swarm?id=" + id);
			HttpURLConnection conn = (HttpURLConnection) delUrl.openConnection();
			conn.setRequestMethod("DELETE");
			
			if (conn.getResponseCode() == 200) {
				return true;
			}
		} catch (IllegalStateException e) {
			kill(id); //might work
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			conn.disconnect();
		}
		return false;
	}
	/**
	 * Careful with this.
	 */
	public static void clear() {
		for (Sprite s : sprites) {
			URL delUrl;
			try {
				delUrl = new URL("http://" + APPLICATION_HOST + ":9081/Swarm?id=" + s.getId());
				HttpURLConnection conn = (HttpURLConnection) delUrl.openConnection();
				conn.setRequestMethod("DELETE");
				conn.getResponseCode();
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (ProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				conn.disconnect();
			}
			
		}
	}
	
	public static void youLose() {
		timer.stop();
		
		Stage lossStage = new Stage();
		lossStage.setTitle("Expiration");
		Label lossLabel = new Label("You have been devoured.");
		Label killLabel = new Label("You killed " + KillCount + " of the undead.");
		Button lossOkay = new Button("Okay");
		lossOkay.setOnMouseClicked(e -> {
			lossStage.close();
		});
		VBox lossPane = new VBox();
		lossPane.setAlignment(Pos.CENTER);
		lossPane.setPrefWidth(300);
		lossPane.getChildren().addAll(lossLabel, killLabel, lossOkay);
		lossStage.setScene(new Scene(lossPane));
		lossStage.initModality(Modality.APPLICATION_MODAL);
		lossStage.requestFocus();
		lossStage.show();
	}
	
	public static void enableDevModePrompt() {
		Stage devStage = new Stage();
		ToggleGroup choices = new ToggleGroup();
		RadioButton localhostBtn = new RadioButton("localhost");
		RadioButton lsoBtn = new RadioButton("libertySwarm.online");
		RadioButton otherBtn = new RadioButton("specify:");
		localhostBtn.setToggleGroup(choices);
		lsoBtn.setToggleGroup(choices);
		otherBtn.setToggleGroup(choices);
		TextField specifyField = new TextField();
		
		Button okayBtn = new Button("Continue");
		okayBtn.setOnMouseClicked(e -> {
			if (choices.getSelectedToggle().equals(localhostBtn)) {
				APPLICATION_HOST = "localhost";
			} 
			else if (choices.getSelectedToggle().equals(lsoBtn)) {
				APPLICATION_HOST = "www.libertyswarm.online";
			} else if (choices.getSelectedToggle().equals(otherBtn)) {
				if (specifyField.getText().length() > 0) {
					APPLICATION_HOST = specifyField.getText();
				}
			} else {
				return;
			}
			devStage.close();
		});
		Button cancelBtn = new Button("Cancel");
		cancelBtn.setOnMouseClicked(e -> {
			System.exit(0);
		});
		HBox okayCancel = new HBox();
		okayCancel.getChildren().addAll(okayBtn, cancelBtn);
		VBox pane = new VBox();
		pane.getChildren().addAll(localhostBtn, lsoBtn, otherBtn, specifyField, okayCancel);
		
		
		devStage.setTitle("server selection");
		
		Scene devScene = new Scene(pane);
		devStage.setScene(devScene);
		devStage.showAndWait();
	}
}
