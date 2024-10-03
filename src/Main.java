/*
 * Coded by     : M. Fazri Nizar
 * Institution  : Sriwijaya University
 * GitHub       : github.com/mfazrinizar
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.File;
import java.io.IOException;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import maze.Direction;
import maze.Maze;
import maze.RecursiveBacktracker;

public class Main extends JPanel {
    private static final long serialVersionUID = 1L;

    // Constants for difficulties
    private static final int WIDTH_EASY = 5;
    private static final int HEIGHT_EASY = 5;
    private static final int WIDTH_MEDIUM = 7;
    private static final int HEIGHT_MEDIUM = 7;
    private static final int WIDTH_HARD = 9;
    private static final int HEIGHT_HARD = 9;
    private static final int TILE_SIZE = 50;

    private static final int MIN_DISTANCE_FROM_START = 3; // Minimum distance from starting point

    private static final int INVISIBLE_WALL_DELAY = 5000; // 5 seconds delay for walls to turn invisible

    private Dimension dimension;
    private List<Shape> shapes;
    private Maze maze;
    private boolean[][][] wallHits;
    private boolean[][] visibleWalls; // Track visible walls
    private int playerX, playerXOri, playerY, playerYOri, goalX, goalY; // Player and goal positions
    private int heartsA, heartsB;
    private int currentDifficulty = 1;
    private int currentGameAttempts = 1;
    private int round = 1;
    private int games = 0;
    private int playerAWins = 0, playerBWins = 0;
    private boolean playerATurn = true; // Toggle between Player A and Player B
    private boolean wallsInvisible = false; // Toggle invisible walls
    private Timer invisibleTimer;
    private boolean canMove = false;
    private boolean practiceMode = false;
    private boolean visibleToggled = false;
    private JLabel roundLabel, gameLabel, playerLabel, heartsLabel, invisibleTimerLabel, playerAWinsLabel,
            playerBWinsLabel;
    private boolean statusPanelCreated = false;
    private JPanel contentPanel;
    JLabel placeholderLabel;
    private Clip clip;

    private JFrame window;

    public Main(JFrame window) {
        this.window = window;
        dimension = new Dimension();
        shapes = new ArrayList<>();
        setFocusable(true);
        addKeyListener(new MazeKeyListener());

        // Set JFrame properties
        window.setResizable(false);

        // Create a panel to hold your components
        contentPanel = new JPanel();
        contentPanel.setPreferredSize(new Dimension(500, 650)); // Set preferred size
        window.setContentPane(contentPanel); // Set the content pane

        // Add title as placeholder
        placeholderLabel = new JLabel("Click Start Game menu to play the game!");
        contentPanel.add(placeholderLabel);

        // Create status panel with labels
        // createStatusPanel();

        // Initialize menu
        createMenu();

        // Start game with easy difficulty by default
        // startGame(getDifficulty(currentDifficulty)[0],
        // getDifficulty(currentDifficulty)[1]);

        window.pack(); // Pack the frame to respect the preferred size
        window.setVisible(true); // Make the frame visible
    }

    // Method to create the status panel with labels
    private void createStatusPanel() {
        JPanel statusPanel = new JPanel();
        statusPanel.setLayout(new GridLayout(4, 2)); // 7 rows, 2 columns

        roundLabel = new JLabel("Round: " + round);
        gameLabel = new JLabel("Game: " + (games + 1));
        playerLabel = new JLabel("Player: " + (playerATurn ? "A" : "B"));
        heartsLabel = new JLabel("Hearts: " + (playerATurn ? heartsA : heartsB));
        playerAWinsLabel = new JLabel("A Wins: " + playerAWins);
        playerBWinsLabel = new JLabel("B Wins: " + playerBWins);
        invisibleTimerLabel = new JLabel();

        try {
            int timerValue = Integer.parseInt(String.valueOf(invisibleTimer));
            invisibleTimerLabel.setText("Invisible in: " + timerValue + " sec");
        } catch (NumberFormatException e) {
            invisibleTimerLabel.setText("Invisible in: 0 sec");
        }

        // Add custom panels with text and ovals
        JPanel playerPanel = createLabelWithOval("Player: ", Color.BLUE, TILE_SIZE / 3);
        JPanel goalPanel = createLabelWithOval("Goal: ", Color.GREEN, TILE_SIZE / 3);

        // Add the components to the status panel in a 2-column layout
        statusPanel.add(roundLabel);
        statusPanel.add(gameLabel);
        statusPanel.add(heartsLabel);
        statusPanel.add(playerLabel);
        statusPanel.add(playerPanel);
        statusPanel.add(goalPanel);
        statusPanel.add(playerAWinsLabel);
        statusPanel.add(playerBWinsLabel);
        statusPanel.add(invisibleTimerLabel);

        // Add the status panel to the window
        window.add(statusPanel, BorderLayout.NORTH); // Place the status panel at the top of the window
    }

    private JPanel createLabelWithOval(String labelText, Color ovalColor, int ovalSize) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT)); // Flow layout to place label and oval in one row

        JLabel label = new JLabel(labelText);

        // Custom panel for drawing the oval
        JPanel ovalPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(ovalColor);
                g.fillOval(0, 0, ovalSize, ovalSize); // Draw the oval
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(ovalSize, ovalSize); // Set size of the oval panel
            }
        };

        // Add label and oval to the panel
        panel.add(label);
        panel.add(ovalPanel);

        return panel;
    }

    // Method to create the menu at the top of the window
    private void createMenu() {
        JMenuBar menuBar = new JMenuBar();

        // Create 'Game' menu
        JMenu gameMenu = new JMenu("Start Game");

        // "1v1 Game" Menu Item
        JMenuItem start1v1GameItem = new JMenuItem("Start 1v1 Game");
        start1v1GameItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));

        start1v1GameItem.addActionListener(e -> start1v1Game());

        // "Practice Game" Menu Item with Submenu for difficulties
        JMenu practiceGameMenu = new JMenu("Start Practice Game");

        // Difficulty options
        JMenuItem easyGameItem = new JMenuItem("Easy Game");
        easyGameItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK));
        easyGameItem.addActionListener(e -> startEasyGame());

        JMenuItem mediumGameItem = new JMenuItem("Medium Game");
        mediumGameItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.CTRL_DOWN_MASK));
        mediumGameItem.addActionListener(e -> startMediumGame());

        JMenuItem hardGameItem = new JMenuItem("Hard Game");
        hardGameItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_DOWN_MASK));
        hardGameItem.addActionListener(e -> startHardGame());

        // Add difficulty options to the "Practice Game" menu
        practiceGameMenu.add(easyGameItem);
        practiceGameMenu.add(mediumGameItem);
        practiceGameMenu.add(hardGameItem);

        // "Invisible/Visible" Toggle Button
        JCheckBoxMenuItem toggleVisibilityItem = new JCheckBoxMenuItem("Toggle Invisible/Visible");
        toggleVisibilityItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK));
        toggleVisibilityItem.addActionListener(e -> toggleVisibility());

        // Add all items to the main 'Game' menu
        gameMenu.add(start1v1GameItem);
        gameMenu.add(practiceGameMenu);
        gameMenu.addSeparator(); // Add a separator line
        gameMenu.add(toggleVisibilityItem);

        // Add 'Game' menu to the menu bar
        menuBar.add(gameMenu);

        // Set the menu bar to the window
        window.setJMenuBar(menuBar);
    }

    private void start1v1Game() {
        practiceMode = false;
        System.out.println("1v1 Game started!");
        currentDifficulty = 1;
        startGame(getDifficulty(currentDifficulty)[0], getDifficulty(currentDifficulty)[1]);
    }

    private void startEasyGame() {
        practiceMode = true;
        currentDifficulty = 1; // Easy
        System.out.println("Easy Game selected!");
        startGame(getDifficulty(currentDifficulty)[0], getDifficulty(currentDifficulty)[1]);
    }

    private void startMediumGame() {
        practiceMode = true;
        currentDifficulty = 2; // Medium
        System.out.println("Medium Game selected!");
        startGame(getDifficulty(currentDifficulty)[0], getDifficulty(currentDifficulty)[1]);
    }

    private void startHardGame() {
        practiceMode = true;
        currentDifficulty = 3; // Hard
        System.out.println("Hard Game selected!");
        startGame(getDifficulty(currentDifficulty)[0], getDifficulty(currentDifficulty)[1]);
    }

    private void toggleVisibility() {
        wallsInvisible = !wallsInvisible; // Toggle the walls visibility
        visibleToggled = !visibleToggled;
        System.out.println("Walls " + (wallsInvisible ? "Invisible" : "Visible"));
        repaint(); // Redraw the maze with the new visibility state
    }

    @Override
    public Dimension getPreferredSize() {
        return this.dimension;
    }

    // @Override
    // public void paintComponent(Graphics g) {
    // super.paintComponent(g);
    // Graphics2D g2d = (Graphics2D) g;

    // g2d.setStroke(new BasicStroke(3.0f));

    // // Draw maze walls
    // for (Shape s : shapes) {
    // g2d.draw(s);
    // g2d.fill(s);
    // }

    // // Define sizes for player and goal circles
    // int goalTileSize = TILE_SIZE / 2;
    // int playerTileSize = TILE_SIZE / 2;

    // // Draw goal (green circle) centered in its tile
    // g2d.setColor(Color.GREEN);
    // g2d.fillOval(goalX * TILE_SIZE + (TILE_SIZE - goalTileSize) / 2,
    // goalY * TILE_SIZE + (TILE_SIZE - goalTileSize) / 2,
    // goalTileSize,
    // goalTileSize);

    // // Draw player (blue circle) centered in its tile
    // g2d.setColor(Color.BLUE);
    // g2d.fillOval(playerX * TILE_SIZE + (TILE_SIZE - playerTileSize) / 2,
    // playerY * TILE_SIZE + (TILE_SIZE - playerTileSize) / 2,
    // playerTileSize,
    // playerTileSize);
    // }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        g2d.setStroke(new BasicStroke(3.0f));

        // Draw the entire grid in grey (as placeholders for invisible walls)
        g2d.setColor(Color.LIGHT_GRAY);
        for (int y = 0; y < maze.getHeight(); y++) {
            for (int x = 0; x < maze.getWidth(); x++) {
                g2d.drawRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            }
        }

        // Draw visible maze walls
        for (int y = 0; y < maze.getHeight(); y++) {
            for (int x = 0; x < maze.getWidth(); x++) {
                if (!wallsInvisible || visibleWalls[x][y]) { // Check if walls are visible
                    // Draw NORTH wall
                    if (maze.isWall(x, y, Direction.NORTH)) {
                        if (wallHits[x][y][0]) {
                            g2d.setColor(Color.RED); // Red for hit NORTH wall
                        } else if (!wallsInvisible) {
                            g2d.setColor(Color.BLACK); // Black for visible wall
                        } else {
                            g2d.setColor(Color.LIGHT_GRAY); // Invisible for not hit NORTH wall
                        }
                        g2d.drawLine(x * TILE_SIZE, y * TILE_SIZE, (x + 1) * TILE_SIZE, y * TILE_SIZE);
                    }

                    // Draw SOUTH wall
                    if (maze.isWall(x, y, Direction.SOUTH)) {
                        if (wallHits[x][y][1]) {
                            g2d.setColor(Color.RED); // Red for hit SOUTH wall
                        } else if (!wallsInvisible) {
                            g2d.setColor(Color.BLACK); // Black for visible wall
                        } else {
                            g2d.setColor(Color.LIGHT_GRAY); // Invisible for not hit SOUTH wall
                        }
                        g2d.drawLine(x * TILE_SIZE, (y + 1) * TILE_SIZE, (x + 1) * TILE_SIZE, (y + 1) * TILE_SIZE);
                    }

                    // Draw WEST wall
                    if (maze.isWall(x, y, Direction.WEST)) {
                        if (wallHits[x][y][2]) {
                            g2d.setColor(Color.RED); // Red for hit WEST wall
                        } else if (!wallsInvisible) {
                            g2d.setColor(Color.BLACK); // Black for visible wall
                        } else {
                            g2d.setColor(Color.LIGHT_GRAY); // Invisible for not hit WEST wall
                        }
                        g2d.drawLine(x * TILE_SIZE, y * TILE_SIZE, x * TILE_SIZE, (y + 1) * TILE_SIZE);
                    }

                    // Draw EAST wall
                    if (maze.isWall(x, y, Direction.EAST)) {
                        if (wallHits[x][y][3]) {
                            g2d.setColor(Color.RED); // Red for hit EAST wall
                        } else if (!wallsInvisible) {
                            g2d.setColor(Color.BLACK); // Black for visible wall
                        } else {
                            g2d.setColor(Color.LIGHT_GRAY); // Invisible for not hit EAST wall
                        }
                        g2d.drawLine((x + 1) * TILE_SIZE, y * TILE_SIZE, (x + 1) * TILE_SIZE, (y + 1) * TILE_SIZE);
                    }
                }
            }
        }

        // Define sizes for player and goal circles
        int goalTileSize = TILE_SIZE / 2;
        int playerTileSize = TILE_SIZE / 2;

        // Draw goal (green circle) centered in its tile
        if (wallsInvisible || visibleToggled) {
            g2d.setColor(Color.GREEN);
            g2d.fillOval(goalX * TILE_SIZE + (TILE_SIZE - goalTileSize) / 2,
                    goalY * TILE_SIZE + (TILE_SIZE - goalTileSize) / 2,
                    goalTileSize,
                    goalTileSize);

            g2d.setColor(Color.BLUE);
            g2d.fillOval(playerX * TILE_SIZE + (TILE_SIZE - playerTileSize) / 2,
                    playerY * TILE_SIZE + (TILE_SIZE - playerTileSize) / 2,
                    playerTileSize,
                    playerTileSize);
        }
    }

    private void startInvisibleTimer() {
        // Cancel the existing timer if it's running
        if (invisibleTimer != null) {
            invisibleTimer.cancel();
            invisibleTimer.purge(); // Removes any canceled tasks to free up memory
        }

        // Reset game-related flags (e.g., wallsInvisible) before starting the new timer
        wallsInvisible = false;
        canMove = false; // Disable player movement at the start

        // Wrap invisibleTimerValue in an array to modify it in the TimerTask
        final int[] invisibleTimerValue = { (int) (INVISIBLE_WALL_DELAY / 1000) }; // Convert milliseconds to seconds

        // Create a new timer
        invisibleTimer = new Timer();

        // Schedule the task to run every second and update the timer label
        invisibleTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // Update the label with the remaining time
                SwingUtilities.invokeLater(() -> {
                    try {
                        int timerValue = Integer.parseInt(String.valueOf(invisibleTimerValue[0]));
                        invisibleTimerLabel.setText("Invisible in: " + timerValue + " sec");
                    } catch (NumberFormatException e) {
                        invisibleTimerLabel.setText("Invisible in: 0 sec");
                    }
                });

                // Decrement the countdown
                invisibleTimerValue[0]--;

                // Check if the timer has reached 0
                if (invisibleTimerValue[0] == 0) {
                    invisibleTimer.cancel(); // Stop the timer
                    visibleToggled = false;
                    wallsInvisible = true; // Set walls to be invisible
                    canMove = true; // Allow the player to move
                    repaint(); // Redraw the maze to reflect the change in wall visibility

                    // Notify players that the walls are now invisible
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(Main.this,
                                "Walls are now invisible! Player " + (playerATurn ? "A" : "B") + "'s turn!");
                    });
                }
            }
        }, 0, 1000); // Run every 1 second
    }

    public void resetWholeGame() {
        // Reset game state
        round = 1;
        games = 0;
        playerAWins = 0;
        playerBWins = 0;
        currentDifficulty = 1;
        wallsInvisible = false;
        canMove = false;
        repaint();
    }

    private int[] getDifficulty(int currentDifficulty) {
        int[] difficulty = new int[2];
        switch (currentDifficulty) {
            case 1:
                difficulty[0] = WIDTH_EASY;
                difficulty[1] = HEIGHT_EASY;
                break;
            case 2:
                difficulty[0] = WIDTH_MEDIUM;
                difficulty[1] = HEIGHT_MEDIUM;
                break;
            case 3:
                difficulty[0] = WIDTH_HARD;
                difficulty[1] = HEIGHT_HARD;
                break;
        }
        return difficulty;
    }

    // private void resetGameState() {
    // // Reset other game states like player position, score, etc.
    // wallsInvisible = false; // Reset wall visibility at the start of each game
    // visibleWalls = new boolean[maze.getWidth()][maze.getHeight()];
    // repaint(); // Repaint the maze
    // }

    private void startGame(int width, int height) {
        maze = new RecursiveBacktracker(width, height);

        contentPanel.remove(placeholderLabel);

        maze.generate();

        visibleWalls = new boolean[width][height]; // Initialize invisible walls

        // Initialize wall hit tracking (4 directions per tile)
        wallHits = new boolean[width][height][4]; // 0=NORTH, 1=SOUTH, 2=WEST, 3=EAST

        heartsA = heartsB = 3; // Start with 3 hearts for both players

        // Randomize player position
        Random random = new Random();
        do {
            playerX = random.nextInt(width);
            playerY = random.nextInt(height);
        } while (Math.abs(playerX - goalX) < MIN_DISTANCE_FROM_START
                || Math.abs(playerY - goalY) < MIN_DISTANCE_FROM_START);

        // Set goal position randomly
        do {
            goalX = random.nextInt(width);
            goalY = random.nextInt(height);
        } while (Math.abs(goalX - playerX) < MIN_DISTANCE_FROM_START
                || Math.abs(goalY - playerY) < MIN_DISTANCE_FROM_START);

        playerXOri = playerX;
        playerYOri = playerY;

        if (!practiceMode)
            changePlayerTurn();

        // Reset shapes for the maze
        shapes.clear();
        loadMaze();

        int panelWidth = maze.getWidth() * TILE_SIZE + 1;
        int panelHeight = maze.getHeight() * TILE_SIZE + 1;
        setPreferredSize(new Dimension(panelWidth, panelHeight));

        if (!statusPanelCreated) {
            createStatusPanel();
            statusPanelCreated = true;
        }

        updateStatus();
        repaint();

        revalidate(); // Refresh layout
        window.pack(); // Resize the window based on the preferred size

        startInvisibleTimer(); // Start the invisible walls timer
    }

    // Method to update status labels dynamically
    private void updateStatus() {
        roundLabel.setText("Round: " + round);
        gameLabel.setText("Game: " + (games + 1));
        playerLabel.setText("Player: " + (playerATurn ? "A" : "B"));
        heartsLabel.setText("Hearts: " + (playerATurn ? heartsA : heartsB));
        playerAWinsLabel.setText("A Wins: " + playerAWins);
        playerBWinsLabel.setText("B Wins: " + playerBWins);
        try {
            int timerValue = Integer.parseInt(String.valueOf(invisibleTimer));
            invisibleTimerLabel.setText("Invisible in: " + timerValue + " sec");
        } catch (NumberFormatException e) {
            invisibleTimerLabel.setText("Invisible in: 0 sec");
        }
    }

    private void playSound(String soundFilePath) {
        try {
            if (clip != null && clip.isRunning()) {
                clip.stop();
            }
            AudioInputStream audioInputStream = AudioSystem
                    .getAudioInputStream(new File(soundFilePath).getAbsoluteFile());
            clip = AudioSystem.getClip();
            clip.open(audioInputStream);
            clip.start();
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    private void loadMaze() {
        for (int y = 0; y < maze.getHeight(); ++y) {
            for (int x = 0; x < maze.getWidth(); ++x) {
                // Add walls for each cell based on maze structure
                if (maze.isWall(x, y, Direction.NORTH)) {
                    shapes.add(new Line2D.Float(x * TILE_SIZE, y * TILE_SIZE, (x + 1) * TILE_SIZE, y * TILE_SIZE));
                }
                if (maze.isWall(x, y, Direction.SOUTH)) {
                    shapes.add(new Line2D.Float(x * TILE_SIZE, (y + 1) * TILE_SIZE, (x + 1) * TILE_SIZE,
                            (y + 1) * TILE_SIZE));
                }
                if (maze.isWall(x, y, Direction.WEST)) {
                    shapes.add(new Line2D.Float(x * TILE_SIZE, y * TILE_SIZE, x * TILE_SIZE, (y + 1) * TILE_SIZE));
                }
                if (maze.isWall(x, y, Direction.EAST)) {
                    shapes.add(new Line2D.Float((x + 1) * TILE_SIZE, y * TILE_SIZE, (x + 1) * TILE_SIZE,
                            (y + 1) * TILE_SIZE));
                }
            }
        }

        int panelWidth = maze.getWidth() * TILE_SIZE + 1;
        int panelHeight = maze.getHeight() * TILE_SIZE + 1;
        dimension.setSize(panelWidth, panelHeight);
    }

    private void handlePlayerMove(Direction direction) {
        int newX = playerX;
        int newY = playerY;

        // Determine new player position based on direction
        switch (direction) {
            case NORTH:
                newY--;
                break;
            case SOUTH:
                newY++;
                break;
            case WEST:
                newX--;
                break;
            case EAST:
                newX++;
                break;
        }

        // Check bounds and walls
        if (newX >= 0 && newX < maze.getWidth() && newY >= 0 && newY < maze.getHeight()) {
            if (!maze.isWall(playerX, playerY, direction)) { // No wall in the direction
                playerX = newX;
                playerY = newY;
            } else {
                // Hit a wall
                playSound("src/assets/hit-wall.wav");
                System.out.println("Hit a wall! at " + playerX + ", " + playerY);

                switch (direction) {
                    case NORTH:
                        wallHits[playerX][playerY][0] = true; // NORTH
                        break;
                    case SOUTH:
                        wallHits[playerX][playerY][1] = true; // SOUTH
                        break;
                    case WEST:
                        wallHits[playerX][playerY][2] = true; // WEST
                        break;
                    case EAST:
                        wallHits[playerX][playerY][3] = true; // EAST
                        break;
                }

                markWall(playerX, playerY); // Mark the wall as visible
                if (playerATurn) {
                    System.out.println("Player A lost a heart!" + heartsA);
                    JOptionPane.showMessageDialog(this, "You hit a wall! Hearts left: " + --heartsA);
                    if (heartsA == 0) {
                        playerATurn = false;
                        JOptionPane.showMessageDialog(this, "Player A lost all hearts!");
                        currentGameAttempts++;
                        if (!(currentGameAttempts > 2))
                            JOptionPane.showMessageDialog(this,
                                    "\nRound: " + round + "\nGames: " + (games + 1) + "\nPlayer "
                                            + (playerATurn ? "A" : "B") + "'s turn!");
                        heartsA = 3;
                        resetPlayerPosition();
                    }
                } else {
                    System.out.println("Player B lost a heart!" + heartsB);
                    JOptionPane.showMessageDialog(this, "You hit a wall! Hearts left: " + --heartsB);
                    if (heartsB == 0) {
                        playerATurn = true;
                        JOptionPane.showMessageDialog(this, "Player B lost all hearts!");
                        currentGameAttempts++;
                        if (!(currentGameAttempts > 2))
                            JOptionPane.showMessageDialog(this,
                                    "\nRound: " + round + "\nGames: " + (games + 1) + "\nPlayer "
                                            + (playerATurn ? "A" : "B") + "'s turn!");
                        heartsB = 3;
                        resetPlayerPosition();
                    }
                }
            }
            checkGameOver();
        }
        repaint();

    }

    private void markWall(int x, int y) {
        visibleWalls[x][y] = true;
        repaint();
    }

    private void resetPlayerPosition() {
        playerX = playerXOri;
        playerY = playerYOri;
    }

    private void changePlayerTurn() {
        if (games % 2 == 0) {
            playerATurn = true;
        } else if (!(currentGameAttempts > 2)) {
            playerATurn = false;
        } else {
            playerATurn = !playerATurn;
        }
        JOptionPane.showMessageDialog(this,
                "\nRound: " + round + "\nGames: " + (games + 1) + "\nPlayer " + (playerATurn ? "A" : "B") + "'s turn!");
    }

    private void checkGameOver() {
        if (playerX == goalX && playerY == goalY) {
            // if (currentGameAttempts > 2)
                currentGameAttempts = 1;
            games++;
            System.out.println("Player " + (playerATurn ? "A" : "B") + " reached the goal!");
            if (playerATurn) {
                playerAWins++;
            } else {
                playerBWins++;
            }
            if (playerAWins == 4) {
                JOptionPane.showMessageDialog(this, "Player A Wins the Game!");
                resetWholeGame();
            } else if (playerBWins == 4) {
                JOptionPane.showMessageDialog(this, "Player B Wins the Game!");
                resetWholeGame();
            } else if (playerAWins == 3 && playerBWins == 3) {
                JOptionPane.showMessageDialog(this, "It's a Draw!");
                resetWholeGame();
            } else {
                startNextRound();
            }
        } else if (currentGameAttempts > 2) {
            currentGameAttempts = 1;
            games++;
            JOptionPane.showMessageDialog(this,
                    "Both players lost the game! Starting new game!");
            startNextRound();
        }
        updateStatus();
    }

    private void startNextRound() {
        if (practiceMode) {
            JOptionPane.showMessageDialog(this, "Game Over!"
                    + "\nCongratulations on completing the practice mode!");
            resetWholeGame();
        }

        else if (games % 2 == 0) {
            round++;
            currentDifficulty++;
            if (round == 2) {
                startGame(WIDTH_MEDIUM, HEIGHT_MEDIUM);
            } else if (round == 3) {
                startGame(WIDTH_HARD, HEIGHT_HARD);
            } else {
                JOptionPane.showMessageDialog(this, "Game Over!\n Player A Wins: " + playerAWins + "\n Player B Wins: "
                        + playerBWins + (playerAWins > playerBWins ? "\nPlayer A Wins!"
                                : (playerBWins > playerAWins) ? "\nPlayer B Wins!" : "\nIt's a Draw!"));
                resetWholeGame();
            }
        } else {
            startGame(getDifficulty(currentDifficulty)[0], getDifficulty(currentDifficulty)[1]);
        }
    }

    private class MazeKeyListener extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            if (!canMove) {
                return;
            }

            int key = e.getKeyCode();

            switch (key) {
                case KeyEvent.VK_UP:
                case 'W':
                    handlePlayerMove(Direction.NORTH);
                    break;
                case KeyEvent.VK_DOWN:
                case 'S':
                    handlePlayerMove(Direction.SOUTH);
                    break;
                case KeyEvent.VK_LEFT:
                case 'A':
                    handlePlayerMove(Direction.WEST);
                    break;
                case KeyEvent.VK_RIGHT:
                case 'D':
                    handlePlayerMove(Direction.EAST);
                    break;
            }
        }
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            JFrame window = new JFrame("Invisible Maze Game");
            Main panel = new Main(window);
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.add(panel);
            window.setLocationRelativeTo(null);
        });
    }
}
