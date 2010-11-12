// NOTE: Please read README.txt before browsing this code.

import java.io.*;
import java.util.*;

import spatialindex.spatialindex.*;
import spatialindex.storagemanager.*;
import spatialindex.rtree.*;

public class RTreeQuery
{
	public static void main(String[] args)
	{
		new RTreeQuery(args);
	}
	
	RTreeQuery(String[] args)
	{
		try
		{
			if (args.length != 3)
			{
				System.err.println("Usage: RTreeQuery query_file tree_file query_type [intersection | 10NN].");
				System.exit(-1);
			}

			LineNumberReader lr = null;

			try
			{
				lr = new LineNumberReader(new FileReader(args[0]));
			}
			catch (FileNotFoundException e)
			{
				System.err.println("Cannot open query file " + args[0] + ".");
				System.exit(-1);
			}

			// Create a disk based storage manager.
			PropertySet ps = new PropertySet();

			ps.setProperty("FileName", args[1]);
				// .idx and .dat extensions will be added.

			IStorageManager diskfile = new DiskStorageManager(ps);

			IBuffer file = new RandomEvictionsBuffer(diskfile, 10, false);
				// applies a main memory random buffer on top of the persistent storage manager
				// (LRU buffer, etc can be created the same way).

			PropertySet ps2 = new PropertySet();

			// If we need to open an existing tree stored in the storage manager, we only
			// have to specify the index identifier as follows
			Integer i = new Integer(1); // INDEX_IDENTIFIER_GOES_HERE (suppose I know that in this case it is equal to 1);
			ps2.setProperty("IndexIdentifier", i);
				// this will try to locate and open an already existing r-tree index from file manager file.

			ISpatialIndex tree = new RTree(ps2, file);

			int count = 0;
			int indexIO = 0;
			int leafIO = 0;
			int id, op;
			double x1, x2, y1, y2;
			double[] f1 = new double[2];
			double[] f2 = new double[2];

			long start = System.currentTimeMillis();
			String line = lr.readLine();

			while (line != null)
			{
				StringTokenizer st = new StringTokenizer(line);
				op = new Integer(st.nextToken()).intValue();
				id = new Integer(st.nextToken()).intValue();
				x1 = new Double(st.nextToken()).doubleValue();
				y1 = new Double(st.nextToken()).doubleValue();
				x2 = new Double(st.nextToken()).doubleValue();
				y2 = new Double(st.nextToken()).doubleValue();

				if (op == 2)
				{
					//query

					f1[0] = x1; f1[1] = y1;
					f2[0] = x2; f2[1] = y2;

					MyVisitor vis = new MyVisitor();

					if (args[2].equals("intersection"))
					{
						Region r = new Region(f1, f2);
						tree.intersectionQuery(r, vis);
							// this will find all data that intersect with the query range.
					}
					else if (args[2].equals("10NN"))
					{
						Point p = new Point(f1);
						tree.nearestNeighborQuery(10, p, vis);
							// this will find the 10 nearest neighbors.
					}
					else
					{
						System.err.println("Unknown query type.");
						System.exit(-1);
					}

					indexIO += vis.m_indexIO;
					leafIO += vis.m_leafIO;
						// example of the Visitor pattern usage, for calculating how many nodes
						// were visited.
				}
				else
				{
					System.err.println("This is not a query operation.");
				}

				if ((count % 1000) == 0) System.err.println(count);

				count++;
				line = lr.readLine();
			}

			long end = System.currentTimeMillis();

			MyQueryStrategy2 qs = new MyQueryStrategy2();
			tree.queryStrategy(qs);

			System.err.println("Indexed space: " + qs.m_indexedSpace);
			System.err.println("Operations: " + count);
			System.err.println(tree);
			System.err.println("Index I/O: " + indexIO);
			System.err.println("Leaf I/O: " + leafIO);
			System.err.println("Minutes: " + ((end - start) / 1000.0f) / 60.0f);

			// flush all pending changes to persistent storage (needed since Java might not call finalize when JVM exits).
			tree.flush();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	// example of a Visitor pattern.
	// findes the index and leaf IO for answering the query and prints
	// the resulting data IDs to stdout.
	class MyVisitor implements IVisitor
	{
		public int m_indexIO = 0;
		public int m_leafIO = 0;

		public void visitNode(final INode n)
		{
			if (n.isLeaf()) m_leafIO++;
			else m_indexIO++;
		}

		public void visitData(final IData d)
		{
			System.out.println(d.getIdentifier());
				// the ID of this data entry is an answer to the query. I will just print it to stdout.
		}
	}

	// example of a Strategy pattern.
	// traverses the tree by level.
	class MyQueryStrategy implements IQueryStrategy
	{
		private ArrayList ids = new ArrayList();

		public void getNextEntry(IEntry entry, int[] nextEntry, boolean[] hasNext)
		{
			Region r = entry.getShape().getMBR();

			System.out.println(r.m_pLow[0] + " " + r.m_pLow[1]);
			System.out.println(r.m_pHigh[0] + " " + r.m_pLow[1]);
			System.out.println(r.m_pHigh[0] + " " + r.m_pHigh[1]);
			System.out.println(r.m_pLow[0] + " " + r.m_pHigh[1]);
			System.out.println(r.m_pLow[0] + " " + r.m_pLow[1]);
			System.out.println();
			System.out.println();
				// print node MBRs gnuplot style!

			// traverse only index nodes at levels 2 and higher.
			if (entry instanceof INode && ((INode) entry).getLevel() > 1)
			{
				for (int cChild = 0; cChild < ((INode) entry).getChildrenCount(); cChild++)
				{
					ids.add(new Integer(((INode) entry).getChildIdentifier(cChild)));
				}
			}

			if (! ids.isEmpty())
			{
				nextEntry[0] = ((Integer) ids.remove(0)).intValue();
				hasNext[0] = true;
			}
			else
			{
				hasNext[0] = false;
			}
		}
	};

	// example of a Strategy pattern.
	// find the total indexed space managed by the index (the MBR of the root).
	class MyQueryStrategy2 implements IQueryStrategy
	{
		public Region m_indexedSpace;

		public void getNextEntry(IEntry entry, int[] nextEntry, boolean[] hasNext)
		{
			// the first time we are called, entry points to the root.
			IShape s = entry.getShape();
			m_indexedSpace = s.getMBR();

			// stop after the root.
			hasNext[0] = false;
		}
	}
}
