package gui;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.DocumentListener;

import org.w3c.dom.events.DocumentEvent;

import indexer.Trec_indexing;

public class MainView extends JFrame implements DocumentListener {

	JTextField textField1;
	private JLabel firstClusterResultsTextArea;
	private static Trec_indexing indexTrec;
	final JLabel secondClusterResultsTextArea;
	final JLabel thirdClusterResultsTextArea;
	
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
		
		secondClusterResultsTextArea = new JLabel();
		secondClusterResultsTextArea.setHorizontalAlignment(JLabel.CENTER);
		secondClusterResultsTextArea.setVerticalAlignment(JLabel.TOP);
		secondClusterResultsTextArea.setBackground(Color.blue);
		secondClusterResultsTextArea.setOpaque(true);
		secondClusterResultsTextArea.setBackground(Color.blue);

		frame.getContentPane().add(secondClusterResultsTextArea, BorderLayout.CENTER);

		thirdClusterResultsTextArea = new JLabel();	
		thirdClusterResultsTextArea.setHorizontalAlignment(JLabel.CENTER);
		thirdClusterResultsTextArea.setVerticalAlignment(JLabel.TOP);
		
		frame.getContentPane().add(thirdClusterResultsTextArea, BorderLayout.LINE_END);
		thirdClusterResultsTextArea.setOpaque(true);
		thirdClusterResultsTextArea.setBackground(Color.green);

		textField1 = new JTextField(10);
		frame.getContentPane().add(textField1, BorderLayout.PAGE_START);
	    textField1.setHorizontalAlignment(JTextField.CENTER);
		textField1.getDocument().addDocumentListener(this);


		frame.pack();
		frame.setSize(800, 600);
		
		frame.setVisible(true);
	}

	public static void main(String[] args) {
		new MainView();
		indexTrec = new Trec_indexing();
		indexTrec.startIndexingFiles(7);
		

	}
	public void typed()
	{
	  String valueTypedByUser = textField1.getText();
	  indexTrec.search(valueTypedByUser);

	  firstClusterResultsTextArea.setText("Result from first cluster");
	  secondClusterResultsTextArea.setText("Result from second cluster");
	  thirdClusterResultsTextArea.setText("Result from third cluster");
	}
	@Override
	public void insertUpdate(javax.swing.event.DocumentEvent e) {
		this.typed();
	}

	@Override
	public void removeUpdate(javax.swing.event.DocumentEvent e) {
		this.typed();
	}

	@Override
	public void changedUpdate(javax.swing.event.DocumentEvent e) {
		this.typed();
	}
}

