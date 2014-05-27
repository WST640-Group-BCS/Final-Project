package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class MainView extends JFrame {

	public MainView()
	{
		super("WST640 - Project - Group b");
		
		JFrame frame = new JFrame("WST640 - Project - Group b");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		
		final JLabel firstClusterResultsTextArea = new JLabel();	
		firstClusterResultsTextArea.setHorizontalAlignment(JLabel.CENTER);
		firstClusterResultsTextArea.setVerticalAlignment(JLabel.TOP);
		firstClusterResultsTextArea.setOpaque(true);
		firstClusterResultsTextArea.setBackground(Color.red);

		frame.getContentPane().add(firstClusterResultsTextArea, BorderLayout.LINE_START);
		firstClusterResultsTextArea.setBackground(Color.red);
		
		final JLabel secondClusterResultsTextArea = new JLabel();
		secondClusterResultsTextArea.setHorizontalAlignment(JLabel.CENTER);
		secondClusterResultsTextArea.setVerticalAlignment(JLabel.TOP);
		secondClusterResultsTextArea.setBackground(Color.blue);
		secondClusterResultsTextArea.setOpaque(true);
		secondClusterResultsTextArea.setBackground(Color.blue);

		frame.getContentPane().add(secondClusterResultsTextArea, BorderLayout.CENTER);

		final JLabel thirdClusterResultsTextArea = new JLabel();	
		thirdClusterResultsTextArea.setHorizontalAlignment(JLabel.CENTER);
		thirdClusterResultsTextArea.setVerticalAlignment(JLabel.TOP);
		
		frame.getContentPane().add(thirdClusterResultsTextArea, BorderLayout.LINE_END);
		thirdClusterResultsTextArea.setOpaque(true);
		thirdClusterResultsTextArea.setBackground(Color.green);

		final JTextField textField1;

		textField1 = new JTextField(10);
		frame.getContentPane().add(textField1, BorderLayout.PAGE_START);
	    textField1.setHorizontalAlignment(JTextField.CENTER);
		textField1.getDocument().addDocumentListener(new DocumentListener() {
			  public void changedUpdate(DocumentEvent e) {
			    textFieldValueChanged();
			  }
			  public void removeUpdate(DocumentEvent e) {
				  textFieldValueChanged();
			  }
			  public void insertUpdate(DocumentEvent e) {
				  textFieldValueChanged();
			  }

			  public void textFieldValueChanged() {
				  String valueTypedByUser = textField1.getText();
				  firstClusterResultsTextArea.setText("Result from first cluster");
				  secondClusterResultsTextArea.setText("Result from second cluster");
				  thirdClusterResultsTextArea.setText("Result from third cluster");
			  }
			});

		frame.pack();
		frame.setSize(800, 600);
		
		frame.setVisible(true);
	}

	public static void main(String[] args) {
		new MainView();
	}
}

