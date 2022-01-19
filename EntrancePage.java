import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class EntrancePage extends JFrame implements ActionListener {

    JButton teacherEntrance;
    JButton studentEntrance;

    EntrancePage(){
        /* adjusting starting position of the window */
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        this.setLocation(dim.width/3, dim.height/4);

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setTitle("Classroom Presenter");
        this.setLayout(null);

        /* text area - upper */
        JLabel text = new JLabel();
        text.setText("Classroom Presenter");
        text.setFont(new Font("Consolas",Font.BOLD,35));
        text.setVerticalAlignment(JLabel.CENTER);
        text.setHorizontalAlignment(JLabel.CENTER);

        /* creating upper panel */
        JPanel upperPanel = new JPanel();
        upperPanel.setBackground(new Color(0X08c8ff));
        upperPanel.setBounds(0, 0, 500, 150);
        upperPanel.setLayout(new BorderLayout());

        /* creating medium panel */
        JPanel mediumPanel = new JPanel();
        mediumPanel.setBackground(Color.blue);
        mediumPanel.setBounds(0, 150, 500, 150);
        mediumPanel.setLayout(new BorderLayout());

        /* creating lower panel */
        JPanel lowerPanel = new JPanel();
        lowerPanel.setBackground(Color.green);
        lowerPanel.setBounds(0, 300, 500, 150);
        lowerPanel.setLayout(new BorderLayout());

        /* creating buttons */
        teacherEntrance = new JButton("Enter as teacher (Create a class)");
        studentEntrance = new JButton("Enter as student (Join to a class)");
        teacherEntrance.addActionListener(this);
        studentEntrance.addActionListener(this);
        teacherEntrance.setFocusable(false);
        studentEntrance.setFocusable(false);


        /* filling panels */
        upperPanel.add(text);
        mediumPanel.add(teacherEntrance);
        lowerPanel.add(studentEntrance);

        /* adding panels to main layout */
        this.add(upperPanel);
        this.add(mediumPanel);
        this.add(lowerPanel);

        this.setSize(new Dimension(515,490));
        this.setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == teacherEntrance){
            System.out.println("teacher entrance");

            TeacherEntranceTextField stdTextField = new TeacherEntranceTextField();

            /* deactivating the entry buttons when one of them is pressed */
            stdTextField.addWindowListener(new WindowAdapter() {
                @Override
                public void windowOpened(WindowEvent e) {
                    super.windowOpened(e);
                    teacherEntrance.setEnabled(false);
                    studentEntrance.setEnabled(false);
                }
            });

            stdTextField.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    super.windowClosed(e);
                    teacherEntrance.setEnabled(true);
                    studentEntrance.setEnabled(true);
                }
            });

        }
        else if(e.getSource() == studentEntrance){
            System.out.println("student entrance");

            StudentEntranceTextField stdTextField = new StudentEntranceTextField();

            /* deactivating the entry buttons when one of them is pressed */
            stdTextField.addWindowListener(new WindowAdapter() {
                @Override
                public void windowOpened(WindowEvent e) {
                    super.windowOpened(e);
                    teacherEntrance.setEnabled(false);
                    studentEntrance.setEnabled(false);
                }

                @Override
                public void windowClosed(WindowEvent e) {
                    super.windowClosed(e);
                    teacherEntrance.setEnabled(true);
                    studentEntrance.setEnabled(true);
                }
            });


        }

    }
}
