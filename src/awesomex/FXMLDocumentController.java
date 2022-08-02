package awesomex;

import java.io.File;
import java.io.FileFilter;
import java.net.URL;
import java.sql.*;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import javax.swing.JFileChooser;
import jssc.SerialPort;
import jssc.SerialPortException;

/**
 *
 * @author itssanat
 */
public class FXMLDocumentController implements Initializable {  // implement runnable

    protected Button play;
    @FXML
    private Button browse;
    @FXML
    private Button music;
    @FXML
    protected TextField nowPlaying;
    @FXML
    private ListView<String> songList;  // list view //

    static ListView<String> songlist2;
    @FXML
    private Button pause;
    @FXML
    private Button prev;
    @FXML
    private Button next;
    @FXML
    private Slider slider;
    @FXML
    private Button mostPlayed;
    @FXML
    private Button shuffle;
    @FXML
    private Button repeatAll;
    @FXML
    private Button repeatOne;
    @FXML
    private Slider volumeSlider;
    @FXML
    private Label timeLable;
    @FXML
    private Label alertLabel;

    private File songFile, prevSong, currentSong;
    private Media media;
    private MediaPlayer mediaPlayer;
    private File[] songs; // to store total song in music dir//
    private ObservableList<String> listItems = FXCollections.observableArrayList(); // observable list 
    public static int listsize = 0;
    final File dir = new File("C:\\Users\\asabul\\Desktop\\music");   // path of music directory // 
    private Thread t;
    private boolean isPlaying = false;
    private boolean isShuffle = false;
    private boolean isRepeatAll = true;
    private boolean isRepeatOne = false;
    private boolean isPaused = false;
    private String path;
    private int currentSongIndex = 0;
    Playback playback = new Playback();
    @FXML
    private ImageView coverPhoto; // to show cover photo of song //
    private Image image;
    private Connection connect;    // object to make connection with sqlite database //
    private Statement statement;
    @FXML
    private Button addFavorite;
    private boolean isFav = false;  // variable to check whether favorite playlist is open or not //
    @FXML
    private ImageView playIcon;
    Image playImage, pauseImage, defaultImage;

    //comm port initialized variables 
    static SerialPort serialPort = new SerialPort("COM3");

    //a scene for a new song 
    static Group _newSongGroup;
    static Scene _newSongScene;
    static Stage _newSongPrimaryStage;

    public FXMLDocumentController(int listSize) {
        this.listsize = listSize;
        this.songlist2 = songList;
    }

    public FXMLDocumentController() {

    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadSongs();

        repeatAll.setText("   Repeat All *");
        volumeSlider.setValue(100);
        slider.setValue(0);
        connect = connectDB();
        addFavorite.setText("");

        playImage = new Image("file:src/awesomex/icons/play.png");
        pauseImage = new Image("file:src/awesomex/icons/pause.png");
        defaultImage = new Image("file:src/awesomex/icons/AesomeXicon.jpg");
        ImageView iv = new ImageView(playImage);
        iv.setFitHeight(34);
        iv.setFitWidth(34);
        pause.setGraphic(iv);
        coverPhoto.setImage(defaultImage);
    }

    public void playButtonAction() {
        close();
        try {
            nowPlaying.setText(songFile.getName());  // updating status bar "nowPlaying" //
            currentSong = songFile; // updating current song name //
            currentSongIndex = songList.getSelectionModel().getSelectedIndex(); // updating the index of current song //
            if (isFavorite(currentSong.getName())) // checking the playlist //
            {
                addFavorite.setText("");
            } else {
                addFavorite.setText("/");
            }
            //t = new Thread(this);
            isPlaying = true;
            //t.start();  //invoking the thread //

            run();
            Thread coins = new coinAcceptor();
            coins.start();
            ImageView iv = new ImageView(pauseImage);
            iv.setFitHeight(34);
            iv.setFitWidth(34);
            pause.setGraphic(iv);

            songList.addEventFilter(KeyEvent.KEY_PRESSED, keyEvent -> {
                System.out.println("Insert coin to select song");
                songList.getSelectionModel().clearSelection();
            });
        } catch (Exception e) {
            nowPlaying.setText("error in playing");
        }
    }

    @FXML
    public void browseButtonAction(ActionEvent event) { // to choose a song from any dir //
        try {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Choose file to play..."); // setting title of title bar //
            chooser.showOpenDialog(null);
            songFile = chooser.getSelectedFile();
            playButtonAction();
        } catch (Exception e) {
            nowPlaying.setText("No file choosen");
        }
    }

    @FXML
    private void musicButtonAction(ActionEvent event) {  // opening music playlist //
        songList.getItems().clear();
        loadSongs();   // calling loadSong method //
        isFav = false;
    }

    @FXML
    private void selectedSong() {  // selecting song from the songList  // 
        if (isFav) {
            String s = songList.getSelectionModel().getSelectedItem();  // selection song from favorite playlist //
            songFile = FavoriteSong(s);
        } else {
            String s = songList.getSelectionModel().getSelectedItem();  // selection song from music playlist //
            if (s != null && !s.isEmpty()) {
                int selectedSong = songList.getSelectionModel().getSelectedIndex();
                songFile = songs[selectedSong];
            }
        }
        playButtonAction();
    }

    public void close() {
        if (isPlaying) {   // check whether a song is playing or not //
            mediaPlayer.stop();   // closing the song //
            isPlaying = false;
            isPaused = false;
        }
    }

    //@Override
    public void run() {  // start of the thread //
        try {
            media = new Media(songFile.toURI().toString());
            mediaPlayer = new MediaPlayer(media);                 // initialisation of MediaPlayer class //
            mediaPlayer.setVolume(volumeSlider.getValue() / 100);  // adding vloume slider //

            // adding a listener to volume slider ///
            volumeSlider.valueProperty().addListener(new ChangeListener<Number>() {
                @Override
                public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {

                    mediaPlayer.setVolume(newValue.doubleValue() / 100);

                }
            });
            mediaPlayer.currentTimeProperty().addListener(new ChangeListener<Duration>() {
                @Override
                public void changed(ObservableValue<? extends Duration> observable, Duration oldValue, Duration newValue) {
                    slider.setValue((newValue.toSeconds() / (mediaPlayer.getTotalDuration().toSeconds())) * 100);
                    timeLable.setText(playback.timeFormat(newValue.toSeconds(), mediaPlayer.getTotalDuration().toSeconds()));
                    image = (Image) (media.getMetadata().get("image"));
                    coverPhoto.setImage(image);
                }

            });

            //Thread.sleep(1000);
            slider.setOnMouseClicked(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    mediaPlayer.seek(Duration.seconds((slider.getValue() / 100) * mediaPlayer.getTotalDuration().toSeconds()));
                }
            });
            MediaView mediaView = new MediaView(mediaPlayer);
            mediaPlayer.play();
            mediaPlayer.setOnEndOfMedia(new Runnable() {  // playing next song when previous song is completed //
                @Override
                public void run() {
                    nextButtonAction(); // invoking next button //
                }
            });

//            //The source to starup the video media player 
//            Group root = new Group();
//            root.getChildren().add(mediaView);
//            Scene scene = new Scene(root, 500, 400);
//            Stage primaryStage = new Stage();
//            primaryStage.setScene(scene);
//            primaryStage.setTitle("Playing video");
//            primaryStage.show();
        } catch (Exception e) {
            nowPlaying.setText("error in playing");
        }
    }

    @FXML
    private void pauseButtonAction(ActionEvent event) {  // to pause and resume the song // 
        if (isPaused) {
            mediaPlayer.play();
            isPaused = false;
            nowPlaying.setText(currentSong.getName());
            //playIcon.setImage(new Image("icons//play.png"));
            ImageView iv = new ImageView(pauseImage);
            iv.setFitHeight(34);
            iv.setFitWidth(34);
            pause.setGraphic(iv);
        } else {
            mediaPlayer.pause();
            isPaused = true;
            nowPlaying.setText("Paused...");
            ImageView iv = new ImageView(playImage);
            iv.setFitHeight(34);
            iv.setFitWidth(34);
            pause.setGraphic(iv);
        }
    }

    @FXML
    private void prevButtonAction(ActionEvent event) {
        int totalSong = listItems.size();
        if (isRepeatOne) {
            songFile = currentSong;
        } else if (isRepeatAll) {
            currentSongIndex = (currentSongIndex - 1 + totalSong) % totalSong;
            songList.getSelectionModel().clearSelection();
            songList.getSelectionModel().select(currentSongIndex);
            songList.scrollTo(currentSongIndex);
            selectedSong();
        } else {
            Random random = new Random();
            int prev = random.nextInt(totalSong);
            songList.getSelectionModel().clearSelection();
            songList.getSelectionModel().select(prev);
            songList.scrollTo(prev);
            selectedSong();
        }
        playButtonAction();
    }

    @FXML
    private void nextButtonAction() {  // not completed, under developement // 
        int totalSong = listItems.size();
        if (isRepeatOne) {
            songFile = currentSong;
        } else if (isRepeatAll) {
            currentSongIndex = (currentSongIndex + 1) % totalSong;
            songList.getSelectionModel().clearSelection();
            songList.getSelectionModel().select(currentSongIndex);
            songList.scrollTo(currentSongIndex);
            selectedSong();
        } else {
            Random random = new Random();
            int next = random.nextInt(totalSong);
            songList.getSelectionModel().clearSelection();
            songList.getSelectionModel().select(next);
            songList.scrollTo(next);
            selectedSong();
        }
        playButtonAction();
    }

    @FXML
    private void songInfoButtonAction(ActionEvent event) {
        try {
            FXMLLoader loader1 = new FXMLLoader(getClass().getResource("SongInfo.fxml"));
            Parent root1 = (Parent) loader1.load();
            SongInfoController controller1 = loader1.getController(); //creating the object of controller class //
            Stage infoWindow = new Stage();
            infoWindow.setResizable(false);  // making window unresizable //
            controller1.album.setText((String) media.getMetadata().get("album"));
            controller1.title.setText((String) media.getMetadata().get("title"));
            controller1.artist.setText((String) media.getMetadata().get("artist"));
            infoWindow.setTitle("Song details"); // setting the title of stage //
            infoWindow.setScene(new Scene(root1));
            infoWindow.show();

        } catch (Exception e) {
            System.out.println("errpr");
        }
    }

    @FXML
    private void shuffleButtonAction(ActionEvent event) {
        isShuffle = true;
        isRepeatAll = false;
        isRepeatOne = false;
        shuffle.setText("   Shuffle *");
        repeatAll.setText("   Repeat All");
        repeatOne.setText("   Repeat One");
    }

    @FXML
    private void repeatAllButtonAction(ActionEvent event) {
        isShuffle = false;
        isRepeatAll = true;
        isRepeatOne = false;
        shuffle.setText("   Shuffle");
        repeatAll.setText("   Repeat All *");
        repeatOne.setText("   Repeat One");
    }

    @FXML
    private void repeatOneButtonAction(ActionEvent event) {
        isShuffle = false;
        isRepeatAll = false;
        isRepeatOne = true;
        shuffle.setText("   Shuffle");
        repeatAll.setText("   Repeat All");
        repeatOne.setText("   Repeat One *");
    }

    // Loding all the song present in "/home/itssanat/Music" dir //
    protected void loadSongs() {
        listItems.clear();
        try {
            FileFilter filter = new FileFilter() {
                @Override
                public boolean accept(File f) {
                    if (f.isDirectory()) {
                        return false;
                    }
                    if (f.getName().endsWith(".mp3") || f.getName().endsWith(".mkv") || f.getName().endsWith(".mp4")) {
                        return true;
                    } else {
                        return false;
                    }
                }
            };

            songs = dir.listFiles(filter);
            for (File file : songs) {
                if (file.getName().endsWith(".mp3") || file.getName().endsWith(".mkv") || file.getName().endsWith(".mp4")) //  file.toString().endsWith(".mp3")
                {
                    listItems.add(file.getName()); // adding song in observable list //
                }
            }
            songList.getItems().addAll(listItems); // to add in observable listView //
            listsize = listItems.size();
            //initialize the com port to listen for input requests 

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error in loding song");
        }
    }

    @FXML
    private void favoriteButtonAction(ActionEvent event) {
        try {
            statement = connect.createStatement();
            ResultSet rs = statement.executeQuery("SELECT * FROM favorite");
            songList.getItems().clear();
            listItems.clear();
            while (rs.next()) {
                listItems.add(rs.getString("name"));
            }
            songList.getItems().addAll(listItems);
            isFav = true;
        } catch (SQLException ex) {
            System.out.println("error in loading");
        }
    }

    public Connection connectDB() { // making connection with sqlite database // 
        try {
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection("jdbc:sqlite:C:/Users/asabul/Documents/NetBeansProjects/AwesomeX-master/awesomexDB.db");
            //System.out.println("connection estd...");
            return conn;
        } catch (Exception e) {
            System.out.println("unable to connect");
            return null;
        }
    }

    @FXML
    private void addFavoriteButtonAction(ActionEvent event) {
        if (isFavorite(currentSong.getName())) {
            String sql = "DELETE FROM favorite WHERE name = ?";
            try {
                PreparedStatement pstmt = connect.prepareStatement(sql);
                pstmt.setString(1, currentSong.getName());
                pstmt.executeUpdate();
                addFavorite.setText("/");
            } catch (SQLException ex) {
                System.out.println("error in deleting");
            }
        } else {
            String sql = "INSERT INTO favorite(name,path) VALUES(?,?)";
            try {
                PreparedStatement pstmt = connect.prepareStatement(sql);
                pstmt.setString(1, currentSong.getName());
                pstmt.setString(2, currentSong.getAbsolutePath());
                pstmt.executeUpdate();
                addFavorite.setText("");
            } catch (SQLException ex) {
                System.out.println("error");
            }
        }
    }

    public boolean isFavorite(String song) {
        try {
            statement = connect.createStatement();
            ResultSet rs = statement.executeQuery("SELECT * FROM favorite");
            while (rs.next()) {
                if (song.equals(rs.getString("name"))) {
                    return true;
                }
            }
            return false;
        } catch (SQLException ex) {
            System.out.println("error in loading");
        }
        return false;
    }

    public File FavoriteSong(String song) {
        for (File file : songs) {
            if (song.equals(file.getName())) {
                return file;
            }
        }
        return null;
    }

    //the bit where you wait requests from the arduino 
    class coinAcceptor extends Thread {

        public void run() {
            try {

                //Serial port configuration variables
                String comPort = "COM3";
                int baudeRate = 3000000;
                int dataBits = 8;
                int stopBits = 1;
                int parity = 0;
                SerialPort serialPort = new SerialPort(comPort);
                try {

                    while (true) {
                        if (serialPort.isOpened()) {

                            serialPort.setParams(baudeRate, dataBits, stopBits, parity);
                            String _coins = "";
                            if (serialPort.getInputBufferBytesCount() > 0) {
                                _coins = serialPort.readString();
                                System.out.println(_coins);
                                final String _coins2 = _coins;
                                try {
                                    if (_coins.equalsIgnoreCase("10")) {
                                        System.out.println("type in the song number of your choice");
                                        Platform.runLater(new Runnable() {
                                            @Override
                                            public void run() {
                                                try {
                                                    // Update UI here.where you request for someone to enter their song number
                                                    EventHandler<ActionEvent> _event = new EventHandler<ActionEvent>() {
                                                        @Override
                                                        public void handle(ActionEvent event) {

                                                            if (alertLabel.getText().length() != 0) {
                                                                alertLabel.setText("");
                                                            } else {
                                                                alertLabel.setTextFill(Color.web("#0076a3"));
                                                                alertLabel.setText("Type in the song number of your choice");
                                                            }
                                                        }
                                                    };
                                                    Timeline _flashInsertCoin = new Timeline(new KeyFrame(Duration.millis(1000), _event));
                                                    _flashInsertCoin.setCycleCount(Timeline.INDEFINITE);
                                                    _flashInsertCoin.play();
                                                    Thread.sleep(5000);

                                                    //the bit where we choose a song by typing in a number
                                                    songList.addEventFilter(KeyEvent.KEY_PRESSED, keyEvent -> {
                                                        // Not editing.
                                                        final Parent parent = songList.getParent();
                                                        parent.fireEvent(keyEvent.copyFor(parent, parent));
                                                        int totalSong = listsize;
                                                        System.out.println("Selected song number " + keyEvent.getCode().getName() + " current song list size " + totalSong);
                                                        currentSongIndex = (Integer.parseInt(keyEvent.getCode().getName()) - 1 + totalSong) % totalSong;
                                                        songList.getSelectionModel().clearSelection();
                                                        songList.getSelectionModel().select(currentSongIndex);
                                                        songList.scrollTo(currentSongIndex);
                                                        selectedSong();
                                                        playButtonAction();
                                                    });
                                                } catch (Exception ex) {
                                                    Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
                                                }
                                            }
                                        });
                                    } else {

                                        Platform.runLater(new Runnable() {
                                            @Override
                                            public void run() {
                                                EventHandler<ActionEvent> _event = new EventHandler<ActionEvent>() {
                                                    @Override
                                                    public void handle(ActionEvent event) {

                                                        if (alertLabel.getText().length() != 0) {
                                                            alertLabel.setText("");
                                                        } else {
                                                            alertLabel.setTextFill(Color.web("#0076a3"));
                                                            alertLabel.setText(_coins2);
                                                        }
                                                    }
                                                };
                                                Timeline _flashInsertCoin = new Timeline(new KeyFrame(Duration.millis(1000), _event));
                                                _flashInsertCoin.setCycleCount(Timeline.INDEFINITE);
                                                _flashInsertCoin.play();
                                            }
                                        });
                                    }
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                        } else {
                            try {
                                serialPort.openPort();
                                System.out.println("Port opened and listening");
                            } catch (Exception ex) {
                                //    ex.printStackTrace();
                            }

                        }
                    }
                    //serialPort.closePort();
                } catch (SerialPortException e) {
                    System.out.println("Connection failure, follow the errors below: ");
                    e.printStackTrace();
                }

            } catch (Exception e) {

                e.printStackTrace();
            }

        }
    }

}
