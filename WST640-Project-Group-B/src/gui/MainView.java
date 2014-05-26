package gui;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JLabel;

public class MainView extends JFrame {

	public MainView()
	{
		JFrame frame = new JFrame("FrameDemo");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JLabel emptyLabel = new JLabel();	
		frame.getContentPane().add(emptyLabel, BorderLayout.CENTER);

		frame.pack();
		frame.setSize(800, 600);

		frame.setVisible(true);
	}
	
	public static void main(String[] args) {
		MainView testView = new MainView();
	}
}

