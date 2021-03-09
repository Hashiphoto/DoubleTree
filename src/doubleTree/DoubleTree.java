package doubleTree;

import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Iterator;

import org.graphstream.algorithm.Algorithm;
import org.graphstream.algorithm.Prim;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

public class DoubleTree implements Algorithm {
    static final double earthRadius = 6378.1370;
    static final double toRad = Math.PI / 180;
    
	private Graph graph;
	private int step = 0;
	private HashSet<Node> eulerTour = new HashSet<Node>();
	private double startTime;

	@Override
	public void init(Graph graph) {
		this.graph = graph;		
	}

	/**
	 * Solve the TSP using the Double Tree algorithm
	 */
	@Override
	public void compute() {
		double segmentStartTime = System.nanoTime();
		switch(step) {
		case 0:
			System.out.println("Prim MST");
			startTime = System.nanoTime();
			prim();
			break;
		case 1:
			System.out.println("Double edges");
			doubleEdges();
			break;
		case 2:
			System.out.println("Euler Tour");
			eulerTour();
			break;
		case 3:
			System.out.println("Cut Short");
			cutShort();
			break;
		}
		double seconds = nanoToSeconds(System.nanoTime() - segmentStartTime);
		System.out.println("\tCompleted in " + new DecimalFormat("#.##########").format(seconds));
		System.out.println("There are " + graph.getNodeCount() + " nodes and " + graph.getEdgeCount() + " edges");
		if(step == 3) {
			System.out.println("\nTotal trip length: " + getTSPLength());
			System.out.println("Calculated in " + nanoToSeconds(System.nanoTime() - startTime));
			return;
		}
		step++;
		// Remove this line if you want to see the calculations run step by step
		//compute();
	}
	
	private double nanoToSeconds(double nanoseconds) {
		return nanoseconds / 1000000000.0;
	}

	/**
	 * Run Prim's algorithm on the graph to find an MST
	 */
	private void prim() {
		Prim prim = new Prim();
		prim.init(graph);
		prim.setWeightAttribute("weight");
		prim.compute();
		int count = 0;
		for(Edge e : prim.getTreeEdges()) {
			e.removeAttribute("ui.hide");
			count++;
		}
		int i = 0;
		while(graph.getEdgeCount() > count) {
			Edge edge = graph.getEdge(i);
			if(edge.hasAttribute("ui.hide")) {
				graph.removeEdge(edge);
			} else {
				i++;
			}
		}
	}
	
	/**
	 * Duplicate every edge
	 * Runs in O(m) time
	 */
	private void doubleEdges() {
		int limit = graph.getEdgeCount();
		for(int i = 0; i < limit; i++) {
			Edge edge = graph.getEdge(i);
			addWeightedEdge(edge.getId() + "doubled", edge.getNode0(), edge.getNode1());
		}
	}
	
	/**
	 * Find a Euler Tour through the graph
	 * Runs in O(m+n) time
	 */
	private void eulerTour() {
		int edgesVisited = 0;
		int totalEdges = graph.getEdgeCount();
		Node n = graph.getNode(0);
		while(edgesVisited < totalEdges) {
			eulerTour.add(n);
			for(Edge e : n.getEdgeSet()) {
				if(e.hasAttribute("visited")) {
					continue;
				}
				e.addAttribute("visited");
				e.setAttribute("ui.class", "red");
				edgesVisited++;
				n = e.getOpposite(n);
				break;
			}
		}
		eulerTour.add(graph.getNode(0));
	}
	
	/**
	 * Shortcut the Euler tour
	 * Runs in O(m) time
	 */
	private void cutShort() {
		int newEdges = 0;
		Node last = null;
		Iterator<Node> itr = eulerTour.iterator();
		boolean getNext = true;
		Node n = null;
		while(itr.hasNext()) {
			if(getNext) {
				n = itr.next();
			} else {
				getNext = true;
			}
			// Found an unvisited node
			if(!n.hasAttribute("visited")) {
				n.setAttribute("visited");
				last = n;
				continue;
			}
			
			// Short cutting over visited nodes
			getNext = false;
			Node original = last;
			// Find the next unvisited node
			while(n.hasAttribute("visited") && itr.hasNext()) {
				// Remove edge from the previous node to the already visited node(s)
				removeEdge(n, last);
				last = n;
				n = itr.next();
			}
			removeEdge(n, last);
			// n is now have the first unvisited node
			if(!n.hasEdgeBetween(original)) {
				Edge e = addWeightedEdge(newEdges + "shortcut", n,original);
				e.setAttribute("ui.class", "red");
				newEdges++;
			}
		}
	}
	
	/**
	 * Helper function for removing an edge from the graph
	 * @param n1	The node one one end of the edge
	 * @param n2	The node on the other end of the edge 
	 */
	private void removeEdge(Node n1, Node n2) {
		Edge e = n1.getEdgeBetween(n2);
		graph.removeEdge(e);
	}
	
	/**
	 * Find the total trip length. This does not count towards the time calculation since it could be 
	 * computed simultaneously during the short-cutting. It is separated out for clarity
	 * Runs in O(n) time
	 * @return The total length of the trip
	 */
	private double getTSPLength() {
		double length = 0;
		for(Edge e : graph.getEachEdge()) {
			double addWeight = (double)e.getAttribute("weight");
			length += addWeight;
		}
		return length;
	}

	/**
	 * Add an undirected weighted edge on the graph between two nodes 
	 * @param name	the unique name of the edge
	 * @param n1	the node that will be at one end of the edge
	 * @param n2	the node at the other end of the edge
	 * @return		the added edge
	 */
	public Edge addWeightedEdge(String name, Node n1, Node n2) {
		Edge newEdge = graph.addEdge(name, n1.getId(), n2.getId());
		Object[] n1Coord = n1.getAttribute("xyz");
		Object[] n2Coord = n2.getAttribute("xyz");
		// Use haversineDistance to find the distance in KM
//		double dist = haversineDistance(fixCoord(n1Coord[0]), fixCoord(n1Coord[1]), fixCoord(n2Coord[0]), fixCoord(n2Coord[1]));
		double dist = distance((double)n1Coord[0], (double)n1Coord[1], (double)n2Coord[0], (double)n2Coord[1]);
		newEdge.addAttribute("weight", dist);
		return newEdge;
	}
	
	private double distance(double x1, double y1, double x2, double y2) {
		return Math.sqrt((y2 - y1) * (y2 - y1) + (x2 - x1) * (x2 - x1));
	}
	
	/**
	 * Helper function to fix the data which is entered as 1000 times the longitude and latitude
	 * @param num
	 * @return 
	 */
	@SuppressWarnings("unused")
	private double fixCoord(Object num)
	{
		return ((double)num) / 1000;
	}
	
	/**
	 * Calculates the distance between two points given longitude and latitude
	 * @param lat1	latitude of node 1
	 * @param long1	longitude of node 1
	 * @param lat2	latitude of node 2
	 * @param long2	longitude of node2
	 * @return The distance between the points across the earth's curvature, in KM
	 */
	@SuppressWarnings("unused")
	private double haversineDistance(double lat1, double long1, double lat2, double long2) {
		double dlong = (long2 - long1) * toRad;
        double dlat = (lat2 - lat1) * toRad;
        double a = Math.pow(Math.sin(dlat / 2.0), 2.0) + Math.cos(lat1 * toRad) * Math.cos(lat2 * toRad)
                * Math.pow(Math.sin(dlong / 2.0), 2.0);
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        double distance = earthRadius * c;

        return distance;
	}
}
