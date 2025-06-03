import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;




public class FloorPlanApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame());
    }
}




class MainFrame extends JFrame {
    private CanvasPanel canvas;
    private ControlPanel controls;




    public MainFrame() {
        setTitle("2D Floor Plan Designer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());




        canvas = new CanvasPanel();
        controls = new ControlPanel(canvas);




        add(canvas, BorderLayout.CENTER);
        add(controls, BorderLayout.EAST);




        setSize(1000, 600);
        setVisible(true);
    }
}




class CanvasPanel extends JPanel {
    private final int gridSize = 20; // Grid cell size
    private ArrayList<Room> rooms;
    private Room draggedRoom;
    private Point dragOffset;
    private Point originalPosition;
    private Room selectedRoom;
    private ArrayList<Door> doors;




    public CanvasPanel() {
        rooms = new ArrayList<>();
        doors = new ArrayList<>();
        setBackground(Color.WHITE);




        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Select or start dragging a room
                for (Room room : rooms) {
                    if (room.contains(e.getPoint())) {
                        selectedRoom = room;
                        draggedRoom = room;
                        dragOffset = new Point(e.getX() - room.getX(), e.getY() - room.getY());
                        originalPosition = new Point(room.getX(), room.getY());
                        repaint();
                        return;
                    }
                }
                selectedRoom = null; // Deselect if no room is clicked
                draggedRoom = null;
                repaint();
            }




            @Override
            public void mouseReleased(MouseEvent e) {
                if (draggedRoom != null) {
                    snapToGrid(draggedRoom);
                    if (isOverlapping(draggedRoom)) {
                        JOptionPane.showMessageDialog(CanvasPanel.this,
                            "Overlap detected! Returning to original position.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                        draggedRoom.setPosition(originalPosition.x, originalPosition.y);
                    }
                    draggedRoom = null;
                    repaint();
                }
            }
        });




        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (draggedRoom != null) {
                    draggedRoom.setPosition(e.getX() - dragOffset.x, e.getY() - dragOffset.y);
                    repaint();
                }
            }
        });
    }




    public Point findFreePosition(int width, int height) {
        for (int y = 0; y < getHeight(); y += gridSize) {
            for (int x = 0; x < getWidth(); x += gridSize) {
                Room tempRoom = new Room(x, y, width, height, Color.BLACK, "Temp","TEMP"); // Dummy room to check for overlap
                if (!isOverlapping(tempRoom)) {
                    return new Point(x, y);
                }
            }
        }
        return new Point(0, 0); // Fallback if no free position is found
    }
   
    public void clearAllRooms() {
        rooms.clear();  // Clear the list of rooms
        repaint();  // Repaint the canvas to reflect the change
    }
   


    public void addRoom(Room room) {
        snapToGrid(room);
        if (isOverlapping(room)) {
            JOptionPane.showMessageDialog(this, "Overlap detected! Room not added.", "Error", JOptionPane.ERROR_MESSAGE);
        } else {
            rooms.add(room);
            repaint();
        }
    }

    public void addDoor(String room1Name, String room2Name) {
        Room room1 = null, room2 = null;
    
        // Find rooms by name
        for (Room room : rooms) {
            if (room.getName().equalsIgnoreCase(room1Name)) {
                room1 = room;
            }
            if (room.getName().equalsIgnoreCase(room2Name)) {
                room2 = room;
            }
        }
    
        if (room1 == null || (room2Name.equalsIgnoreCase("outside") && room2 != null)) {
            JOptionPane.showMessageDialog(this, "Invalid room names!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
    
        // Determine door coordinates
        int x1, y1, x2, y2;
        if (room2 == null) { // Outside door
            x1 = room1.getX();
            y1 = room1.getY() + room1.getHeight() / 2;
            x2 = x1 - 20; // 20px length for outside door
            y2 = y1;
        } else { // Door between two rooms
            x1 = room1.getX() + room1.getWidth();
            y1 = room1.getY() + room1.getHeight() / 2;
            x2 = room2.getX();
            y2 = y1;
        }
    
        doors.add(new Door(x1, y1, x2, y2, room1, room2));
        repaint();
    }
    


    public void deleteSelectedRoom() {
        if (selectedRoom != null) {
            rooms.remove(selectedRoom);
            selectedRoom = null;
            repaint();
        } else {
            JOptionPane.showMessageDialog(this, "No room selected to delete!", "Error", JOptionPane.WARNING_MESSAGE);
        }
    }




    private void snapToGrid(Room room) {
        int newX = Math.round(room.getX() / (float) gridSize) * gridSize;
        int newY = Math.round(room.getY() / (float) gridSize) * gridSize;
        room.setPosition(newX, newY);
    }




    private boolean isOverlapping(Room room) {
        for (Room other : rooms) {
            if (room != other && room.intersects(other)) {
                return true;
            }
        }
        return false;
    }




    public void savePlan(File file) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("FLOORPLAN\n");
            for (Room room : rooms) {
                writer.write(room.toString());
                writer.newLine();
            }
        }
    }




    public void loadPlan(File file) throws IOException {
        rooms.clear();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String header = reader.readLine();
            if (!"FLOORPLAN".equals(header)) {
                throw new IOException("Invalid file format");
            }
            String line;
            while ((line = reader.readLine()) != null) {
                rooms.add(Room.fromString(line));
            }
        }
        selectedRoom = null;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
    
        Graphics2D g2 = (Graphics2D) g; // Use Graphics2D for better control
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    
        // Draw grid
        g2.setColor(Color.LIGHT_GRAY);
        for (int x = 0; x < getWidth(); x += gridSize) {
            g2.drawLine(x, 0, x, getHeight());
        }
        for (int y = 0; y < getHeight(); y += gridSize) {
            g2.drawLine(0, y, getWidth(), y);
        }
    
        // Draw rooms
        for (Room room : rooms) {
            g2.setColor(room.getColor());
            g2.fillRect(room.getX(), room.getY(), room.getWidth(), room.getHeight());
            
            g2.setColor(Color.BLACK);
            Stroke defaultStroke = g2.getStroke();
    
            // Set thick border
            g2.setStroke(new BasicStroke(3)); // Thicker wall
            g2.drawRect(room.getX(), room.getY(), room.getWidth(), room.getHeight());
    
            g2.setStroke(defaultStroke); // Reset stroke
        }
        // Draw doors
            g2.setColor(Color.WHITE);
            for (Door door : doors) {
                g2.setStroke(new BasicStroke(5)); // Make doors visually distinct
                g2.drawLine(door.getX1(), door.getY1(), door.getX2(), door.getY2());
            }

    
        // Highlight selected room
        if (selectedRoom != null) {
            g2.setColor(Color.RED);
            g2.setStroke(new BasicStroke(3)); // Highlight border stroke
            g2.drawRect(selectedRoom.getX(), selectedRoom.getY(), selectedRoom.getWidth(), selectedRoom.getHeight());
    
           // Floating message bubble
            String message = selectedRoom.getName() + " (" + selectedRoom.getType() + "): " + selectedRoom.getWidth() + "x" + selectedRoom.getHeight();
            int bubbleX = selectedRoom.getX() + selectedRoom.getWidth() + 5; // Slightly right of the room
            int bubbleY = selectedRoom.getY() - 5; // Slightly above the room


            g2.setColor(new Color(255, 255, 225)); // Light yellow background
            g2.fillRoundRect(bubbleX, bubbleY, message.length() * 7 + 10, 20, 10, 10);
            g2.setColor(Color.BLACK);
            g2.drawRoundRect(bubbleX, bubbleY, message.length() * 7 + 10, 20, 10, 10);
            g2.drawString(message, bubbleX + 5, bubbleY + 15);

        }
    }
      
}




class ControlPanel extends JPanel {
    private CanvasPanel canvas;
    private JComboBox<String> roomTypeDropdown;
    private JTextField widthField, heightField;
    private JButton addButton, saveButton, loadButton, deleteButton;
    private JButton addDoorButton;



    private JButton clearButton;


public ControlPanel(CanvasPanel canvas) {
    this.canvas = canvas;


    setLayout(new GridLayout(10, 1, 5, 5));  // Change from 9 to 10 to accommodate the new button


    roomTypeDropdown = new JComboBox<>(new String[]{"Bedroom", "Bathroom", "Living Room", "Kitchen"});
    widthField = new JTextField("100");
    heightField = new JTextField("100");
    addButton = new JButton("Add Room");
    saveButton = new JButton("Save Plan");
    loadButton = new JButton("Load Plan");
    deleteButton = new JButton("Delete Room");
    clearButton = new JButton("Clear Canvas");  // New Clear button
    addDoorButton = new JButton("Add Door");


    add(new JLabel("Room Type:"));
    add(roomTypeDropdown);
    add(new JLabel("Width:"));
    add(widthField);
    add(new JLabel("Height:"));
    add(heightField);
    add(addButton);
    add(saveButton);
    add(loadButton);
    add(deleteButton);
    add(clearButton);  // Add the clear button to the panel
    add(addDoorButton);
    


    addButton.addActionListener(e -> addRoom());
    saveButton.addActionListener(e -> savePlan());
    loadButton.addActionListener(e -> loadPlan());
    deleteButton.addActionListener(e -> deleteRoom());
    clearButton.addActionListener(e -> clearCanvas());  // Clear canvas action
    addDoorButton.addActionListener(e -> addDoor());
}


private void clearCanvas() {
    canvas.clearAllRooms();  // Call the method in the canvas to clear all rooms
}

private void addRoom() {
    try {
        String roomType = (String) roomTypeDropdown.getSelectedItem();
        int width = Integer.parseInt(widthField.getText());
        int height = Integer.parseInt(heightField.getText());

        if (width <= 0 || height <= 0) {
            JOptionPane.showMessageDialog(this, "Dimensions must be positive integers!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String name = JOptionPane.showInputDialog(this, "Enter a name for the room:", "Room Name", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Room name cannot be empty!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Color color = switch (roomType) {
            case "Bedroom" -> Color.decode("#247BA0");
            case "Bathroom" -> Color.decode("#70C1B3");
            case "Living Room" -> Color.decode("#F3FFBD");
            case "Kitchen" -> Color.decode("#FF1654");
            default -> Color.GRAY;
        };

        Point freePosition = canvas.findFreePosition(width, height);
        Room room = new Room(freePosition.x, freePosition.y, width, height, color, roomType, name);
        canvas.addRoom(room);
    } catch (NumberFormatException ex) {
        JOptionPane.showMessageDialog(this, "Please enter valid numeric dimensions!", "Error", JOptionPane.ERROR_MESSAGE);
    }
}


    private void savePlan() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                canvas.savePlan(fileChooser.getSelectedFile());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error saving file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void addDoor() {
        JTextField room1Field = new JTextField();
        JTextField room2Field = new JTextField();
    
        Object[] message = {
            "Room 1 Name:", room1Field,
            "Room 2 Name (or 'outside'):", room2Field
        };
    
        int option = JOptionPane.showConfirmDialog(this, message, "Add Door", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            String room1Name = room1Field.getText().trim();
            String room2Name = room2Field.getText().trim();
            if (!room1Name.isEmpty() && !room2Name.isEmpty()) {
                canvas.addDoor(room1Name, room2Name);
            } else {
                JOptionPane.showMessageDialog(this, "Both fields must be filled!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    


    private void loadPlan() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                canvas.loadPlan(fileChooser.getSelectedFile());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error loading file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }




    private void deleteRoom() {
        canvas.deleteSelectedRoom();
    }
}




class Room {
    private int x, y, width, height;
    private Color color;
    private String type;
    private String name;




    public Room(int x, int y, int width, int height, Color color, String type, String name) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.color = color;
        this.type = type;
        this.name = name;
    }

    public String getName() {
        return name;
    }


    public boolean contains(Point point) {
        return point.x >= x && point.x <= x + width && point.y >= y && point.y <= y + height;
    }




    public boolean intersects(Room other) {
        return !(x + width <= other.x || x >= other.x + other.width || y + height <= other.y || y >= other.y + other.height);
    }




    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Color getColor() {
        return color;
    }

    public String getType() {
        return type;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
public String toString() {
    return type + "," + x + "," + y + "," + width + "," + height + "," + name;
}


    public static Room fromString(String str) {
        String[] parts = str.split(",");
        return new Room(
            Integer.parseInt(parts[1]),
            Integer.parseInt(parts[2]),
            Integer.parseInt(parts[3]),
            Integer.parseInt(parts[4]),
            switch (parts[0]) {
                case "Bedroom" -> Color.decode("#247BA0");
                case "Bathroom" -> Color.decode("#70C1B3");
                case "Living Room" -> Color.decode("#F3FFBD");
                case "Kitchen" -> Color.decode("#FF1654");
                default -> Color.GRAY;
            },
            parts[0],
            parts[5] // Assuming the 6th part of the string is the name
        );
    }
    
}

class Door {
    private int x1, y1, x2, y2; // Coordinates for the door's start and end points
    private Room room1, room2; // Rooms the door connects (null if "outside")

    public Door(int x1, int y1, int x2, int y2, Room room1, Room room2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.room1 = room1;
        this.room2 = room2;
    }

    public int getX1() {
        return x1;
    }

    public int getY1() {
        return y1;
    }

    public int getX2() {
        return x2;
    }

    public int getY2() {
        return y2;
    }

    public Room getRoom1() {
        return room1;
    }

    public Room getRoom2() {
        return room2;
    }

    public boolean isOutsideDoor() {
        return room2 == null; // Indicates a door leading outside
    }
}