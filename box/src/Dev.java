import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.Vector;

public class Dev extends JFrame implements ActionListener {
    // Declare UI components
    private JButton startButton;
    private JButton saveCountButton;
    private JLabel countLabel;
    private JTextField minRadiusTextField;
    private JTextField maxRadiusTextField;
    private Camera camera;
    private Timer timer;

    private JTable countTable;
    private DefaultTableModel tableModel;
    private Vector<String> tableHeaders;
    private Vector<Vector<String>> tableData;
    private int sum;

    public Dev() {
        super("Real-Time Bolt Counting");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(800, 500));

        // Initialize UI components
        startButton = new JButton("Start");
        startButton.addActionListener(this);

        saveCountButton = new JButton("Save Count");
        saveCountButton.addActionListener(this);

        countLabel = new JLabel("Circle Count: 0");
        countLabel.setFont(new Font("Arial", Font.BOLD, 18));

        JLabel minRadiusLabel = new JLabel("Minimum Radius:");
        minRadiusTextField = new JTextField(5);

        JLabel maxRadiusLabel = new JLabel("Maximum Radius:");
        maxRadiusTextField = new JTextField(5);

        // Create button panel and add UI components
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        buttonPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        buttonPanel.add(startButton, gbc);

        gbc.gridx = 1;
        buttonPanel.add(saveCountButton, gbc);

        gbc.gridx = 2;
        buttonPanel.add(countLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        buttonPanel.add(minRadiusLabel, gbc);

        gbc.gridx = 1;
        buttonPanel.add(minRadiusTextField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        buttonPanel.add(maxRadiusLabel, gbc);

        gbc.gridx = 1;
        buttonPanel.add(maxRadiusTextField, gbc);

        // Create table for count data
        tableHeaders = new Vector<>();
        tableHeaders.add("Count");
        tableHeaders.add("Sum");
        tableData = new Vector<>();

        tableModel = new DefaultTableModel(tableData, tableHeaders) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        countTable = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component comp = super.prepareRenderer(renderer, row, column);
                if (row % 2 == 0) {
                    comp.setBackground(new Color(235, 235, 235)); // Light gray color
                } else {
                    comp.setBackground(Color.WHITE);
                }
                return comp;
            }
        };
        countTable.setFont(new Font("Arial", Font.PLAIN, 14));
        countTable.setRowHeight(25);
        JScrollPane scrollPane = new JScrollPane(countTable);

        // Create table panel and add the count table
        JPanel tablePanel = new JPanel();
        tablePanel.setBackground(Color.WHITE);
        tablePanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        tablePanel.setLayout(new BorderLayout());
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        getContentPane().setBackground(new Color(245, 245, 245)); // Light gray color

        setLayout(new BorderLayout());
        add(buttonPanel, BorderLayout.PAGE_START);
        add(tablePanel, BorderLayout.CENTER);

        sum = 0;
    }

    // Method to start the camera
    private void startCamera(int cameraIndex) {
        String minRadiusText = minRadiusTextField.getText();
        String maxRadiusText = maxRadiusTextField.getText();

        if (minRadiusText.isEmpty() || maxRadiusText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter both minimum and maximum radius.");
            return;
        }

        if (!isNumeric(minRadiusText) || !isNumeric(maxRadiusText)) {
            JOptionPane.showMessageDialog(this, "Please enter numeric values for the minimum and maximum radius.");
            return;
        }

        int minRadius = Integer.parseInt(minRadiusText);
        int maxRadius = Integer.parseInt(maxRadiusText);

        if (minRadius >= maxRadius) {
            JOptionPane.showMessageDialog(this, "Minimum radius must be smaller than the maximum radius.");
            return;
        }
        if (camera == null) {
            camera = new Camera();
            if (!camera.open(cameraIndex)) {
                System.out.println("Failed to open camera!");
                return;
            }

            Mat frame = new Mat();
            camera.read(frame);

            if (!frame.empty()) {
                BufferedImage image = camera.matToBufferedImage(frame);
                ImageIcon icon = new ImageIcon(image);
                JLabel label = new JLabel(icon);
                JPanel cameraPanel = new JPanel();
                cameraPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
                cameraPanel.add(label);

                add(cameraPanel, BorderLayout.SOUTH);
                pack();
                setVisible(true);
                revalidate();

                // Create a timer to continuously read frames from the camera
                timer = new Timer(33, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        camera.read(frame);
                        if (!frame.empty()) {
                            Mat grayImage = new Mat();
                            Imgproc.cvtColor(frame, grayImage, Imgproc.COLOR_BGR2GRAY);
                            Imgproc.blur(grayImage, grayImage, new Size(3, 3));
                            int threshold = 100;
                            int minRadius = Integer.parseInt(minRadiusTextField.getText());
                            int maxRadius = Integer.parseInt(maxRadiusTextField.getText());
                            Mat circles = camera.detectCircles(grayImage, threshold, minRadius, maxRadius);

                            // Update circle count and display it
                            int circleCount = camera.getCircleCount(circles);
                            countLabel.setText("Circle Count: " + circleCount);

                            camera.drawCircles(frame, circles, 1.0);
                            BufferedImage updatedImage = camera.matToBufferedImage(frame);
                            icon.setImage(updatedImage);
                            label.repaint();
                        }
                    }
                });
                timer.start();
            }
        }
    }

    // Method to stop the camera
    private void stopCamera() {
        if (camera != null) {
            camera.release();
            camera = null;
            getContentPane().remove(2);
            pack();
            timer.stop();
            countLabel.setText("Circle Count: 0");
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Handle button clicks
        if (e.getSource() == startButton) {
            if (startButton.getText().equals("Start")) {
                String minRadiusText = minRadiusTextField.getText();
                String maxRadiusText = maxRadiusTextField.getText();

                if (minRadiusText.isEmpty() || maxRadiusText.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Please enter both minimum and maximum radius.");
                    return;
                }

                if (!isNumeric(minRadiusText) || !isNumeric(maxRadiusText)) {
                    JOptionPane.showMessageDialog(this, "Please enter numeric values for the minimum and maximum radius.");
                    return;
                }

                int minRadius = Integer.parseInt(minRadiusText);
                int maxRadius = Integer.parseInt(maxRadiusText);

                if (minRadius >= maxRadius) {
                    JOptionPane.showMessageDialog(this, "Minimum radius must be smaller than the maximum radius.");
                    return;
                }

                int cameraIndex = Integer.parseInt(JOptionPane.showInputDialog("Enter camera index:"));
                startCamera(cameraIndex);
                startButton.setText("Stop");
            } else {
                stopCamera();
                startButton.setText("Start");
            }
        } else if (e.getSource() == saveCountButton) {
            int circleCount = Integer.parseInt(countLabel.getText().split(": ")[1]);

            Vector<String> rowData = new Vector<>();
            rowData.add(String.valueOf(circleCount));
            sum += circleCount;
            rowData.add(String.valueOf(sum));

            tableData.add(rowData);
            tableModel.fireTableDataChanged();

            // Scroll to the bottom of the table
            countTable.scrollRectToVisible(countTable.getCellRect(countTable.getRowCount() - 1, 0, true));
        }
    }

    private boolean isNumeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");
    }

    public static void main(String[] args) {
        // Load OpenCV library
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        // Run the application
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Dev dev = new Dev();
                dev.pack();
                dev.setVisible(true);
            }
        });
    }
}
