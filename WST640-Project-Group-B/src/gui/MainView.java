package gui;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import indexer.Trec_indexing;

public class MainView extends JFrame {

	JTextField textField1;
	private JLabel firstClusterResultsTextArea;
	
	public JTextField getSearchField()
	{
		return this.textField1;
	}
	
	public JLabel firstClusterResultsTextArea()
	{
		return this.firstClusterResultsTextArea;
	}

	
	public MainView()
	{
		super("WST640 - Project - Group b");
		
		JFrame frame = new JFrame("WST640 - Project - Group b");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		
		this.firstClusterResultsTextArea = new JLabel();	
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

		textField1 = new JTextField(10);
		frame.getContentPane().add(textField1, BorderLayout.PAGE_START);
	    textField1.setHorizontalAlignment(JTextField.CENTER);
		

		frame.pack();
		frame.setSize(800, 600);
		
		frame.setVisible(true);
	}

	public static void main(String[] args) {
		new MainView();
		Trec_indexing indexTrec = new Trec_indexing();
		indexTrec.indexAndSearch();
	}
}

