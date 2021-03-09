package doubleTree;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

import javax.swing.JButton;
import javax.swing.JFrame;

import org.graphstream.graph.Edge;
import org.graphstream.graph.ElementNotFoundException;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.stream.GraphParseException;
import org.graphstream.ui.swingViewer.ViewPanel;
import org.graphstream.ui.view.Viewer;

public class HeuristicTSP {
	
	private Graph graph;
	DoubleTree doubleTree;
	
	public static void main(String[] args) {
        System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
        HeuristicTSP s = new HeuristicTSP(true);
        String rawPath = "D:\\OneDrive - Seattle Pacific University\\Eclipse Workspace\\DoubleTree\\luxembourg.txt";
//        String dgsPath = "D:\\OneDrive - Seattle Pacific University\\Eclipse Workspace\\DoubleTree\\luxembourg.dgs";
//        String rawPath = "D:\\OneDrive - Seattle Pacific University\\Eclipse Workspace\\DoubleTree\\oman.txt";
//        String dgsPath = "D:\\OneDrive - Seattle Pacific University\\Eclipse Workspace\\DoubleTree\\oman.dgs";
//        String rawPath = "D:\\OneDrive - Seattle Pacific University\\Eclipse Workspace\\DoubleTree\\japan.txt";
        
        // Read a raw text file or read from a .dgs file. The .dgs files are often read incorrectly, forcing us to 
        // generate a new graph from the raw data every time
        s.generateGraph(rawPath);
//        s.writeFile(dgsPath);
        // OR
//        s.readFile(dgsPath);
	}
	
	/**
	 * Instantiate an empty graph and display the GUI
	 */
	public HeuristicTSP(boolean drawGraph) {
		graph = new MultiGraph("The Graph");
		
		doubleTree = new DoubleTree();
		doubleTree.init(graph);
		
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Container content = frame.getContentPane();
		content.setLayout(new BorderLayout());
		JButton run = new JButton("Find TSP");
		run.setBackground(Color.WHITE);
		run.setPreferredSize(new Dimension(500, 60));
		run.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				doubleTree.compute();
			}
		});
		content.add(run, BorderLayout.SOUTH);
		if(drawGraph) {
			Viewer viewer = new Viewer(graph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
			viewer.setCloseFramePolicy(Viewer.CloseFramePolicy.HIDE_ONLY);
			viewer.disableAutoLayout();
			ViewPanel viewPanel = viewer.addDefaultView(false);
			viewPanel.setPreferredSize(new Dimension(1000,800));
			content.add(viewPanel, BorderLayout.CENTER);
		}
		frame.pack();
		frame.setVisible(true);
	}
	
	/**
	 * Reads a .dgs file into graph using Graph.read(). This does not work for large files
	 * It often reads in incorrect edges or edges without the right attributes
	 * @param path
	 */
	public void readFile(String path)
	{
		try {
			graph.read(path);
		} catch (ElementNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (GraphParseException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Uses the Graph.write() method to output a .dgs file representing the current graph
	 * @param path the location to save the .dgs file
	 */
	public void writeFile(String path){
		try {
			graph.write(path);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Creates a complete graph based on the raw text file. Weight is calculated mathematically by their
	 * relative longitudes and latitudes
	 * @param rawFile the source .txt file. Must be space separated and formatted as: nodeNum latitude longitude
	 */
	public void generateGraph(String rawFile) {
		File file = new File(rawFile);
		Scanner scanner = null;
		try {
			scanner = new Scanner(file);
		} catch (FileNotFoundException e) {
			System.err.println("Could not instantiate file scanner. Aborting");
			System.exit(1);
		}
		
		String css = "edge {size:1px;z-index:2;fill-color:gray;}" + 
				"edge .red{size:1px;z-index:2;fill-color:red;}" + 
				"edge .green{size:1px;z-index:2;fill-color:green;}" + 
				"node {size:4px;fill-color:black;text-size:16;text-mode:normal;text-background-mode:plain;z-index:3;text-alignment:under;}";
		
		graph.addAttribute("ui.stylesheet", css);
		graph.addAttribute("ui.antialias");
		
		Scanner lineScanner = null;
		int edgeIndex = 0;
		// Connect the new node to every existing node
		while(scanner.hasNextLine()) {
			lineScanner = new Scanner(scanner.nextLine());
			lineScanner.useDelimiter("[\n ]");
			String nodeIndex = lineScanner.next().trim();
			String yString = lineScanner.next().trim();
			String xString = lineScanner.next().trim();
			double y = Double.parseDouble(yString);
			double x = Double.parseDouble(xString);
			Node newNode = null;
			if(graph.getNode(nodeIndex) == null) {
				newNode = graph.addNode(nodeIndex);
				newNode.addAttribute("xyz", x, y, 0.0);
				for(Node n : graph)
				{
					if(n == newNode)
					{
						continue;
					}
					String edgeName = Integer.toString(edgeIndex);
					edgeIndex++;
					Edge newEdge = doubleTree.addWeightedEdge(edgeName, newNode, n);
					newEdge.addAttribute("ui.hide");
				}
			}
		}
		lineScanner.close();
		scanner.close();
	}
}
